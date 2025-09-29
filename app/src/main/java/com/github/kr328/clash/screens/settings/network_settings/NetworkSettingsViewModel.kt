package com.github.kr328.clash.screens.settings.network_settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkSettingsUiState(
    val enableVpn: Boolean = true,
    val bypassPrivateNetwork: Boolean = true,
    val dnsHijacking: Boolean = false,
    val allowBypass: Boolean = true,
    val allowIpv6: Boolean = false,
    val systemProxy: Boolean = false,
    val tunStackMode: String = "mixed",
    val accessControlMode: AccessControlMode = AccessControlMode.AcceptAll,
)

class NetworkSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val uiStore = UiStore(app)
    private val serviceStore = ServiceStore(app)

    private val _uiState = MutableStateFlow(NetworkSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.value = NetworkSettingsUiState(
            enableVpn = uiStore.enableVpn,
            bypassPrivateNetwork = serviceStore.bypassPrivateNetwork,
            dnsHijacking = serviceStore.dnsHijacking,
            allowBypass = serviceStore.allowBypass,
            allowIpv6 = serviceStore.allowIpv6,
            systemProxy = serviceStore.systemProxy,
            tunStackMode = serviceStore.tunStackMode,
            accessControlMode = serviceStore.accessControlMode
        )
    }

    fun setEnableVpn(enabled: Boolean) {
        uiStore.enableVpn = enabled
        _uiState.value = _uiState.value.copy(enableVpn = enabled)
    }

    fun setBypassPrivateNetwork(enabled: Boolean) {
        serviceStore.bypassPrivateNetwork = enabled
        _uiState.value = _uiState.value.copy(bypassPrivateNetwork = enabled)
    }

    fun setDnsHijacking(enabled: Boolean) {
        serviceStore.dnsHijacking = enabled
        _uiState.value = _uiState.value.copy(dnsHijacking = enabled)
    }

    fun setAllowBypass(enabled: Boolean) {
        serviceStore.allowBypass = enabled
        _uiState.value = _uiState.value.copy(allowBypass = enabled)
    }

    fun setAllowIpv6(enabled: Boolean) {
        serviceStore.allowIpv6 = enabled
        _uiState.value = _uiState.value.copy(allowIpv6 = enabled)
    }

    fun setSystemProxy(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= 29) {
            serviceStore.systemProxy = enabled
            _uiState.value = _uiState.value.copy(systemProxy = enabled)
        }
    }

    fun setTunStackMode(index: Int) {
        val mode = when (index) { 0 -> "system"; 1 -> "gvisor"; else -> "mixed" }
        serviceStore.tunStackMode = mode
        _uiState.value = _uiState.value.copy(tunStackMode = mode)
    }

    fun setAccessControlMode(index: Int) {
        val mode = when (index) { 0 -> AccessControlMode.AcceptAll; 1 -> AccessControlMode.AcceptSelected; else -> AccessControlMode.DenySelected }
        serviceStore.accessControlMode = mode
        _uiState.value = _uiState.value.copy(accessControlMode = mode)
    }
}