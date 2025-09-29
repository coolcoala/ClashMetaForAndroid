package com.github.kr328.clash.screens.proxies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.UiEvents
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.remote.StatusClient
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.remote.Broadcasts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ProxyUiState(
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val proxies: List<Proxy> = emptyList(),
    val isSelector: Boolean = true,
    val currentNow: String? = null,
    val loading: Boolean = false,
    val running: Boolean = false,
)

class ProxyViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ProxyUiState())
    val state: StateFlow<ProxyUiState> = _state
    private val MINIMUM_LOADING_MS = 400L

    private val uiStore = UiStore(app)
    private val broadcasts = Broadcasts(app)
    private val observer = object : Broadcasts.Observer {
        override fun onServiceRecreated() { refreshGroups() }
        override fun onStarted() { refreshGroups() }
        override fun onStopped(cause: String?) { refreshGroups() }
        override fun onProfileChanged() { refreshGroups() }
        override fun onProfileUpdateCompleted(uuid: UUID?) { refreshGroups() }
        override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) { /* ignore */ }
        override fun onProfileLoaded() { refreshGroups() }
    }

    init {
        refreshGroups()
        broadcasts.addObserver(observer)
        broadcasts.register()
    }

    override fun onCleared() {
        super.onCleared()
        broadcasts.removeObserver(observer)
        broadcasts.unregister()
    }

    fun refreshGroups() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val running = StatusClient(getApplication()).currentProfile() != null
            _state.value = _state.value.copy(loading = true, running = running)

            if (!running) {
                _state.value = ProxyUiState(running = false, loading = false)
                return@launch
            }

            try {
                val names = withClash { queryProxyGroupNames(uiStore.proxyExcludeNotSelectable) }
                val selected = _state.value.selectedGroup?.takeIf { it in names } ?: names.firstOrNull()
                _state.value = _state.value.copy(groups = names)

                if (selected != null) {
                    fetchProxiesForGroup(selected)
                }
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < MINIMUM_LOADING_MS) {
                    delay(MINIMUM_LOADING_MS - elapsedTime)
                }
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun loadGroup(name: String) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _state.value = _state.value.copy(loading = true)
            try {
                fetchProxiesForGroup(name)
            } finally {
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime < MINIMUM_LOADING_MS) {
                    delay(MINIMUM_LOADING_MS - elapsedTime)
                }
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    private suspend fun fetchProxiesForGroup(name: String) {
        val group = withClash { queryProxyGroup(name, uiStore.proxySort) }
        _state.value = _state.value.copy(
            selectedGroup = name,
            proxies = group.proxies,
            isSelector = group.type == Proxy.Type.Selector,
            currentNow = group.now,
        )
    }

    fun selectProxy(name: String) {
        val group = _state.value.selectedGroup ?: return
        viewModelScope.launch {
            withClash { patchSelector(group, name) }
            loadGroup(group)
            UiEvents.emit(UiEvents.Event.ProxySelected(group, name))
        }
    }

    fun healthCheck() {
        val group = _state.value.selectedGroup ?: return
        viewModelScope.launch {
            withClash { healthCheck(group) }
            loadGroup(group)
        }
    }
}
