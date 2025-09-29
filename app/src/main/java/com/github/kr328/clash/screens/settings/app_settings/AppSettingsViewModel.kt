package com.github.kr328.clash.screens.settings.app_settings

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.RestartReceiver
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.mainActivityAlias
import com.github.kr328.clash.service.store.ServiceStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AppSettingsUiState(
    val darkMode: DarkMode = DarkMode.Auto,
    val hideAppIcon: Boolean = false,
    val autoRestart: Boolean = false,
    val dynamicNotification: Boolean = false,
)

sealed class AppSettingsEvent {
    data object RecreateAllActivities : AppSettingsEvent()
}

class AppSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val uiStore = UiStore(app)
    private val serviceStore = ServiceStore(app)
    private val packageManager = app.packageManager

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AppSettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        _uiState.value = AppSettingsUiState(
            darkMode = uiStore.darkMode,
            hideAppIcon = uiStore.hideAppIcon,
            autoRestart = isComponentEnabled(RestartReceiver::class.componentName),
            dynamicNotification = serviceStore.dynamicNotification
        )
    }

    fun setDarkMode(mode: DarkMode) {
        uiStore.darkMode = mode
        _uiState.value = _uiState.value.copy(darkMode = mode)

        viewModelScope.launch {
            _events.send(AppSettingsEvent.RecreateAllActivities)
        }
    }

    fun setHideAppIcon(hide: Boolean) {
        uiStore.hideAppIcon = hide
        _uiState.value = _uiState.value.copy(hideAppIcon = hide)

        val newState = if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        packageManager.setComponentEnabledSetting(ComponentName(getApplication(), mainActivityAlias), newState, PackageManager.DONT_KILL_APP)
    }

    fun setAutoRestart(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoRestart = enabled)
        setComponentEnabled(RestartReceiver::class.componentName, enabled)
    }

    fun setDynamicNotification(enabled: Boolean) {
        serviceStore.dynamicNotification = enabled
        _uiState.value = _uiState.value.copy(dynamicNotification = enabled)
    }

    private fun isComponentEnabled(componentName: ComponentName): Boolean {
        val status = packageManager.getComponentEnabledSetting(componentName)
        return status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun setComponentEnabled(componentName: ComponentName, enabled: Boolean) {
        val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packageManager.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
    }
}