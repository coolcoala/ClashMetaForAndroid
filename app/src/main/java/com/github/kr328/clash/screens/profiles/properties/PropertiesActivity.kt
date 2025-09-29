package com.github.kr328.clash.screens.profiles.properties

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.design.R
import com.github.kr328.clash.screens.settings.FilesActivity
import com.github.kr328.clash.ui.theme.AppTheme
import java.util.*

class PropertiesViewModelFactory(
    private val application: Application,
    owner: SavedStateRegistryOwner,
    private val defaultArgs: Bundle?
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        handle["uuid"] = defaultArgs?.getSerializable("uuid") as? UUID
        return PropertiesViewModel(application, handle) as T
    }
}

class PropertiesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = PropertiesViewModelFactory(application, this, Bundle().apply {
            putSerializable("uuid", intent.uuid)
        })
        val viewModel: PropertiesViewModel by viewModels { factory }

        setContent {
            AppTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            NavigationEvent.CloseScreen -> {
                                setResult(RESULT_OK)
                                finish()
                            }
                            is NavigationEvent.ShowToast -> {
                                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                PropertiesScreen(
                    state = uiState,
                    onEvent = viewModel::handleEvent,
                    onNavigateUp = { finish() },
                    onNavigateToFiles = {
                        intent.uuid?.let {
                            startActivity(FilesActivity::class.intent.setUUID(it))
                        }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertiesScreen(
    state: PropertiesUiState,
    onEvent: (PropertiesEvent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToFiles: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_properties)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFiles) {
                        Icon(Icons.Filled.Description, contentDescription = stringResource(R.string.profile_files))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { onEvent(PropertiesEvent.OnSaveClicked) },
                enabled = state.saveEnabled && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.save_and_update))
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onEvent(PropertiesEvent.OnNameChanged(it)) },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = state.isSaving
                )
                if (state.isUrlFieldVisible) {
                    OutlinedTextField(
                        value = state.url,
                        onValueChange = { onEvent(PropertiesEvent.OnUrlChanged(it)) },
                        label = { Text(stringResource(R.string.url)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = state.isSaving
                    )
                }
                OutlinedTextField(
                    value = state.intervalMinutes,
                    onValueChange = { onEvent(PropertiesEvent.OnIntervalChanged(it)) },
                    label = { Text(stringResource(R.string.auto_update_minutes)) },
                    placeholder = { Text(stringResource(R.string.zero_do_not_update)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = state.isSaving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (state.isSaving) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}