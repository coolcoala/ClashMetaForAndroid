package com.github.kr328.clash.screens.settings.meta_features

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.screens.settings.PreferenceList
import com.github.kr328.clash.screens.settings.PreferenceTextField
import com.github.kr328.clash.screens.settings.SectionTitle
import com.github.kr328.clash.ui.theme.AppTheme
import kotlinx.coroutines.launch
import com.github.kr328.clash.design.R as DesignR

class MetaFeatureSettingsActivity : ComponentActivity() {
    private val viewModel: MetaFeatureSettingsViewModel by viewModels()

    override fun onStop() {
        super.onStop()
        viewModel.saveChanges()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MetaSettingsEvent.ShowToast -> {
                            Toast.makeText(this@MetaFeatureSettingsActivity, event.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        setContent {
            AppTheme {
                MetaFeatureSettingsScreen(
                    viewModel = viewModel,
                    finishActivity = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaFeatureSettingsScreen(
    viewModel: MetaFeatureSettingsViewModel,
    finishActivity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = uiState.configuration
    val isLoading = uiState.isLoading

    var showResetDialog by remember { mutableStateOf(false) }

    var geoFileTypeToImport by remember { mutableStateOf<GeoFileType?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            geoFileTypeToImport?.let { type ->
                viewModel.importGeoFile(uri, type)
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(DesignR.string.meta_features)) },
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
                    PreferenceList(
                        title = stringResource(DesignR.string.unified_delay),
                        valueText = when (configuration.unifiedDelay) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    unifiedDelay = when (it) {
                                        1 -> true; 2 -> false; else -> null
                                    }
                                )
                            )
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.geodata_mode),
                        valueText = when (configuration.geodataMode) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    geodataMode = when (it) {
                                        1 -> true; 2 -> false; else -> null
                                    }
                                )
                            )
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.tcp_concurrent),
                        valueText = when (configuration.tcpConcurrent) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    tcpConcurrent = when (it) {
                                        1 -> true; 2 -> false; else -> null
                                    }
                                )
                            )
                        }
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.find_process_mode),
                        valueText = when (configuration.findProcessMode) {
                            ConfigurationOverride.FindProcessMode.Off -> stringResource(DesignR.string.off)
                            ConfigurationOverride.FindProcessMode.Strict -> stringResource(DesignR.string.strict)
                            ConfigurationOverride.FindProcessMode.Always -> stringResource(DesignR.string.always)
                            else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.off),
                            stringResource(DesignR.string.strict),
                            stringResource(DesignR.string.always)
                        ),
                        onSelected = { index ->
                            val newMode = when (index) {
                                1 -> ConfigurationOverride.FindProcessMode.Off
                                2 -> ConfigurationOverride.FindProcessMode.Strict
                                3 -> ConfigurationOverride.FindProcessMode.Always
                                else -> null
                            }
                            viewModel.updateConfiguration(configuration.copy(findProcessMode = newMode))
                        }
                    )
                }

                item { SectionTitle(title = stringResource(DesignR.string.sniffer_setting)) }

                val snifferEnabled = configuration.sniffer.enable != false

                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.sniffer),
                        valueText = when (configuration.sniffer.enable) {
                            true -> stringResource(DesignR.string.enabled); false -> stringResource(
                                DesignR.string.disabled
                            ); else -> stringResource(DesignR.string.dont_modify)
                        },
                        options = listOf(
                            stringResource(DesignR.string.dont_modify),
                            stringResource(DesignR.string.enabled),
                            stringResource(DesignR.string.disabled)
                        ),
                        onSelected = { index ->
                            val enabled = when (index) {
                                1 -> true; 2 -> false; else -> null
                            }
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        enable = enabled
                                    )
                                )
                            )
                        }
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.sniff_http_ports),
                        subtitle = stringResource(DesignR.string.ports_comma_separated),
                        value = configuration.sniffer.sniff.http.ports?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        sniff = configuration.sniffer.sniff.copy(
                                            http = configuration.sniffer.sniff.http.copy(ports = list.ifEmpty { null })
                                        )
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.sniff_http_override_destination),
                        valueText = when (configuration.sniffer.sniff.http.overrideDestination) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        sniff = configuration.sniffer.sniff.copy(
                                            http = configuration.sniffer.sniff.http.copy(
                                                overrideDestination = when (it) {
                                                    1 -> true; 2 -> false; else -> null
                                                }
                                            )
                                        )
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.sniff_tls_ports),
                        subtitle = stringResource(DesignR.string.ports_comma_separated),
                        value = configuration.sniffer.sniff.tls.ports?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        sniff = configuration.sniffer.sniff.copy(
                                            tls = configuration.sniffer.sniff.tls.copy(ports = list.ifEmpty { null })
                                        )
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.sniff_tls_override_destination),
                        valueText = when (configuration.sniffer.sniff.tls.overrideDestination) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        sniff = configuration.sniffer.sniff.copy(
                                            tls = configuration.sniffer.sniff.tls.copy(
                                                overrideDestination = when (it) {
                                                    1 -> true; 2 -> false; else -> null
                                                }
                                            )
                                        )
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.sniff_quic_ports),
                        subtitle = stringResource(DesignR.string.ports_comma_separated),
                        value = configuration.sniffer.sniff.quic.ports?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        sniff = configuration.sniffer.sniff.copy(
                                            quic = configuration.sniffer.sniff.quic.copy(ports = list.ifEmpty { null })
                                        )
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.sniff_quic_override_destination),
                        valueText = when (configuration.sniffer.sniff.quic.overrideDestination) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        sniff = configuration.sniffer.sniff.copy(
                                            quic = configuration.sniffer.sniff.quic.copy(
                                                overrideDestination = when (it) {
                                                    1 -> true; 2 -> false; else -> null
                                                }
                                            )
                                        )
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceList(
                        title = stringResource(DesignR.string.force_dns_mapping),
                        valueText = when (configuration.sniffer.forceDnsMapping) {
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
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        forceDnsMapping = when (it) {
                                            1 -> true; 2 -> false; else -> null
                                        }
                                    )
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.force_domain),
                        subtitle = stringResource(DesignR.string.domains_comma_separated),
                        value = configuration.sniffer.forceDomain?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        forceDomain = list.ifEmpty { null })
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.skip_domain),
                        subtitle = stringResource(DesignR.string.domains_comma_separated),
                        value = configuration.sniffer.skipDomain?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        skipDomain = list.ifEmpty { null })
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.skip_src_address),
                        subtitle = stringResource(DesignR.string.addresses_cidr_comma_separated),
                        value = configuration.sniffer.skipSrcAddress?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        skipSrcAddress = list.ifEmpty { null })
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }
                item {
                    PreferenceTextField(
                        title = stringResource(DesignR.string.skip_dst_address),
                        subtitle = stringResource(DesignR.string.addresses_cidr_comma_separated),
                        value = configuration.sniffer.skipDstAddress?.joinToString(", ") ?: "",
                        onValueChange = { text ->
                            val list = text.split(',').map(String::trim).filter(String::isNotEmpty)
                            viewModel.updateConfiguration(
                                configuration.copy(
                                    sniffer = configuration.sniffer.copy(
                                        skipDstAddress = list.ifEmpty { null })
                                )
                            )
                        },
                        enabled = snifferEnabled
                    )
                }

                item { SectionTitle(title = stringResource(DesignR.string.geox_files)) }
                item {
                    PreferenceActionItem(
                        title = stringResource(DesignR.string.import_geoip_file),
                        subtitle = stringResource(DesignR.string.press_to_import),
                        onClick = {
                            geoFileTypeToImport = GeoFileType.GeoIP
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }
                item {
                    PreferenceActionItem(
                        title = stringResource(DesignR.string.import_geosite_file),
                        subtitle = stringResource(DesignR.string.press_to_import),
                        onClick = {
                            geoFileTypeToImport = GeoFileType.GeoSite
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }
                item {
                    PreferenceActionItem(
                        title = stringResource(DesignR.string.import_country_file),
                        subtitle = stringResource(DesignR.string.press_to_import),
                        onClick = {
                            geoFileTypeToImport = GeoFileType.Country
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }
                item {
                    PreferenceActionItem(
                        title = stringResource(DesignR.string.import_asn_file),
                        subtitle = stringResource(DesignR.string.press_to_import),
                        onClick = {
                            geoFileTypeToImport = GeoFileType.ASN
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }

                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(DesignR.string.reset_meta_settings)) },
            text = { Text(text = stringResource(DesignR.string.reset_meta_settings_message)) },
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

@Composable
private fun PreferenceActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(Icons.Default.UploadFile, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}