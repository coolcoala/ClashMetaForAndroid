package com.github.kr328.clash.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.store.ServiceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SettingsUiState(
    val darkMode: DarkMode = DarkMode.Auto,
    val enableVpn: Boolean = true,
    val hideAppIcon: Boolean = false,
    val allowIpv6: Boolean = false,
    val bypassPrivateNetwork: Boolean = true,
    val dnsHijacking: Boolean = true,
    val allowBypass: Boolean = true,
    val systemProxy: Boolean = true,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val uiStore = UiStore(app)
    private val srvStore = ServiceStore(app)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        load()
    }

    fun load() {
        _state.value = SettingsUiState(
            darkMode = uiStore.darkMode,
            enableVpn = uiStore.enableVpn,
            hideAppIcon = uiStore.hideAppIcon,
            allowIpv6 = srvStore.allowIpv6,
            bypassPrivateNetwork = srvStore.bypassPrivateNetwork,
            dnsHijacking = srvStore.dnsHijacking,
            allowBypass = srvStore.allowBypass,
            systemProxy = srvStore.systemProxy,
        )
    }

    fun setDarkMode(mode: DarkMode) {
        uiStore.darkMode = mode
        load()
    }

    fun setEnableVpn(value: Boolean) {
        uiStore.enableVpn = value
        load()
    }

    fun setHideAppIcon(value: Boolean) {
        uiStore.hideAppIcon = value
        load()
    }

    fun setAllowIpv6(value: Boolean) {
        srvStore.allowIpv6 = value
        load()
    }

    fun setBypassPrivateNetwork(value: Boolean) {
        srvStore.bypassPrivateNetwork = value
        load()
    }

    fun setDnsHijacking(value: Boolean) {
        srvStore.dnsHijacking = value
        load()
    }

    fun setAllowBypass(value: Boolean) {
        srvStore.allowBypass = value
        load()
    }

    fun setSystemProxy(value: Boolean) {
        srvStore.systemProxy = value
        load()
    }
}


