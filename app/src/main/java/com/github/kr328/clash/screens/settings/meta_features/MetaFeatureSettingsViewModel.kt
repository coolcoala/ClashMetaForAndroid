package com.github.kr328.clash.screens.settings.meta_features

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.util.clashDir
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.github.kr328.clash.design.R

data class MetaSettingsUiState(
    val configuration: ConfigurationOverride? = null,
    val isLoading: Boolean = true,
)

sealed class MetaSettingsEvent {
    data class ShowToast(val message: String) : MetaSettingsEvent()
}

enum class GeoFileType {
    GeoIP, GeoSite, Country, ASN
}

class MetaFeatureSettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(MetaSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<MetaSettingsEvent>()
    val events = _events.receiveAsFlow()

    private var didClear = false

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.value = MetaSettingsUiState(isLoading = true)
            val config = withClash { queryOverride(Clash.OverrideSlot.Persist) }
            _uiState.value = MetaSettingsUiState(configuration = config, isLoading = false)
        }
    }

    fun updateConfiguration(newConfig: ConfigurationOverride) {
        _uiState.value = _uiState.value.copy(configuration = newConfig)
    }

    fun resetToDefaults(onFinished: () -> Unit) {
        viewModelScope.launch {
            didClear = true
            withClash { clearOverride(Clash.OverrideSlot.Persist) }
            onFinished()
        }
    }

    fun importGeoFile(uri: Uri?, type: GeoFileType) {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver

        viewModelScope.launch {
            if (uri == null) {
                _events.send(MetaSettingsEvent.ShowToast(context.getString(R.string.no_file_selected)))
                return@launch
            }

            try {
                val cursor: Cursor? = contentResolver.query(uri, null, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val displayName = if (idx != -1) it.getString(idx) else "unknown"
                        val ext = "." + displayName.substringAfterLast('.', "")
                        val valid = listOf(".metadb", ".db", ".dat", ".mmdb")

                        if (ext !in valid) {
                            _events.send(MetaSettingsEvent.ShowToast(context.getString(R.string.geofile_unknown_format, ext)))
                            return@use
                        }

                        val outName = when (type) {
                            GeoFileType.GeoIP -> "geoip$ext"
                            GeoFileType.GeoSite -> "geosite$ext"
                            GeoFileType.Country -> "country$ext"
                            GeoFileType.ASN -> "ASN$ext"
                        }

                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            FileOutputStream(File(context.clashDir, outName)).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        _events.send(MetaSettingsEvent.ShowToast(context.getString(R.string.geofile_imported_success, outName)))
                    }
                }
            } catch (e: Exception) {
                _events.send(MetaSettingsEvent.ShowToast(context.getString(R.string.import_error, e.message)))
            }
        }
    }

    fun saveChanges() {
        if (!didClear) {
            _uiState.value.configuration?.let { config ->
                viewModelScope.launch {
                    withClash { patchOverride(Clash.OverrideSlot.Persist, config) }
                }
            }
        }
    }
}