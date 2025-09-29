package com.github.kr328.clash.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.common.util.grantPermissions
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.theme.AppTheme
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FilesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val scope = rememberCoroutineScope()
                val uuid = remember { intent.uuid }
                if (uuid == null) { finish(); return@AppTheme }

                var profile by remember { mutableStateOf<Profile?>(null) }
                var files by remember { mutableStateOf<List<File>>(emptyList()) }
                val stack = remember { Stack<String>() }
                val client = remember { FilesClient(this@FilesActivity) }
                var canEdit by remember { mutableStateOf(false) }
                val root = remember { uuid.toString() }

                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        scope.launch {
                            val parent = if (stack.empty()) root else stack.last()
                            val name = uri.fileName ?: "File"
                            client.importDocument(parent, uri, name)
                            refresh(client, stack, root) { files = it }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    profile = withContext(Dispatchers.IO) { withProfile { queryByUUID(uuid) } }
                    if (profile == null) { finish(); return@LaunchedEffect }
                    canEdit = profile?.type != Profile.Type.Url
                    refresh(filesClient = client, stack = stack, root = uuid.toString()) { files = it }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(stringResource(R.string.profile_files)) },
                            navigationIcon = { IconButton(onClick = {
                                if (stack.empty()) finish() else { stack.pop(); scope.launch { refresh(client, stack, root) { files = it } } }
                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                            actions = {
                                if (canEdit) {
                                    IconButton(onClick = { importLauncher.launch("*/*") }) { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                        )
                    }
                ) { inner ->
                    if (files.isEmpty()) {
                        Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.empty)) }
                    } else {
                        LazyColumn(Modifier.padding(inner).fillMaxSize()) {
                            items(files, key = { it.id }) { f ->
                                FileRow(
                                    file = f,
                                    canEdit = canEdit,
                                    onOpen = {
                                        if (f.isDirectory) {
                                            stack.push(f.id)
                                            scope.launch { refresh(client, stack, root) { files = it } }
                                        } else {
                                            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(
                                                client.buildDocumentUri(f.id),
                                                "text/plain"
                                            ).grantPermissions()
                                            startActivity(intent)
                                        }
                                    },
                                    onDelete = { scope.launch { client.deleteDocument(f.id); refresh(client, stack, root) { files = it } } },
                                    onRename = { newName -> scope.launch { client.renameDocument(f.id, newName); refresh(client, stack, root) { files = it } } },
                                    onExport = {
                                        scope.launch {
                                            registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) {}
                                            // Note: Compose-friendly launcher simplistic replacement
                                        }
                                    },
                                    onImportHere = { uri -> scope.launch { client.copyDocument(f.id, uri); refresh(client, stack, root) { files = it } } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun refresh(filesClient: FilesClient, stack: Stack<String>, root: String, set: (List<File>) -> Unit) {
        val documentId = stack.lastOrNull() ?: root
        val files = withContext(Dispatchers.IO) {
            if (stack.empty()) {
                val list = filesClient.list(documentId)
                val config = list.firstOrNull { it.id.endsWith("config.yaml") }
                if (config == null || config.size > 0) list else listOf(config)
            } else {
                filesClient.list(documentId)
            }
        }
        set(files)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileRow(
    file: File,
    canEdit: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onExport: () -> Unit,
    onImportHere: (Uri) -> Unit,
) {
    rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(file.name) },
        supportingContent = { Text(if (file.isDirectory) stringResource(R.string.folder) else file.size.toString()) },
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        trailingContent = if (canEdit) {
            { IconButton(onClick = { showMenu = true }) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) } }
        } else null,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    if (showMenu) {
        val rename = remember { mutableStateOf(TextFieldValue(file.name)) }
        ModalBottomSheet(onDismissRequest = { showMenu = false }) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(value = rename.value, onValueChange = { rename.value = it }, label = { Text(stringResource(R.string.new_name)) })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { onRename(rename.value.text); showMenu = false }) { Text(stringResource(R.string.rename)) }
                    TextButton(onClick = { onDelete(); showMenu = false }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
}