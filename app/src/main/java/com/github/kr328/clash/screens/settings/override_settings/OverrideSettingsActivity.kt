package com.github.kr328.clash.screens.settings.override_settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.screens.settings.PreferenceList
import com.github.kr328.clash.screens.settings.PreferenceTextField
import com.github.kr328.clash.screens.settings.SectionTitle
import com.github.kr328.clash.ui.theme.AppTheme
import com.github.kr328.clash.design.R as DesignR

class OverrideSettingsActivity : ComponentActivity() {
    private val viewModel: OverrideSettingsViewModel by viewModels()

    override fun onStop() {
        super.onStop()
        viewModel.saveChanges()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                OverrideSettingsScreen(
                    viewModel = viewModel,
                    finishActivity = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverrideSettingsScreen(
    viewModel: OverrideSettingsViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = uiState.configuration
    val isLoading = uiState.isLoading

    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(DesignR.string.override)) },
                navigationIcon = { IconButton(onClick = finishActivity) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(DesignR.string.back)) } },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(DesignR.string.reset))
                    }
                }
            )
        }
    ) { inner ->
        if (isLoading || configuration == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
            ) {
                item { SectionTitle(title = stringResource(DesignR.string.general)) }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.http_port),
                        subtitle = stringResource(DesignR.string.zero_disabled_empty_not_modify),
                        value = configuration.httpPort?.toString() ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(httpPort = it.toIntOrNull()) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.socks_port),
                        subtitle = stringResource(DesignR.string.zero_disabled_empty_not_modify),
                        value = configuration.socksPort?.toString() ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(socksPort = it.toIntOrNull()) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.redirect_port),
                        subtitle = stringResource(DesignR.string.zero_disabled_empty_not_modify),
                        value = configuration.redirectPort?.toString() ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(redirectPort = it.toIntOrNull()) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.tproxy_port),
                        subtitle = stringResource(DesignR.string.zero_disabled_empty_not_modify),
                        value = configuration.tproxyPort?.toString() ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(tproxyPort = it.toIntOrNull()) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.mixed_port),
                        subtitle = stringResource(DesignR.string.zero_disabled_empty_not_modify),
                        value = configuration.mixedPort?.toString() ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(mixedPort = it.toIntOrNull()) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.authentication),
                        subtitle = stringResource(DesignR.string.users_comma_separated),
                        value = configuration.authentication?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.update { it.copy(authentication = list.ifEmpty { null }) }
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.allow_lan),
                        valueText = when (configuration.allowLan) {
                            true -> stringResource(DesignR.string.enabled); false -> stringResource(
                                DesignR.string.disabled
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.enabled),
                            stringResource(DesignR.string.disabled)
                        ),
                        onSelected = {
                            viewModel.update { c ->
                                c.copy(
                                    allowLan = when (it) {
                                        1 -> true; 2 -> false; else -> null
                                    }
                                )
                            }
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.ipv6),
                        valueText = when (configuration.ipv6) {
                            true -> stringResource(DesignR.string.enabled); false -> stringResource(
                                DesignR.string.disabled
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.enabled),
                            stringResource(DesignR.string.disabled)
                        ),
                        onSelected = {
                            viewModel.update { c ->
                                c.copy(
                                    ipv6 = when (it) {
                                        1 -> true; 2 -> false; else -> null
                                    }
                                )
                            }
                        }
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.bind_address),
                        subtitle = stringResource(DesignR.string.bind_address_hint),
                        value = configuration.bindAddress ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(bindAddress = it.ifBlank { null }) } }
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                }

                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.external_controller),
                        subtitle = stringResource(DesignR.string.external_controller_hint),
                        value = configuration.externalController ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(externalController = it.ifBlank { null }) } }
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.external_controller_tls),
                        subtitle = stringResource(DesignR.string.external_controller_tls_hint),
                        value = configuration.externalControllerTLS ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(externalControllerTLS = it.ifBlank { null }) } }
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.allow_origins),
                        subtitle = stringResource(DesignR.string.origins_comma_separated),
                        value = configuration.externalControllerCors.allowOrigins?.joinToString(", ")
                            ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.update { c ->
                                c.copy(
                                    externalControllerCors = c.externalControllerCors.copy(
                                        allowOrigins = list.ifEmpty { null })
                                )
                            }
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.allow_private_network),
                        valueText = when (configuration.externalControllerCors.allowPrivateNetwork) {
                            true -> stringResource(DesignR.string.enabled); false -> stringResource(
                                DesignR.string.disabled
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.enabled),
                            stringResource(DesignR.string.disabled)
                        ),
                        onSelected = {
                            viewModel.update { c ->
                                c.copy(
                                    externalControllerCors = c.externalControllerCors.copy(
                                        allowPrivateNetwork = when (it) {
                                            1 -> true; 2 -> false; else -> null
                                        }
                                    )
                                )
                            }
                        }
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.secret),
                        subtitle = stringResource(DesignR.string.secret_hint),
                        value = configuration.secret ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(secret = it.ifBlank { null }) } }
                    )
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
                        title = stringResource(DesignR.string.mode),
                        valueText = configuration.mode?.name?.replaceFirstChar { it.uppercase() }
                            ?: stringResource(DesignR.string.dont_modify),
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            "Direct",
                            "Global",
                            "Rule"
                        ),
                        onSelected = { index ->
                            val newMode = when (index) {
                                1 -> TunnelState.Mode.Direct; 2 -> TunnelState.Mode.Global; 3 -> TunnelState.Mode.Rule; else -> null
                            }
                            viewModel.update { it.copy(mode = newMode) }
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.log_level),
                        valueText = configuration.logLevel?.name?.replaceFirstChar { it.uppercase() }
                            ?: stringResource(DesignR.string.dont_modify),
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            "Info",
                            "Warning",
                            "Error",
                            "Debug",
                            "Silent"
                        ),
                        onSelected = { index ->
                            val newLevel = when (index) {
                                1 -> LogMessage.Level.Info; 2 -> LogMessage.Level.Warning; 3 -> LogMessage.Level.Error; 4 -> LogMessage.Level.Debug; 5 -> LogMessage.Level.Silent; else -> null
                            }
                            viewModel.update { it.copy(logLevel = newLevel) }
                        }
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.hosts),
                        subtitle = stringResource(DesignR.string.hosts_hint),
                        value = configuration.hosts?.entries?.joinToString(", ") { "${it.key}=${it.value}" }
                            ?: "",
                        onValueChange = { text ->
                            val map = text.split(',')
                                .map { it.trim().split('=', limit = 2) }
                                .filter { it.size == 2 && it[0].isNotBlank() }
                                .associate { it[0] to it[1] }
                            viewModel.update { it.copy(hosts = map.ifEmpty { null }) }
                        }
                    )
                }

                item { SectionTitle(title = stringResource(DesignR.string.dns)) }

                val dnsEnabled = configuration.dns.enable != false

                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.strategy),
                        valueText = when (configuration.dns.enable) {
                            true -> stringResource(DesignR.string.force_enable); false -> stringResource(
                                DesignR.string.use_built_in
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.force_enable),
                            stringResource(DesignR.string.use_built_in)
                        ),
                        onSelected = { index ->
                            val enabled = when (index) {
                                1 -> true; 2 -> false; else -> null
                            }
                            viewModel.update { it.copy(dns = it.dns.copy(enable = enabled)) }
                        }
                    )
                }

                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.prefer_h3),
                        valueText = when (configuration.dns.preferH3) {
                            true -> stringResource(DesignR.string.enabled); false -> stringResource(
                                DesignR.string.disabled
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.enabled),
                            stringResource(DesignR.string.disabled)
                        ),
                        onSelected = {
                            viewModel.update { c ->
                                c.copy(
                                    dns = c.dns.copy(
                                        preferH3 = when (it) {
                                            1 -> true; 2 -> false; else -> null
                                        }
                                    )
                                )
                            }
                        },
                        enabled = dnsEnabled
                    )
                }

                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.listen),
                        subtitle = stringResource(DesignR.string.listen_hint),
                        value = configuration.dns.listen ?: "",
                        onValueChange = { viewModel.update { c -> c.copy(dns = c.dns.copy(listen = it.ifBlank { null })) } },
                        enabled = dnsEnabled
                    )
                }

                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.enhanced_mode),
                        valueText = configuration.dns.enhancedMode?.name?.replaceFirstChar { it.uppercase() }
                            ?: stringResource(DesignR.string.dont_modify),
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            "None",
                            "FakeIp",
                            "Mapping"
                        ),
                        onSelected = { index ->
                            val mode = when (index) {
                                1 -> ConfigurationOverride.DnsEnhancedMode.None; 2 -> ConfigurationOverride.DnsEnhancedMode.FakeIp; 3 -> ConfigurationOverride.DnsEnhancedMode.Mapping; else -> null
                            }
                            viewModel.update { it.copy(dns = it.dns.copy(enhancedMode = mode)) }
                        },
                        enabled = dnsEnabled
                    )
                }

                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.name_server),
                        subtitle = stringResource(DesignR.string.servers_comma_separated),
                        value = configuration.dns.nameServer?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.update { it.copy(dns = it.dns.copy(nameServer = list.ifEmpty { null })) }
                        },
                        enabled = dnsEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.fallback),
                        subtitle = stringResource(DesignR.string.servers_comma_separated),
                        value = configuration.dns.fallback?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.update { it.copy(dns = it.dns.copy(fallback = list.ifEmpty { null })) }
                        },
                        enabled = dnsEnabled
                    )
                }

                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.default_name_server),
                        subtitle = stringResource(DesignR.string.servers_comma_separated),
                        value = configuration.dns.defaultServer?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.update { it.copy(dns = it.dns.copy(defaultServer = list.ifEmpty { null })) }
                        },
                        enabled = dnsEnabled
                    )
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
                        title = stringResource(DesignR.string.geoip_fallback),
                        valueText = when (configuration.dns.fallbackFilter.geoIp) {
                            true -> stringResource(DesignR.string.enabled); false -> stringResource(
                                DesignR.string.disabled
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.enabled),
                            stringResource(DesignR.string.disabled)
                        ),
                        onSelected = {
                            viewModel.update { c ->
                                c.copy(
                                    dns = c.dns.copy(
                                        fallbackFilter = c.dns.fallbackFilter.copy(
                                            geoIp = when (it) {
                                                1 -> true; 2 -> false; else -> null
                                            }
                                        )
                                    )
                                )
                            }
                        },
                        enabled = dnsEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.geoip_fallback_code),
                        subtitle = stringResource(DesignR.string.country_code_hint),
                        value = configuration.dns.fallbackFilter.geoIpCode ?: "",
                        onValueChange = {
                            viewModel.update { c ->
                                c.copy(
                                    dns = c.dns.copy(
                                        fallbackFilter = c.dns.fallbackFilter.copy(geoIpCode = it.ifBlank { null })
                                    )
                                )
                            }
                        },
                        enabled = dnsEnabled
                    )
                }

                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(DesignR.string.reset_override_settings)) },
            text = { Text(text = stringResource(DesignR.string.reset_override_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetToDefaults { finishActivity() }
                    showResetDialog = false
                }) { Text(stringResource(DesignR.string.reset)) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(DesignR.string.cancel)) } }
        )
    }
}