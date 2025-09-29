package com.github.kr328.clash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.ui.theme.AppTheme
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.design.R

class ProvidersActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var providers by remember { mutableStateOf<List<Provider>>(emptyList()) }
                val updating = remember { mutableStateListOf<Boolean>() }

                LaunchedEffect(Unit) {
                    providers = withContext(Dispatchers.IO) { withClash { queryProviders().sorted() } }
                    updating.clear(); updating.addAll(List(providers.size) { false })
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(stringResource(R.string.providers)) },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                            actions = {
                                IconButton(
                                    onClick = {
                                        // Update all except Inline
                                        providers.forEachIndexed { idx, p ->
                                            if (p.vehicleType != Provider.VehicleType.Inline) {
                                                updating[idx] = true
                                                updateProvider(idx, p, updating) {
                                                    providers = withClash { queryProviders().sorted() }
                                                    if (updating.size < providers.size) {
                                                        val more = providers.size - updating.size
                                                        repeat(more) { updating.add(false) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                ) { Icon(Icons.Filled.Sync, contentDescription = null) }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                        )
                    }
                ) { inner ->
                    if (providers.isEmpty()) {
                        Column(Modifier.padding(inner).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.no_providers))
                        }
                    } else {
                        LazyColumn(Modifier.padding(inner).fillMaxSize()) {
                            itemsIndexed(providers, key = { _, p -> p.name + p.type.name }) { idx, p ->
                                ProviderRow(
                                    provider = p,
                                    updating = updating.getOrNull(idx) == true,
                                    onUpdate = {
                                        updating[idx] = true
                                        updateProvider(idx, p, updating) {
                                            providers = withClash { queryProviders().sorted() }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateProvider(idx: Int, provider: Provider, updating: MutableList<Boolean>, onDone: suspend () -> Unit) {
        lifecycleScope.launch {
            try {
                withClash { updateProvider(provider.type, provider.name) }
            } catch (_: Exception) {
            } finally {
                updating[idx] = false
                onDone()
            }
        }
    }
}

@Composable
private fun ProviderRow(provider: Provider, updating: Boolean, onUpdate: () -> Unit) {
    val inline = provider.vehicleType == Provider.VehicleType.Inline
    ListItem(
        headlineContent = { Text(provider.name) },
        supportingContent = { Text(provider.type.name) },
        trailingContent = if (inline) null else {
            {
                Text(
                    text = if (updating) stringResource(R.string.updating_short) else stringResource(R.string.update),
                    color = if (updating) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = if (updating) Modifier else Modifier.clickable { onUpdate() }
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
    )
}


