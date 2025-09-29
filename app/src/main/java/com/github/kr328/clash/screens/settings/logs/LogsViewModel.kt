package com.github.kr328.clash.screens.settings.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.util.logsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LogsUiState(
    val logFiles: List<LogFile> = emptyList(),
    val isLoading: Boolean = true,
)

class LogsViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val files = loadFiles()
            _uiState.update { it.copy(logFiles = files, isLoading = false) }
        }
    }

    fun deleteAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().logsDir.deleteRecursively()
            withContext(Dispatchers.Main) {
                refresh()
            }
        }
    }

    private suspend fun loadFiles(): List<LogFile> = withContext(Dispatchers.IO) {
        val logsDir = getApplication<Application>().cacheDir.resolve("logs")
        val files = logsDir.listFiles()?.toList() ?: emptyList()
        files
            .mapNotNull { LogFile.parseFromFileName(it.name) }
            .sortedByDescending { it.date }
    }
}