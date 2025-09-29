package com.github.kr328.clash.screens.profiles.new_profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.kr328.clash.design.R
import com.github.kr328.clash.ui.theme.AppTheme
import kotlinx.coroutines.launch

class NewProfileActivity : ComponentActivity() {
    private val viewModel: NewProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                val url = uri.getQueryParameter("url")
                val name = uri.getQueryParameter("name")

                viewModel.initializeWith(url, name)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is NewProfileSideEffect.FinishActivity -> {
                            setResult(RESULT_OK)
                            finish()
                        }
                        is NewProfileSideEffect.ShowToast -> {
                            Toast.makeText(this@NewProfileActivity, event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        setContent {
            AppTheme {
                NewProfileScreen(
                    viewModel = viewModel,
                    finishActivity = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProfileScreen(
    viewModel: NewProfileViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.create_profile)) },
                navigationIcon = {
                    IconButton(onClick = finishActivity) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.handleEvent(NewProfileEvent.OnSaveClicked) },
                enabled = uiState.saveEnabled && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.save_and_activate))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.url,
                    onValueChange = { viewModel.handleEvent(NewProfileEvent.OnUrlChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_url)) },
                    leadingIcon = { Icon(Icons.Default.Http, contentDescription = null) },
                    singleLine = true,
                    readOnly = uiState.isSaving,
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let {
                                viewModel.handleEvent(NewProfileEvent.OnUrlChanged(it))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = stringResource(R.string.import_from_clipboard)
                            )
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.advanced_settings)) },
                    trailingContent = {
                        Icon(
                            imageVector = if (uiState.isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.expand)
                        )
                    },
                    modifier = Modifier.clickable { viewModel.handleEvent(NewProfileEvent.ToggleAdvanced) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            item {
                AnimatedVisibility(
                    visible = uiState.isAdvancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.handleEvent(NewProfileEvent.OnNameChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.profile_name_optional)) },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                            singleLine = true,
                            readOnly = uiState.isSaving
                        )
                        OutlinedTextField(
                            value = uiState.intervalMinutes,
                            onValueChange = { viewModel.handleEvent(NewProfileEvent.OnIntervalChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.auto_update_minutes)) },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            placeholder = { Text(stringResource(R.string.zero_do_not_update)) },
                            singleLine = true,
                            readOnly = uiState.isSaving,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            if (uiState.isSaving) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = uiState.statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}