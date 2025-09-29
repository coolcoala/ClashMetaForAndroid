package com.github.kr328.clash.screens.profiles.new_profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
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
import java.util.UUID
import com.github.kr328.clash.design.R

data class NewProfileUiState(
    val url: String = "",
    val name: String = "",
    val intervalMinutes: String = "1440",
    val isAdvancedExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val saveEnabled: Boolean = false,
    val statusText: String = ""
)

sealed interface NewProfileEvent {
    data class OnUrlChanged(val url: String) : NewProfileEvent
    data class OnNameChanged(val name: String) : NewProfileEvent
    data class OnIntervalChanged(val interval: String) : NewProfileEvent
    data object OnSaveClicked : NewProfileEvent
    data object ToggleAdvanced : NewProfileEvent
}

sealed interface NewProfileSideEffect {
    data object FinishActivity : NewProfileSideEffect
    data class ShowToast(val message: String) : NewProfileSideEffect
}

class NewProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(NewProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<NewProfileSideEffect>()
    val events = _events.receiveAsFlow()

    fun initializeWith(url: String?, name: String?) {
        if (url.isNullOrBlank()) return

        _uiState.update {
            it.copy(
                url = url,
                name = name ?: "",
                saveEnabled = true,
                isAdvancedExpanded = !name.isNullOrBlank()
            )
        }
    }

    fun handleEvent(event: NewProfileEvent) {
        when (event) {
            is NewProfileEvent.OnUrlChanged -> {
                _uiState.update {
                    it.copy(
                        url = event.url,
                        saveEnabled = event.url.isNotBlank()
                    )
                }
            }
            is NewProfileEvent.OnNameChanged -> {
                _uiState.update { it.copy(name = event.name) }
            }
            is NewProfileEvent.OnIntervalChanged -> {
                if (event.interval.all { it.isDigit() }) {
                    _uiState.update { it.copy(intervalMinutes = event.interval) }
                }
            }
            NewProfileEvent.OnSaveClicked -> {
                saveAndSelectProfile()
            }
            NewProfileEvent.ToggleAdvanced -> {
                _uiState.update { it.copy(isAdvancedExpanded = !it.isAdvancedExpanded) }
            }
        }
    }

    private fun saveAndSelectProfile() {
        val currentState = _uiState.value
        if (!currentState.saveEnabled || currentState.isSaving) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSaving = true, statusText = getApplication<Application>().getString(R.string.save_profile)) }

            var temporaryProfileUuid: UUID? = null

            try {
                val profileName = currentState.name.ifBlank {
                    extractNameFromUrl(currentState.url)
                }
                val interval = currentState.intervalMinutes.toLongOrNull()?.let { it * 60000 } ?: 0

                val uuid = withProfile {
                    create(Profile.Type.Url, profileName)
                }
                temporaryProfileUuid = uuid

                withProfile {
                    patch(uuid, profileName, currentState.url, interval)
                }
                withProfile {
                    commit(uuid) { fs ->
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
                val newProfile = withProfile { queryByUUID(uuid) }
                if (newProfile != null) {
                    withProfile { setActive(newProfile) }
                }

                _events.send(NewProfileSideEffect.FinishActivity)

            } catch (e: Exception) {
                temporaryProfileUuid?.let {
                    withProfile { delete(it) }
                }
                _events.send(NewProfileSideEffect.ShowToast(getApplication<Application>().getString(R.string.format_error_message, e.message)))
            } finally {
                _uiState.update { it.copy(isSaving = false, statusText = "") }
            }
        }
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            Uri.parse(url).lastPathSegment?.substringBeforeLast('.') ?: getApplication<Application>().getString(R.string.new_profile)
        } catch (e: Exception) {
            return getApplication<Application>().getString(R.string.new_profile)
        }
    }
}