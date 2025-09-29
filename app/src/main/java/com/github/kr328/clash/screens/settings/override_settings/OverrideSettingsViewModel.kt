package com.github.kr328.clash.screens.settings.override_settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OverrideSettingsUiState(
    val configuration: ConfigurationOverride? = null,
    val isLoading: Boolean = true,
)

class OverrideSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(OverrideSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.value = OverrideSettingsUiState(isLoading = true)
            val config = withClash { queryOverride(Clash.OverrideSlot.Persist) }
            _uiState.value = OverrideSettingsUiState(configuration = config, isLoading = false)
        }
    }

    fun update(block: (ConfigurationOverride) -> ConfigurationOverride) {
        _uiState.value.configuration?.let {
            _uiState.value = _uiState.value.copy(configuration = block(it))
        }
    }

    fun saveChanges() {
        val configToSave = _uiState.value.configuration ?: return
        viewModelScope.launch {
            withClash { patchOverride(Clash.OverrideSlot.Persist, configToSave) }
        }
    }

    fun resetToDefaults(onFinished: () -> Unit) {
        viewModelScope.launch {
            withClash { clearOverride(Clash.OverrideSlot.Persist) }
            onFinished()
        }
    }
}