package com.github.kr328.clash.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.screens.settings.app_settings.AppSettingsActivity
import com.github.kr328.clash.screens.settings.logs.LogsActivity
import com.github.kr328.clash.screens.settings.meta_features.MetaFeatureSettingsActivity
import com.github.kr328.clash.screens.settings.network_settings.NetworkSettingsActivity
import com.github.kr328.clash.screens.settings.override_settings.OverrideSettingsActivity
import com.github.kr328.clash.design.R as DesignR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(DesignR.string.settings)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(DesignR.string.interface_and_app),
                    subtitle = stringResource(DesignR.string.interface_and_app_subtitle),
                    onClick = { ctx.startActivity(AppSettingsActivity::class.intent) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Wifi,
                    title = stringResource(DesignR.string.network),
                    subtitle = stringResource(DesignR.string.network_settings_subtitle),
                    onClick = { ctx.startActivity(NetworkSettingsActivity::class.intent) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(DesignR.string.override),
                    subtitle = stringResource(DesignR.string.override_settings_subtitle),
                    onClick = { ctx.startActivity(OverrideSettingsActivity::class.intent) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(DesignR.string.meta_features),
                    subtitle = stringResource(DesignR.string.meta_features_subtitle),
                    onClick = { ctx.startActivity(MetaFeatureSettingsActivity::class.intent) }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Description,
                    title = stringResource(DesignR.string.logs),
                    subtitle = stringResource(DesignR.string.logs_subtitle),
                    onClick = { ctx.startActivity(LogsActivity::class.intent) }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    )
}