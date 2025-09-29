import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import com.github.kr328.clash.screens.profiles.new_profile.NewProfileActivity
import com.github.kr328.clash.screens.profiles.ProfilesViewModel
import com.github.kr328.clash.screens.profiles.properties.PropertiesActivity
import com.github.kr328.clash.screens.profiles.UpdateStatus
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import androidx.core.net.toUri
import com.github.kr328.clash.service.model.Profile
import java.util.concurrent.TimeUnit
import androidx.compose.ui.res.stringResource
import com.github.kr328.clash.design.R as DesignR

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen() {
    val vm: ProfilesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.state.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.loading,
        onRefresh = { vm.updateAllProfiles() }
    )

    var contextMenuProfile by rememberSaveable { mutableStateOf<Profile?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(DesignR.string.profiles)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { ctx.startActivity(NewProfileActivity::class.intent) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(DesignR.string.create_profile))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                state.isLoading && state.profiles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.profiles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(DesignR.string.no_profiles) + "\n" + stringResource(DesignR.string.press_plus_to_add_profile),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(state.profiles.size) { idx ->
                            val p = state.profiles[idx]
                            ProfileCard(
                                profile = p,
                                onActivate = { vm.setActive(p) },
                                onOpenMenu = { contextMenuProfile = p },
                                updateStatus = state.updateStatus[p.uuid]
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (contextMenuProfile != null) {
        ProfileContextMenu(
            profile = contextMenuProfile!!,
            onDismiss = { contextMenuProfile = null },
            onUpdate = { vm.update(contextMenuProfile!!) },
            onEdit = { ctx.startActivity(PropertiesActivity::class.intent.setUUID(contextMenuProfile!!.uuid)) },
            onDelete = { vm.delete(contextMenuProfile!!) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: Profile,
    onActivate: () -> Unit,
    onOpenMenu: () -> Unit,
    updateStatus: UpdateStatus?
) {
    ElevatedCard(
        onClick = onActivate,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (profile.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                AnimatedContent(
                    targetState = updateStatus,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "statusIconAnimation"
                ) { status ->
                    if (status != null) {
                        Icon(
                            imageVector = if (status == UpdateStatus.SUCCESS) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = stringResource(DesignR.string.update_status),
                            tint = if (status == UpdateStatus.SUCCESS) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(DesignR.string.menu)
                    )
                }
            }

            val subtitle = if (profile.type == Profile.Type.Url) {
                profile.profileTitle?.takeIf { it.isNotBlank() }
                    ?: extractDomain(profile.source)
                    ?: stringResource(DesignR.string.url)
            } else {
                typeLabel(profile.type)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (profile.pending || !profile.imported) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profile.pending) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(DesignR.string.pending)) })
                    }
                    if (!profile.imported) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(DesignR.string.unsaved)) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val used = (profile.upload + profile.download).coerceAtLeast(0)
            val total = profile.total
            if (total > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DataUsage,
                        contentDescription = stringResource(DesignR.string.traffic),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            DesignR.string.format_used_of_total,
                            formatBytes(used),
                            formatBytes(total)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DataUsage,
                        contentDescription = stringResource(DesignR.string.traffic),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(DesignR.string.format_used_only, formatBytes(used)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Update,
                    contentDescription = stringResource(DesignR.string.updated),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(profile.updatedAt),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    imageVector = if (profile.expire > 0 && (profile.expire - System.currentTimeMillis()) < 0) Icons.Default.EventBusy else Icons.Default.Schedule,
                    contentDescription = stringResource(DesignR.string.expiration),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatExpiration(profile.expire),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContextMenu(
    profile: Profile,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ListItem(
            headlineContent = { Text(profile.name, style = MaterialTheme.typography.titleLarge) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        if (profile.type != Profile.Type.File) {
            ListItem(
                headlineContent = { Text(stringResource(DesignR.string.update)) },
                leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                modifier = Modifier.clickable { onUpdate(); onDismiss() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(DesignR.string.edit)) },
            leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
            modifier = Modifier.clickable { onEdit(); onDismiss() },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        ListItem(
            headlineContent = { Text(stringResource(DesignR.string.delete)) },
            leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
            modifier = Modifier.clickable { onDelete(); onDismiss() },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun typeLabel(t: Profile.Type): String = when (t) {
    Profile.Type.File -> stringResource(DesignR.string.file)
    Profile.Type.Url -> stringResource(DesignR.string.url)
    Profile.Type.External -> stringResource(DesignR.string.external)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "${bytes} B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
}

private fun extractDomain(url: String): String? {
    return try {
        url.toUri().host
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return stringResource(DesignR.string.never)
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

@Composable
private fun formatExpiration(expire: Long): String {
    if (expire == 0L) {
        return "∞"
    }
    val diff = expire - System.currentTimeMillis()
    return if (diff > 0) {
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days > 0) stringResource(DesignR.string.format_days_abbr, days.toInt()) else stringResource(DesignR.string.less_than_one_day)
    } else {
        stringResource(DesignR.string.expired)
    }
}