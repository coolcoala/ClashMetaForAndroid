package com.github.kr328.clash.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.screens.profiles.new_profile.NewProfileActivity
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import com.github.kr328.clash.design.R as DesignR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMotionApi::class)
@Composable
fun HomeScreen(vm: MainComposeViewModel) {
    val state by vm.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vm.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    var groupMenuExpanded by remember { mutableStateOf(false) }
    var proxyMenuExpanded by remember { mutableStateOf(false) }

    var selectorWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val ctx = LocalContext.current


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = DesignR.drawable.map),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )

        when {
            state.isLoading && state.profiles.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.profiles.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyProfilePrompt(
                        onAddProfileClick = {
                            ctx.startActivity(NewProfileActivity::class.intent)
                        }
                    )
                }
            }
            else -> {
                val collapsedScene = ConstraintSet {
                    val powerButtonBlock = createRefFor("powerButtonBlock")
                    val selectorsBlock = createRefFor("selectorsBlock")
                    val subscriptionInfoCard = createRefFor("subscriptionInfoCard")

                    constrain(subscriptionInfoCard) {
                        top.linkTo(parent.top, margin = 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        alpha = 0f
                    }

                    constrain(powerButtonBlock) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    constrain(selectorsBlock) {
                        top.linkTo(powerButtonBlock.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        alpha = 0f
                    }
                }

                val expandedScene = ConstraintSet {
                    val powerButtonBlock = createRefFor("powerButtonBlock")
                    val selectorsBlock = createRefFor("selectorsBlock")
                    val subscriptionInfoCard = createRefFor("subscriptionInfoCard")

                    constrain(subscriptionInfoCard) {
                        top.linkTo(parent.top, margin = 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        alpha = 1f
                    }

                    constrain(powerButtonBlock) {
                        top.linkTo(subscriptionInfoCard.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }

                    constrain(selectorsBlock) {
                        top.linkTo(powerButtonBlock.bottom, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        alpha = 1f
                    }
                }

                val progress by animateFloatAsState(
                    targetValue = if (state.running && !state.isConnecting) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )

                MotionLayout(
                    start = collapsedScene,
                    end = expandedScene,
                    progress = progress,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val hasSubscriptionInfo = state.activeProfileTotal > 0 || state.activeProfileExpire > 0

                    // Создаем Row для двух карточек
                    Row(
                        modifier = Modifier
                            .layoutId("subscriptionInfoCard") // Используем тот же ID для анимации
                            .alpha(if (hasSubscriptionInfo || progress > 0f) 1f else 0f)
                            .height(IntrinsicSize.Min), // Делает обе карточки одинаковой высоты
                        horizontalArrangement = Arrangement.spacedBy(16.dp) // Расстояние между карточками
                    ) {
                        TrafficInfoSubCard(
                            modifier = Modifier.weight(1f), // Занимает половину места
                            download = state.activeProfileDownload,
                            upload = state.activeProfileUpload,
                            total = state.activeProfileTotal,
                        )
                        ExpirationInfoSubCard(
                            modifier = Modifier.weight(1f), // Занимает вторую половину
                            expire = state.activeProfileExpire
                        )
                    }

                    Column(
                        modifier = Modifier.layoutId("powerButtonBlock"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val connectedColor = Color(0xFF19C37D)
                        val disconnectedColor = MaterialTheme.colorScheme.onSurfaceVariant

                        val statusText = when {
                            state.isConnecting -> stringResource(DesignR.string.connecting)
                            state.running -> stringResource(DesignR.string.connected)
                            else -> stringResource(DesignR.string.disconnected)
                        }
                        val animatedStatusColor by animateColorAsState(
                            targetValue = when {
                                state.isConnecting -> MaterialTheme.colorScheme.primary
                                state.running -> connectedColor
                                else -> disconnectedColor
                            },
                            label = "statusColorAnimation"
                        )
                        Text(
                            text = statusText,
                            color = animatedStatusColor,
                            fontSize = 32.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center) {
                            val animatedGlowColor by animateColorAsState(
                                targetValue = if (state.running) animatedStatusColor.copy(alpha = 0.5f) else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "glowColorAnimation"
                            )
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .drawBehind {
                                        if (animatedGlowColor.alpha > 0f) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    0.2f to animatedGlowColor,
                                                    1.0f to Color.Transparent
                                                )
                                            )
                                        }
                                    },
                            )
                            PowerButton(
                                isRunning = state.running,
                                isConnecting = state.isConnecting,
                                onClick = { vm.toggleService { intent -> vpnLauncher.launch(intent) } }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.layoutId("selectorsBlock"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.onSizeChanged { selectorWidth = it.width }) {
                            SelectorCard(
                                title = stringResource(DesignR.string.group),
                                content = {
                                    Text(
                                        text = state.groupName ?: "—",
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = { vm.refreshAll(); groupMenuExpanded = true }
                            )
                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(
                                    extraSmall = RoundedCornerShape(
                                        12.dp
                                    )
                                )
                            ) {
                                DropdownMenu(
                                    expanded = groupMenuExpanded,
                                    onDismissRequest = { groupMenuExpanded = false },
                                    modifier = Modifier.width(with(density) { selectorWidth.toDp() }),
                                    offset = DpOffset(0.dp, 0.dp)
                                ) {
                                    state.groupNames.forEach { name ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                vm.selectGroup(name)
                                                groupMenuExpanded = false
                                            },
                                            trailingIcon = if (name == state.groupName) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = stringResource(DesignR.string.selected),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.onSizeChanged { selectorWidth = it.width }) {
                            val selectedProxy =
                                remember(state.proxyName, state.proxiesInSelectedGroup) {
                                    state.proxiesInSelectedGroup.find { it.name == state.proxyName }
                                }

                            SelectorCard(
                                title = stringResource(DesignR.string.proxy),
                                onClick = {
                                    if (!proxyMenuExpanded) {
                                        vm.healthCheckForSelectedGroup()
                                    }
                                    proxyMenuExpanded = true
                                },
                                content = {
                                    PingBadge(
                                        delay = selectedProxy?.delay ?: 0,
                                        isTesting = state.isTestingPings,
                                        onClick = { vm.healthCheckForSelectedGroup() },
                                        enabled = true
                                    )
                                    Text(
                                        text = state.proxyName ?: "—",
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(
                                    extraSmall = RoundedCornerShape(
                                        12.dp
                                    )
                                )
                            ) {
                                DropdownMenu(
                                    expanded = proxyMenuExpanded,
                                    onDismissRequest = { proxyMenuExpanded = false },
                                    modifier = Modifier.width(with(density) { selectorWidth.toDp() }),
                                    offset = DpOffset(0.dp, 0.dp)
                                ) {
                                    state.proxiesInSelectedGroup.forEach { proxy ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    PingBadge(
                                                        delay = proxy.delay,
                                                        isTesting = state.isTestingPings,
                                                        onClick = { },
                                                        enabled = false
                                                    )
                                                    Text(
                                                        text = proxy.name,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            onClick = {
                                                vm.selectProxy(proxy.name)
                                                proxyMenuExpanded = false
                                            },
                                            trailingIcon = if (proxy.name == state.proxyName) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = stringResource(DesignR.string.selected),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun TrafficInfoSubCard(
    download: Long,
    upload: Long,
    total: Long,
    modifier: Modifier = Modifier
) {
    val used = (download + upload).coerceAtLeast(0)
    OutlinedCard(
        modifier = modifier.height(80.dp) // <-- Ваш размер 80.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Заголовок вверху
            Text(
                text = stringResource(id = DesignR.string.traffic),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Spacer занимает все свободное место, прижимая контент ниже к низу
            Spacer(modifier = Modifier.weight(1f))

            // Значение
            Text(
                text = formatTrafficUsage(used, total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Прогресс-бар появляется только при необходимости, не занимая место, когда его нет
            if (total > 0) {
                Spacer(Modifier.height(8.dp)) // Небольшой отступ над прогресс-баром
                val progress = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ExpirationInfoSubCard(
    expire: Long,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.height(80.dp) // <-- Ваш размер 80.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Заголовок вверху
            Text(
                text = stringResource(id = DesignR.string.expiration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Spacer занимает все свободное место
            Spacer(modifier = Modifier.weight(1f))

            // Значение
            Text(
                text = formatExpiration(expire),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Прогресс-бар появляется только при необходимости
            if (expire > 0) {
                Spacer(Modifier.height(8.dp))
                val diffMillis = expire - System.currentTimeMillis()
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis)
                val progress = (daysRemaining.toFloat() / 30f).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}


@Composable
private fun PingBadge(
    delay: Int,
    isTesting: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val (text, color) = when {
        0 <= delay && delay == 65535 -> "---" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        delay < 200 -> "$delay" to Color(0xFF19C37D)
        delay < 500 -> "$delay" to Color(0xFFFFA726)
        else -> "$delay" to MaterialTheme.colorScheme.error
    }

    val finalOnClick = if (enabled) onClick else null

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .then(if (finalOnClick != null) Modifier.clickable(enabled = !isTesting, onClick = finalOnClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isTesting && delay <= 0) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 1.5.dp
            )
        } else {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SelectorCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(DesignR.string.select)
                )
            }
        }
    }
}

@Composable
private fun PowerButton(
    isRunning: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    val connectedColor = Color(0xFF19C37D)
    val disconnectedColor = lerp(
        start = MaterialTheme.colorScheme.outlineVariant,
        stop = MaterialTheme.colorScheme.outline,
        fraction = 0.25f
    )
    val connectingColor = MaterialTheme.colorScheme.primary

    val animatedStatusColor by animateColorAsState(
        targetValue = when {
            isConnecting -> connectingColor
            isRunning -> connectedColor
            else -> disconnectedColor
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColorAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scaleAnimation")

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val highlightColor = if (isDark) {
        Color.White.copy(alpha = 0.5f)
    } else {
        lerp(start = animatedStatusColor, stop = Color.White, fraction = 0.4f)
    }

    val haptics = LocalHapticFeedback.current

    Surface(
        onClick = {
            if (!isConnecting) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = Modifier
            .size(160.dp)
            .scale(scale),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            highlightColor,
                            animatedStatusColor
                        ),
                        radius = 180f,
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = DesignR.drawable.power_button),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            translationY = 2f
                        }
                )
                Icon(
                    painter = painterResource(id = DesignR.drawable.power_button),
                    contentDescription = stringResource(DesignR.string.power_button),
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}


@Composable
private fun TrafficInfoItem(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrafficCard(
    totalDownload: String,
    totalUpload: String,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrafficInfoItem(
                icon = ImageVector.vectorResource(id = DesignR.drawable.ic_download),
                iconTint = Color(0xFF19C37D),
                value = totalDownload,
                label = stringResource(DesignR.string.download),
                modifier = Modifier.weight(1f)
            )

            TrafficInfoItem(
                icon = ImageVector.vectorResource(id = DesignR.drawable.ic_upload),
                iconTint = Color(0xFF42A5F5),
                value = totalUpload,
                label = stringResource(DesignR.string.upload),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmptyProfilePrompt(onAddProfileClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(DesignR.string.no_profiles),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(DesignR.string.add_profile_to_start),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddProfileClick) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(DesignR.string.create_profile))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%d KB".format((bytes / 1024.0).roundToLong())
    bytes < 1024 * 1024 * 1024 -> "%d MB".format((bytes / (1024.0 * 1024)).roundToLong())
    else -> "%d GB".format((bytes / (1024.0 * 1024 * 1024)).roundToLong())
}
private fun formatTrafficUsage(used: Long, total: Long): String {
    // Если нет информации о общем трафике, просто форматируем использованный
    if (total <= 0) {
        return formatBytes(used)
    }

    // Определяем единицу измерения на основе общего объема (total)
    val (divisor, unit) = when {
        // Для гигабайт и больше
        total >= 1024L * 1024 * 1024 -> Pair(1024.0 * 1024 * 1024, "GB")
        // Для мегабайт
        total >= 1024L * 1024 -> Pair(1024.0 * 1024, "MB")
        // Для килобайт
        total >= 1024L -> Pair(1024.0, "KB")
        // Для байт
        else -> Pair(1.0, "B")
    }

    val usedInUnit = used / divisor
    val totalInUnit = total / divisor

    // Если единица - байты, показываем целые числа
    return if (unit == "B") {
        "${used.toLong()} / ${total.toLong()} B"
    } else {
        // Для остальных - с одним знаком после запятой
        "%d / %d %s".format(usedInUnit.roundToLong(), totalInUnit.roundToLong(), unit)
    }
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