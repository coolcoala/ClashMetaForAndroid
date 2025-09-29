package com.github.kr328.clash.screens.settings.access_control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.ui.theme.AppTheme

class AccessControlActivity : ComponentActivity() {
    private val viewModel: AccessControlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AccessControlScreen(viewModel = viewModel, finishActivity = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    viewModel: AccessControlViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val filteredApps = remember(uiState.allApps, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            uiState.allApps
        } else {
            uiState.allApps.filter {
                it.label.contains(uiState.searchQuery, true) ||
                        it.packageName.contains(uiState.searchQuery, true)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.access_control_title)) },
                navigationIcon = { IconButton(onClick = finishActivity) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu)) }
                }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (uiState.allApps.isEmpty()) stringResource(R.string.app_list_empty) else stringResource(R.string.no_results))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in uiState.selectedPackages,
                            onToggle = { isChecked -> viewModel.onAppSelected(app.packageName, isChecked) }
                        )
                    }
                }
            }
        }
    }

    if (showMenu) {
        AccessControlMenuSheet(
            state = uiState,
            onDismiss = { showMenu = false },
            viewModel = viewModel
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        leadingContent = {
            val bmp = remember(app.icon) {
                app.icon.toBitmap()
            }
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(40.dp)
            )
        },
        headlineContent = { Text(app.label) },
        supportingContent = { Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessControlMenuSheet(
    state: AccessControlState,
    onDismiss: () -> Unit,
    viewModel: AccessControlViewModel
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.select_all)) },
                modifier = Modifier.clickable { viewModel.onSelectAll(); onDismiss() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.select_none)) },
                modifier = Modifier.clickable { viewModel.onSelectNone(); onDismiss() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.select_invert)) },
                modifier = Modifier.clickable { viewModel.onSelectInvert(); onDismiss() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.system_apps)) },
                trailingContent = {
                    Switch(
                        checked = state.showSystemApps,
                        onCheckedChange = viewModel::onShowSystemAppsChanged
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.reverse)) },
                trailingContent = {
                    Switch(
                        checked = state.sortReversed,
                        onCheckedChange = viewModel::onSortReversedChanged
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_from_clipboard)) },
                modifier = Modifier.clickable {
                    clipboardManager.getText()?.text?.let { viewModel.onImportFromClipboard(it) }
                    onDismiss()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_to_clipboard)) },
                modifier = Modifier.clickable {
                    val text = state.selectedPackages.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(text))
                    onDismiss()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}