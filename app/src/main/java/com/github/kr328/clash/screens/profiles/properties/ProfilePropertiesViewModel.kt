package com.github.kr328.clash.screens.profiles.properties

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.github.kr328.clash.design.R
import java.util.UUID

data class PropertiesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val name: String = "",
    val url: String = "",
    val intervalMinutes: String = "",
    val statusText: String = "",
    val isUrlFieldVisible: Boolean = true,
    val saveEnabled: Boolean = false,
)

sealed interface PropertiesEvent {
    data class OnNameChanged(val name: String) : PropertiesEvent
    data class OnUrlChanged(val url: String) : PropertiesEvent
    data class OnIntervalChanged(val interval: String) : PropertiesEvent
    data object OnSaveClicked : PropertiesEvent
}

sealed interface NavigationEvent {
    data object CloseScreen : NavigationEvent
    data class ShowToast(val message: String) : NavigationEvent
}

class PropertiesViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val profileUuid: UUID? = savedStateHandle.get<UUID>("uuid")
    private var initialProfile: Profile? = null

    private val _uiState = MutableStateFlow(PropertiesUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadInitialProfile()
    }

    private fun loadInitialProfile() {
        if (profileUuid == null) {
            viewModelScope.launch { _navigationEvent.send(NavigationEvent.CloseScreen) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val profile = withProfile { queryByUUID(profileUuid) }
            if (profile == null) {
                _navigationEvent.send(NavigationEvent.CloseScreen)
                return@launch
            }
            initialProfile = profile
            _uiState.update {
                it.copy(
                    isLoading = false,
                    name = profile.name,
                    url = profile.source,
                    intervalMinutes = if (profile.interval == 0L) "" else (profile.interval / 60000).toString(),
                    isUrlFieldVisible = profile.type != Profile.Type.File,
                    saveEnabled = profile.name.isNotBlank()
                )
            }
        }
    }

    fun handleEvent(event: PropertiesEvent) {
        when (event) {
            is PropertiesEvent.OnNameChanged -> {
                _uiState.update { it.copy(name = event.name, saveEnabled = event.name.isNotBlank()) }
            }
            is PropertiesEvent.OnUrlChanged -> {
                _uiState.update { it.copy(url = event.url) }
            }
            is PropertiesEvent.OnIntervalChanged -> {
                if (event.interval.all { it.isDigit() }) {
                    _uiState.update { it.copy(intervalMinutes = event.interval) }
                }
            }
            PropertiesEvent.OnSaveClicked -> {
                saveAndSyncProfile()
            }
        }
    }

    private fun saveAndSyncProfile() {
        val currentState = _uiState.value
        val profile = initialProfile ?: return
        if (!currentState.saveEnabled) return

        val newInterval = currentState.intervalMinutes.toLongOrNull()?.let { it * 60000 } ?: 0

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSaving = true, statusText = getApplication<Application>().getString(R.string.saving)) }
            try {
                withProfile { patch(profile.uuid, currentState.name, currentState.url, newInterval) }

                withProfile {
                    commit(profile.uuid) { fs ->
                        val status = when (fs.action) {
                            FetchStatus.Action.FetchConfiguration -> getApplication<Application>().getString(
                                R.string.format_fetching_configuration, fs.args[0])
                            FetchStatus.Action.FetchProviders -> getApplication<Application>().getString(
                                R.string.format_fetching_provider, fs.args[0])
                            FetchStatus.Action.Verifying -> getApplication<Application>().getString(
                                R.string.verifying)
                        }
                        _uiState.update { it.copy(statusText = status) }
                    }
                }
                _navigationEvent.send(NavigationEvent.CloseScreen)
            } catch (e: Exception) {
                _navigationEvent.send(NavigationEvent.ShowToast(getApplication<Application>().getString(R.string.format_error_message, e.message)))
            } finally {
                _uiState.update { it.copy(isSaving = false, statusText = "") }
            }
        }
    }
}