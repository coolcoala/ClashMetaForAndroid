package com.github.kr328.clash.screens.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.UiEvents
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.util.trafficDownload
import com.github.kr328.clash.core.util.trafficUpload
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val running: Boolean = false,
    val isConnecting: Boolean = false,
    val profileName: String? = null,
    val profiles: List<Profile> = emptyList(),
    val groupName: String? = null,
    val proxyName: String? = null,
    val proxyDelay: String? = null,
    val groupNames: List<String> = emptyList(),
    val proxiesInSelectedGroup: List<Proxy> = emptyList(),
    val totalDownload: String = "0 B",
    val totalUpload: String = "0 B",
    val isTestingPings: Boolean = false,
    val isLoading: Boolean = true,
    val activeProfileUpload: Long = 0,
    val activeProfileDownload: Long = 0,
    val activeProfileTotal: Long = 0,
    val activeProfileExpire: Long = 0,
    val activeProfileSupportUrl: String? = null,
    val activeProfileCreatedAt: Long = 0,
)

private data class ProxyInfo(
    val groupName: String? = null,
    val proxyName: String? = null,
    val proxyDelay: String? = null,
    val groupNames: List<String> = emptyList(),
    val proxiesInSelectedGroup: List<Proxy> = emptyList()
)

class MainComposeViewModel(app: Application) : AndroidViewModel(app), Broadcasts.Observer {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var tickerJob: Job? = null

    init {
        Remote.broadcasts.addObserver(this)
        refreshAll()
    }

    override fun onCleared() {
        super.onCleared()
        Remote.broadcasts.removeObserver(this)
    }

    override fun onStarted() {
        _state.value = _state.value.copy(isConnecting = true)
        refreshAll()
    }

    override fun onStopped(cause: String?) {
        _state.value = _state.value.copy(isConnecting = false)
        refreshAll()
    }


    override fun onProfileChanged() {
        refreshAll()
    }

    override fun onProfileLoaded() {
        _state.value = _state.value.copy(isConnecting = false)
        refreshAll()
    }
    override fun onServiceRecreated() {}
    override fun onProfileUpdateCompleted(uuid: UUID?) {}
    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {}

    fun refreshAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val running = Remote.broadcasts.clashRunning
                val activeProfile = withProfile { queryActive() }

                runCatching { loadProfiles() }

                if (running) {
                    startTicker()
                    val proxyInfo = fetchActiveProxyState()
                    _state.value = _state.value.copy(
                        running = true,
                        profileName = activeProfile?.name,
                        groupName = proxyInfo.groupName,
                        proxyName = proxyInfo.proxyName,
                        proxyDelay = proxyInfo.proxyDelay,
                        groupNames = proxyInfo.groupNames,
                        proxiesInSelectedGroup = proxyInfo.proxiesInSelectedGroup,
                        activeProfileUpload = activeProfile?.upload ?: 0L,
                        activeProfileDownload = activeProfile?.download ?: 0L,
                        activeProfileTotal = activeProfile?.total ?: 0L,
                        activeProfileExpire = activeProfile?.expire ?: 0L,
                        activeProfileSupportUrl = activeProfile?.supportUrl,
                    )
                } else {
                    stopTicker()
                    _state.value = _state.value.copy(
                        running = false,
                        isConnecting = false,
                        profileName = activeProfile?.name,
                        groupName = null,
                        proxyName = null,
                        proxyDelay = null,
                        groupNames = emptyList(),
                        proxiesInSelectedGroup = emptyList(),
                        totalDownload = "0 B",
                        totalUpload = "0 B",
                        activeProfileUpload = 0L,
                        activeProfileDownload = 0L,
                        activeProfileTotal = 0L,
                        activeProfileExpire = 0L,
                        activeProfileSupportUrl = activeProfile?.supportUrl
                    )
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleService(onVpnPermissionNeeded: (Intent) -> Unit) {
        if (Remote.broadcasts.clashRunning) {
            getApplication<Application>().stopClashService()
        } else {
            val request = getApplication<Application>().startClashService()
            if (request != null) {
                onVpnPermissionNeeded(request)
            }
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {

    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (true) {
                runCatching { fetchTraffic() }
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private suspend fun fetchTraffic() {
        withClash {
            val total = queryTrafficTotal()
            _state.value = _state.value.copy(
                totalDownload = total.trafficDownload(),
                totalUpload = total.trafficUpload()
            )
        }
    }

    private suspend fun fetchActiveProxyState(): ProxyInfo {
        return try {
            withClash {
                val allGroups = queryProxyGroupNames(false)
                if (allGroups.isEmpty()) {
                    ProxyInfo()
                } else {
                    val groupToLoad = _state.value.groupName?.takeIf { it in allGroups } ?: allGroups.first()
                    val group = queryProxyGroup(groupToLoad, ProxySort.Default)
                    val now = group.now
                    val current = group.proxies.find { it.name == now }
                    val delayValue = current?.delay?.takeIf { it.toLong() != 0L && it != 65535 }?.toString() ?: "---"
                    ProxyInfo(
                        groupName = groupToLoad,
                        proxyName = now,
                        proxyDelay = delayValue,
                        groupNames = allGroups,
                        proxiesInSelectedGroup = group.proxies
                    )
                }
            }
        } catch (e: Exception) {
            ProxyInfo()
        }
    }

    private suspend fun loadProfiles() {
        val list = withProfile { queryAll() }
        _state.value = _state.value.copy(profiles = list)
    }

    fun selectGroup(name: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(groupName = name)
            val proxyInfo = fetchActiveProxyState()
            _state.value = _state.value.copy(
                proxyName = proxyInfo.proxyName,
                proxyDelay = proxyInfo.proxyDelay,
                proxiesInSelectedGroup = proxyInfo.proxiesInSelectedGroup
            )
            UiEvents.emit(UiEvents.Event.GroupSelected(name))
        }
    }

    fun selectProxy(name: String) {
        val group = _state.value.groupName ?: return
        viewModelScope.launch {
            withClash { patchSelector(group, name) }
            selectGroup(group)
            UiEvents.emit(UiEvents.Event.ProxySelected(group, name))
        }
    }

    fun setActiveProfile(profile: Profile) {
        viewModelScope.launch {
            withProfile {
                if (profile.imported) setActive(profile)
            }
        }
    }

    fun healthCheckForSelectedGroup() {
        val group = _state.value.groupName ?: return

        viewModelScope.launch {
            _state.update { it.copy(isTestingPings = true) }

            try {
                withClash { healthCheck(group) }
                selectGroup(group)
            } finally {
                _state.update { it.copy(isTestingPings = false) }
            }
        }
    }
}