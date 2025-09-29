package com.github.kr328.clash.screens.settings.access_control

import android.Manifest
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccessControlState(
    val allApps: List<AppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showSystemApps: Boolean = false,
    val sortBy: AppInfoSort = AppInfoSort.Label,
    val sortReversed: Boolean = false,
)

class AccessControlViewModel(app: Application) : AndroidViewModel(app) {
    private val uiStore = UiStore(app)
    private val serviceStore = ServiceStore(app)
    private val packageManager = app.packageManager

    private val _uiState = MutableStateFlow(AccessControlState())
    val uiState = _uiState.asStateFlow()

    private val initialSelection: Set<String> = serviceStore.accessControlPackages

    init {
        loadApps()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAppSelected(packageName: String, selected: Boolean) {
        val currentSelection = _uiState.value.selectedPackages.toMutableSet()
        if (selected) {
            currentSelection.add(packageName)
        } else {
            currentSelection.remove(packageName)
        }
        _uiState.update { it.copy(selectedPackages = currentSelection) }
    }

    fun onSelectAll() {
        val allPackages = _uiState.value.allApps.map { it.packageName }.toSet()
        _uiState.update { it.copy(selectedPackages = allPackages) }
    }

    fun onSelectNone() {
        _uiState.update { it.copy(selectedPackages = emptySet()) }
    }

    fun onSelectInvert() {
        val allPackages = _uiState.value.allApps.map { it.packageName }.toSet()
        val currentSelection = _uiState.value.selectedPackages
        _uiState.update { it.copy(selectedPackages = allPackages - currentSelection) }
    }

    fun onImportFromClipboard(text: String) {
        val fromClipboard = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val allPackages = _uiState.value.allApps.map { it.packageName }.toSet()
        _uiState.update { it.copy(selectedPackages = allPackages.intersect(fromClipboard)) }
    }

    fun onShowSystemAppsChanged(show: Boolean) {
        uiStore.accessControlSystemApp = show
        loadApps()
    }

    fun onSortChanged(sortBy: AppInfoSort) {
        uiStore.accessControlSort = sortBy
        loadApps()
    }

    fun onSortReversedChanged(reversed: Boolean) {
        uiStore.accessControlReverse = reversed
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val selected = serviceStore.accessControlPackages
            val showSystem = uiStore.accessControlSystemApp
            val sort = uiStore.accessControlSort
            val reverse = uiStore.accessControlReverse

            val apps = withContext(Dispatchers.IO) {
                val base = compareByDescending<AppInfo> { it.packageName in selected }
                val comparator = if (reverse) base.thenDescending(sort) else base.then(sort)

                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                    .asSequence()
                    .filter { it.packageName != getApplication<Application>().packageName }
                    .filter { it.applicationInfo != null }
                    .filter { it.requestedPermissions?.contains(Manifest.permission.INTERNET) == true || it.applicationInfo!!.uid < Process.FIRST_APPLICATION_UID }
                    .filter { showSystem || (it.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .map { it.toAppInfo(packageManager) }
                    .sortedWith(comparator)
                    .toList()
            }

            _uiState.value = AccessControlState(
                allApps = apps,
                selectedPackages = selected,
                showSystemApps = showSystem,
                sortBy = sort,
                sortReversed = reverse,
                isLoading = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        val finalSelection = _uiState.value.selectedPackages
        if (initialSelection != finalSelection) {
            viewModelScope.launch {
                serviceStore.accessControlPackages = finalSelection
                if (Remote.broadcasts.clashRunning) {
                    getApplication<Application>().stopClashService()
                    while (Remote.broadcasts.clashRunning) { delay(200) }
                    getApplication<Application>().startClashService()
                }
            }
        }
    }
}