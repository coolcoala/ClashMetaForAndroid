package com.github.kr328.clash.screens.settings.logs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setFileName
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.screens.settings.logcat.LogcatActivity
import com.github.kr328.clash.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

class LogsActivity : ComponentActivity() {
    private val viewModel: LogsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LogsScreen(
                    viewModel = viewModel,
                    finishActivity = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.logs)) },
                navigationIcon = {
                    IconButton(onClick = finishActivity) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { showConfirmDialog = true }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_all)) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    context.startActivity(LogcatActivity::class.intent)
                }
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.start_logging))
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.logFiles.isEmpty() -> {
                    Text(stringResource(R.string.no_saved_log_files), modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.logFiles, key = { it.fileName }) { logFile ->
                            LogItem(
                                logFile = logFile,
                                onClick = {
                                    val intent = LogcatActivity::class.intent.setFileName(logFile.fileName)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_all_logs_confirm)) },
            text = { Text(stringResource(R.string.action_cannot_be_undone)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllLogs()
                    showConfirmDialog = false
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun LogItem(logFile: LogFile, onClick: () -> Unit) {
    val dateFormatter = remember {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(logFile.fileName) },
        supportingContent = { Text(dateFormatter.format(logFile.date)) },
        leadingContent = { Icon(Icons.Outlined.Description, contentDescription = null) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.open)) }
    )
}