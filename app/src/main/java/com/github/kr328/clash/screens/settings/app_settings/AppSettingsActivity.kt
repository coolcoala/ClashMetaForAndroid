package com.github.kr328.clash.screens.settings.app_settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.ui.theme.AppTheme
import com.github.kr328.clash.util.ApplicationObserver
import kotlinx.coroutines.launch
import com.github.kr328.clash.design.R
import com.github.kr328.clash.screens.settings.PreferenceList
import com.github.kr328.clash.screens.settings.PreferenceSwitch
import com.github.kr328.clash.screens.settings.SectionTitle

class AppSettingsActivity : ComponentActivity() {
    private val viewModel: AppSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AppSettingsEvent.RecreateAllActivities -> {
                            ApplicationObserver.createdActivities.forEach { it.recreate() }
                        }
                    }
                }
            }
        }

        setContent {
            AppTheme {
                AppSettingsScreen(viewModel = viewModel, finishActivity = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: AppSettingsViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.interface_and_app)) },
            navigationIcon = { IconButton(onClick = finishActivity) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
        )
    }) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            item {
                SectionTitle(stringResource(R.string.behavior))
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.auto_restart),
                    subtitle = stringResource(R.string.auto_restart_subtitle),
                    checked = uiState.autoRestart,
                    onCheckedChange = viewModel::setAutoRestart
                )
            }

            item {
                SectionTitle(stringResource(R.string.interface_))
            }
            item {
                PreferenceList(
                    title = stringResource(R.string.theme),
                    valueText = when (uiState.darkMode) {
                        DarkMode.Auto -> stringResource(R.string.theme_system)
                        DarkMode.ForceLight -> stringResource(R.string.always_light)
                        DarkMode.ForceDark -> stringResource(R.string.always_dark)
                    },
                    options = DarkMode.entries.map {
                        when (it) {
                            DarkMode.Auto -> stringResource(R.string.theme_system)
                            DarkMode.ForceLight -> stringResource(R.string.always_light)
                            DarkMode.ForceDark -> stringResource(R.string.always_dark)
                        }
                    },
                    onSelected = {
                        viewModel.setDarkMode(DarkMode.entries[it])
                    }
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.hide_app_icon_title),
                    subtitle = stringResource(R.string.hide_app_icon_subtitle),
                    checked = uiState.hideAppIcon,
                    onCheckedChange = viewModel::setHideAppIcon
                )
            }

            item {
                SectionTitle(stringResource(R.string.notifications))
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.show_traffic),
                    subtitle = stringResource(R.string.show_traffic_summary),
                    checked = uiState.dynamicNotification,
                    onCheckedChange = viewModel::setDynamicNotification
                )
            }
        }
    }
}