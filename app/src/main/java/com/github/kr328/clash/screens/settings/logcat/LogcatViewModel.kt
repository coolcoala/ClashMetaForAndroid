package com.github.kr328.clash.screens.settings.logcat

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.LogcatService
import com.github.kr328.clash.common.compat.startForegroundServiceCompat
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.log.LogcatFilter
import com.github.kr328.clash.log.LogcatReader
import com.github.kr328.clash.util.logsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class LogcatUiState(
    val title: String = "Logcat",
    val messages: List<LogMessage> = emptyList(),
    val isFileMode: Boolean = false,
    val isLoading: Boolean = true,
    val showConfirmDelete: Boolean = false,
    val file: LogFile? = null,
    val snackbarMessage: String? = null
)

sealed interface LogcatEvent {
    data object Delete : LogcatEvent
    data object ConfirmDelete : LogcatEvent
    data object DismissDeleteDialog : LogcatEvent
    data class Export(val uri: Uri?) : LogcatEvent
    data object SnackbarShown : LogcatEvent
}

class LogcatViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LogcatUiState())
    val uiState = _uiState.asStateFlow()

    private var logcatService: LogcatService? = null
    private var serviceConnection: ServiceConnection? = null
    private var streamingJob: Job? = null

    init {
        val fileName = savedStateHandle.get<String>("fileName")
        if (fileName != null) {
            loadFileMode(fileName)
        } else {
            loadStreamingMode()
        }
    }

    private fun loadFileMode(fileName: String) {
        val file = LogFile.parseFromFileName(fileName)
        if (file == null) {
            _uiState.update { it.copy(isLoading = false, snackbarMessage = "Invalid file") }
            return
        }

        _uiState.update {
            it.copy(
                isFileMode = true,
                file = file,
                title = file.fileName
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val messages = try {
                LogcatReader(getApplication(), file).readAll()
            } catch (e: Exception) {
                emptyList()
            }
            _uiState.update {
                it.copy(messages = messages, isLoading = false)
            }
        }
    }

    private fun loadStreamingMode() {
        _uiState.update { it.copy(isFileMode = false, title = "Logcat") }
    }

    fun startStreaming(context: Context) {
        if (streamingJob?.isActive == true) return

        val appContext = context.applicationContext
        appContext.startForegroundServiceCompat(LogcatService::class.intent)

        streamingJob = viewModelScope.launch {
            logcatService = bindLogcatService(appContext)

            var initial = true
            while (isActive) {
                val snapshot = logcatService?.snapshot(initial)
                if (snapshot != null) {
                    _uiState.update {
                        it.copy(messages = snapshot.messages, isLoading = false)
                    }
                    initial = false
                }
                delay(500)
            }
        }
    }

    fun handleEvent(event: LogcatEvent, context: Context) {
        when (event) {
            LogcatEvent.Delete -> _uiState.update { it.copy(showConfirmDelete = true) }
            LogcatEvent.DismissDeleteDialog -> _uiState.update { it.copy(showConfirmDelete = false) }
            LogcatEvent.ConfirmDelete -> {
                _uiState.update { it.copy(showConfirmDelete = false) }
                viewModelScope.launch(Dispatchers.IO) {
                    _uiState.value.file?.let {
                        context.logsDir.resolve(it.fileName).delete()
                    }
                }
            }
            is LogcatEvent.Export -> {
                val uri = event.uri ?: return
                val currentMessages = _uiState.value.messages
                val currentFile = _uiState.value.file ?: return

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        export(currentMessages, currentFile, uri, context)
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(snackbarMessage = "File exported successfully") }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(snackbarMessage = "Export failed: ${e.message}") }
                        }
                    }
                }
            }
            LogcatEvent.SnackbarShown -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private suspend fun bindLogcatService(context: Context): LogcatService {
        return suspendCoroutine { continuation ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val srv = (service as Binder).queryLocalInterface("") as LogcatService
                    continuation.resume(srv)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    logcatService = null
                }
            }
            serviceConnection = conn
            context.bindService(LogcatService::class.intent, conn, Context.BIND_AUTO_CREATE)
        }
    }

    private suspend fun export(messages: List<LogMessage>, file: LogFile, uri: Uri, context: Context) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { writer ->
                    LogcatFilter(writer, context).use { f ->
                        f.writeHeader(file.date)
                        messages.forEach { f.writeMessage(it) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        streamingJob?.cancel()
        serviceConnection?.let {
            getApplication<Application>().unbindService(it)
        }
        super.onCleared()
    }
}