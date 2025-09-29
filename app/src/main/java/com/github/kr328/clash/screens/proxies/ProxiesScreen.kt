package com.github.kr328.clash.screens.proxies

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.design.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyScreen() {
    val vm: ProxyViewModel = viewModel()
    val state by vm.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshGroups()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profiles)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        floatingActionButton = {
            if (state.running) {
                FloatingActionButton(onClick = { vm.healthCheck() }) {
                    Icon(Icons.Filled.Speed, contentDescription = stringResource(R.string.check_delay))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                if (!state.running) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.service_not_running))
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(state.groups.size) { idx ->
                            val name = state.groups[idx]
                            val selected = name == state.selectedGroup
                            FilterChip(
                                selected = selected,
                                onClick = { vm.loadGroup(name) },
                                label = { Text(name, modifier = Modifier.padding(vertical = 8.dp)) }
                            )
                        }
                    }

                    if (state.proxies.isEmpty() && !state.loading) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.proxies_unavailable))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 8.dp)
                        ) {
                            items(state.proxies.size) { idx ->
                                val p = state.proxies[idx]
                                ProxyItem(
                                    proxy = p,
                                    isCurrent = p.name == state.currentNow,
                                    isSelectable = state.isSelector,
                                    onClick = { vm.selectProxy(p.name) }
                                )
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.loading,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp)
                    .padding(top = 58.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ProxyItem(
    proxy: Proxy,
    isCurrent: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = { if (isSelectable && !isCurrent) onClick() },
        colors = if (isCurrent) CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) else CardDefaults.outlinedCardColors(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = proxy.name,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyLarge
            )
            DelayIndicator(delayMs = proxy.delay)
        }
    }
}

@Composable
private fun DelayIndicator(delayMs: Int) {
    if (delayMs == 0 || delayMs == 65535) return

    val color = when {
        delayMs < 200 -> Color(0xFF4CAF50)
        delayMs < 500 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Text(
        text = "$delayMs",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White,
        modifier = Modifier
            .background(color = color, shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
