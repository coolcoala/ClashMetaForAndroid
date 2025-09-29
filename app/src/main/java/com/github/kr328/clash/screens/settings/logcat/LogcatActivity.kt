package com.github.kr328.clash.screens.settings.logcat

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.kr328.clash.LogcatService
import com.github.kr328.clash.common.util.fileName
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.R
import com.github.kr328.clash.screens.settings.logs.LogsActivity
import com.github.kr328.clash.ui.theme.AppTheme
import kotlinx.coroutines.launch

class LogcatViewModelFactory(
	private val application: Application,
	private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		if (modelClass.isAssignableFrom(LogcatViewModel::class.java)) {
			@Suppress("UNCHECKED_CAST")
			return LogcatViewModel(application, savedStateHandle) as T
		}
		throw IllegalArgumentException("Unknown ViewModel class")
	}
}

class LogcatActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val factory = LogcatViewModelFactory(
			application,
			SavedStateHandle(mapOf("fileName" to intent?.fileName))
		)
		val viewModel: LogcatViewModel by viewModels { factory }

		setContent {
			AppTheme {
				val uiState by viewModel.uiState.collectAsState()

				if (!uiState.isFileMode) {
					DisposableEffect(Unit) {
						viewModel.startStreaming(applicationContext)
						onDispose { }
					}
				}

				LogcatScreen(
					state = uiState,
					onEvent = { event ->
						when (event) {
							LogcatEvent.ConfirmDelete -> {
								viewModel.handleEvent(event, applicationContext)
								finish()
							}
							else -> viewModel.handleEvent(event, applicationContext)
						}
					},
					onCloseStreaming = {
						stopService(LogcatService::class.intent)
						startActivity(LogsActivity::class.intent)
						finish()
					},
					onNavigateUp = { finish() }
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogcatScreen(
	state: LogcatUiState,
	onEvent: (LogcatEvent) -> Unit,
	onCloseStreaming: () -> Unit,
	onNavigateUp: () -> Unit
) {
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()

	LaunchedEffect(state.snackbarMessage) {
		if (state.snackbarMessage != null) {
			scope.launch {
				snackbarHostState.showSnackbar(state.snackbarMessage)
				onEvent(LogcatEvent.SnackbarShown)
			}
		}
	}

	Scaffold(
		snackbarHost = { SnackbarHost(snackbarHostState) },
		topBar = {
			LogcatTopAppBar(
				state = state,
				onEvent = onEvent,
				onNavigateUp = onNavigateUp,
				onCloseStreaming = onCloseStreaming
			)
		}
	) { innerPadding ->
		when {
			state.isLoading -> {
				Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			}
			state.messages.isEmpty() -> {
				Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
					Text(stringResource(R.string.empty))
				}
			}
			else -> {
				LogList(
					messages = state.messages,
					modifier = Modifier.padding(innerPadding)
				)
			}
		}

		if (state.showConfirmDelete) {
			DeleteConfirmDialog(onEvent)
		}
	}
}

@Composable
private fun LogList(messages: List<LogMessage>, modifier: Modifier = Modifier) {
	val listState = rememberLazyListState()

	LaunchedEffect(messages.size) {
		if (messages.isNotEmpty()) {
			listState.animateScrollToItem(messages.lastIndex)
		}
	}

	LazyColumn(
		state = listState,
		modifier = modifier.fillMaxSize()
	) {
		items(messages, key = { it.hashCode() }) { msg ->
			LogcatRow(msg)
		}
	}
}

@Composable
private fun LogcatRow(msg: LogMessage) {
	val color = when (msg.level) {
		LogMessage.Level.Debug -> LocalContentColor.current
		LogMessage.Level.Info -> MaterialTheme.colorScheme.primary
		LogMessage.Level.Warning -> MaterialTheme.colorScheme.tertiary
		LogMessage.Level.Error -> MaterialTheme.colorScheme.error
		else -> LocalContentColor.current
	}
	Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
		Text(
			text = msg.time.toString(),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(Modifier.height(2.dp))
		Text(msg.message, color = color)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogcatTopAppBar(
	state: LogcatUiState,
	onEvent: (LogcatEvent) -> Unit,
	onNavigateUp: () -> Unit,
	onCloseStreaming: () -> Unit,
) {
	val context = LocalContext.current
	val exportLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.CreateDocument("text/plain"),
		onResult = { uri: Uri? -> onEvent(LogcatEvent.Export(uri)) }
	)

	CenterAlignedTopAppBar(
		title = { Text(state.title) },
		navigationIcon = {
			IconButton(onClick = onNavigateUp) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
			}
		},
		actions = {
			if (state.isFileMode) {
				IconButton(onClick = { onEvent(LogcatEvent.Delete) }) {
					Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
				}
				IconButton(onClick = { exportLauncher.launch(state.file?.fileName ?: "log.txt") }) {
					Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.export))
				}
			} else {
				IconButton(onClick = onCloseStreaming) {
					Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_streaming))
				}
			}
		}
	)
}

@Composable
private fun DeleteConfirmDialog(onEvent: (LogcatEvent) -> Unit) {
	AlertDialog(
		onDismissRequest = { onEvent(LogcatEvent.DismissDeleteDialog) },
		title = { Text(stringResource(R.string.delete_log_confirm)) },
		text = { Text(stringResource(R.string.action_cannot_be_undone)) },
		confirmButton = {
			TextButton(onClick = { onEvent(LogcatEvent.ConfirmDelete) }) {
				Text(stringResource(R.string.delete))
			}
		},
		dismissButton = {
			TextButton(onClick = { onEvent(LogcatEvent.DismissDeleteDialog) }) {
				Text(stringResource(R.string.cancel))
			}
		}
	)
}