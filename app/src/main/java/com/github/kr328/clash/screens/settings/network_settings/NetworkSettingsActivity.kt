package com.github.kr328.clash.screens.settings.network_settings

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.ui.theme.AppTheme
import com.github.kr328.clash.design.R
import com.github.kr328.clash.screens.settings.PreferenceList
import com.github.kr328.clash.screens.settings.PreferenceSwitch
import com.github.kr328.clash.screens.settings.access_control.AccessControlActivity

class NetworkSettingsActivity : ComponentActivity() {
    private val viewModel: NetworkSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                NetworkSettingsScreen(
                    viewModel = viewModel,
                    finishActivity = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSettingsScreen(
    viewModel: NetworkSettingsViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    val isVpnModeEnabled = uiState.enableVpn

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.network)) },
            navigationIcon = { IconButton(onClick = finishActivity) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
        )
    }) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.route_via_vpn),
                    subtitle = stringResource(R.string.route_via_vpn_summary),
                    checked = uiState.enableVpn,
                    onCheckedChange = viewModel::setEnableVpn
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.bypass_private_network),
                    subtitle = stringResource(R.string.bypass_private_network_summary),
                    checked = uiState.bypassPrivateNetwork,
                    onCheckedChange = viewModel::setBypassPrivateNetwork,
                    enabled = isVpnModeEnabled
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.dns_hijacking),
                    subtitle = stringResource(R.string.dns_hijacking_summary),
                    checked = uiState.dnsHijacking,
                    onCheckedChange = viewModel::setDnsHijacking,
                    enabled = isVpnModeEnabled
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.allow_bypass),
                    subtitle = stringResource(R.string.allow_bypass_summary),
                    checked = uiState.allowBypass,
                    onCheckedChange = viewModel::setAllowBypass,
                    enabled = isVpnModeEnabled
                )
            }
            item {
                PreferenceSwitch(
                    title = stringResource(R.string.ipv6),
                    subtitle = stringResource(R.string.ipv6_summary),
                    checked = uiState.allowIpv6,
                    onCheckedChange = viewModel::setAllowIpv6,
                    enabled = isVpnModeEnabled
                )
            }
            if (Build.VERSION.SDK_INT >= 29) {
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.system_proxy),
                        subtitle = stringResource(R.string.system_proxy_summary),
                        checked = uiState.systemProxy,
                        onCheckedChange = viewModel::setSystemProxy,
                        enabled = isVpnModeEnabled
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }

            item {
                PreferenceList(
                    title = stringResource(R.string.tun_stack),
                    valueText = when (uiState.tunStackMode) {
                        "system" -> "System"; "gvisor" -> "gVisor"; else -> "Mixed"
                    },
                    options = listOf("System", "gVisor", "Mixed"),
                    onSelected = viewModel::setTunStackMode,
                    enabled = isVpnModeEnabled
                )
            }
            item {
                PreferenceList(
                    title = stringResource(R.string.access_control_title),
                    valueText = when (uiState.accessControlMode) {
                        AccessControlMode.AcceptAll -> stringResource(R.string.allow_all_apps)
                        AccessControlMode.AcceptSelected -> stringResource(R.string.allow_selected_apps)
                        AccessControlMode.DenySelected -> stringResource(R.string.deny_selected_apps)
                    },
                    options = listOf(
                        stringResource(R.string.allow_all_apps),
                        stringResource(R.string.allow_selected_apps),
                        stringResource(R.string.deny_selected_apps)
                    ),
                    onSelected = viewModel::setAccessControlMode,
                    enabled = isVpnModeEnabled
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Apps,
                    title = stringResource(R.string.app_list),
                    subtitle = stringResource(R.string.access_control_packages_summary),
                    onClick = { ctx.startActivity(AccessControlActivity::class.intent) }
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