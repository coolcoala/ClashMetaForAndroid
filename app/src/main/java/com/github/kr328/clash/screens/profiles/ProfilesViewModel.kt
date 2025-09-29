package com.github.kr328.clash.screens.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.UiEvents
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class UpdateStatus {
    SUCCESS,
    FAILURE
}
data class ProfilesUiState(
    val isLoading: Boolean = true,
    val profiles: List<Profile> = emptyList(),
    val loading: Boolean = false,
    val updateStatus: Map<UUID, UpdateStatus> = emptyMap()
)

class ProfilesViewModel(app: Application) : AndroidViewModel(app), Broadcasts.Observer {
    private val _state = MutableStateFlow(ProfilesUiState())
    val state: StateFlow<ProfilesUiState> = _state
    private val statusResetJobs = mutableMapOf<UUID, Job>()

    init {
        Remote.broadcasts.addObserver(this)
        refresh()
    }

    override fun onCleared() {
        super.onCleared()
        Remote.broadcasts.removeObserver(this)
    }

    override fun onProfileChanged() {
        refresh()
    }

    override fun onStarted() {}
    override fun onStopped(cause: String?) {}
    override fun onProfileLoaded() {}
    override fun onServiceRecreated() {}
    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if (uuid == null) return

        statusResetJobs[uuid]?.cancel()

        statusResetJobs[uuid] = viewModelScope.launch {
            val newStatusMap = _state.value.updateStatus + (uuid to UpdateStatus.SUCCESS)
            _state.value = _state.value.copy(updateStatus = newStatusMap)

            delay(5000)

            val clearedStatusMap = _state.value.updateStatus - uuid
            _state.value = _state.value.copy(updateStatus = clearedStatusMap)
        }
    }
    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if (uuid == null) return

        statusResetJobs[uuid]?.cancel()
        statusResetJobs[uuid] = viewModelScope.launch {
            val newStatusMap = _state.value.updateStatus + (uuid to UpdateStatus.FAILURE)
            _state.value = _state.value.copy(updateStatus = newStatusMap)

            delay(5000)

            val clearedStatusMap = _state.value.updateStatus - uuid
            _state.value = _state.value.copy(updateStatus = clearedStatusMap)
        }
    }


    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val list = withProfile { queryAll() }
                _state.value = _state.value.copy(profiles = list)
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun updateAllProfiles() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                withProfile {
                    queryAll().forEach { profile ->
                        if (profile.imported && profile.type != Profile.Type.File) {
                            update(profile.uuid)
                        }
                    }
                }
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun setActive(profile: Profile) {
        viewModelScope.launch {
            withProfile { if (profile.imported) setActive(profile) }
            UiEvents.emit(UiEvents.Event.ProfileSelected(profile.name))
        }
    }

    fun delete(profile: Profile) {
        viewModelScope.launch {
            withProfile { delete(profile.uuid) }
        }
    }

    fun update(profile: Profile) {
        viewModelScope.launch {
            withProfile { update(profile.uuid) }
        }
    }
}