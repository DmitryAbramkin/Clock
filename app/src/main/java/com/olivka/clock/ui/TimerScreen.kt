package com.olivka.clock.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.olivka.clock.R
import com.olivka.clock.data.TimerEntity
import com.olivka.clock.service.TimerService
import com.olivka.clock.ui.theme.AutoSizeText
import com.olivka.clock.ui.theme.clampedSp
import com.olivka.clock.ui.theme.errorContainerDark
import com.olivka.clock.ui.theme.errorDark
import com.olivka.clock.ui.theme.onErrorContainerDark
import com.olivka.clock.ui.theme.onPrimaryDark
import com.olivka.clock.ui.theme.onSurfaceDark
import com.olivka.clock.ui.theme.onSurfaceVariantDark
import com.olivka.clock.ui.theme.onTertiaryDark
import com.olivka.clock.ui.theme.outlineVariantDark
import com.olivka.clock.ui.theme.primaryDark
import com.olivka.clock.ui.theme.surfaceContainerHighDark
import com.olivka.clock.ui.theme.tertiaryDark
import com.olivka.clock.utils.TimerUtils
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onAddTimer: () -> Unit = {},
    onOpenTimer: (Int) -> Unit = {}
) {
    val timers by viewModel.allTimers.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val context = LocalContext.current
    val navigationInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                    )
                )
                .padding(horizontal = 16.dp)
                .padding(bottom = navigationInset + 72.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.timers),
                color = onSurfaceDark,
                fontSize = clampedSp(28f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            TimerListTab(
                timers = timers,
                loaded = loaded,
                modifier = Modifier.weight(1f),
                onOpenTimer = onOpenTimer,
                onToggleTimer = { timer ->
                    val action = if (timer.isRunning) {
                        TimerService.ACTION_PAUSE
                    } else {
                        TimerService.ACTION_START
                    }
                    TimerService.start(context, action, timer.id)
                },
                onResetTimer = { timer ->
                    TimerService.start(context, TimerService.ACTION_RESET, timer.id)
                },
                onDeleteTimer = { timer ->
                    TimerService.start(context, TimerService.ACTION_DELETE, timer.id)
                }
            )
        }

        FloatingActionButton(
            onClick = onAddTimer,
            shape = RoundedCornerShape(24.dp),
            containerColor = tertiaryDark,
            contentColor = onTertiaryDark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navigationInset + 72.dp)
                .size(80.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_timer),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerListTab(
    timers: List<TimerEntity>,
    loaded: Boolean,
    modifier: Modifier = Modifier,
    onOpenTimer: (Int) -> Unit,
    onToggleTimer: (TimerEntity) -> Unit,
    onResetTimer: (TimerEntity) -> Unit,
    onDeleteTimer: (TimerEntity) -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasRunning = timers.any { it.isRunning }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(hasRunning, lifecycleOwner) {
        if (!hasRunning) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(500L)
            }
        }
    }

    if (!loaded) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryDark)
        }
        return
    }

    if (timers.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.timer_list_empty),
                    color = onSurfaceDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.timer_list_empty_hint),
                    color = onSurfaceVariantDark,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(timers, key = { it.id }) { timer ->
            var showDeleteConfirm by remember { mutableStateOf(false) }
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart ||
                        value == SwipeToDismissBoxValue.StartToEnd
                    ) {
                        if (prefs.getBoolean("no_swipe_delete_confirm", false)) {
                            onDeleteTimer(timer)
                            true
                        } else {
                            showDeleteConfirm = true
                            false
                        }
                    } else {
                        true
                    }
                }
            )

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(stringResource(R.string.delete_timer), color = onSurfaceDark) },
                    text = {
                        Text(
                            stringResource(R.string.delete_timer_question),
                            color = onSurfaceVariantDark
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onDeleteTimer(timer)
                            showDeleteConfirm = false
                        }) {
                            Text(stringResource(R.string.delete), color = errorDark)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(stringResource(R.string.cancel), color = primaryDark)
                        }
                    },
                    containerColor = surfaceContainerHighDark,
                    titleContentColor = onSurfaceDark,
                    textContentColor = onSurfaceVariantDark
                )
            }

            val remaining = if (timer.isRunning) {
                (timer.endAtMillis - now).coerceAtLeast(0L)
            } else {
                timer.remainingMs
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val alignment = if (dismissState.dismissDirection ==
                        SwipeToDismissBoxValue.StartToEnd
                    ) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(errorContainerDark, RoundedCornerShape(28.dp))
                            .padding(horizontal = 24.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = onErrorContainerDark,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            ) {
                TimerCard(
                    timer = timer,
                    remainingMs = remaining,
                    onClick = { onOpenTimer(timer.id) },
                    onToggle = { onToggleTimer(timer) },
                    onReset = { onResetTimer(timer) }
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: TimerEntity,
    remainingMs: Long,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val progress = TimerUtils.progress(remainingMs, timer.durationMs)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (timer.label.isNotBlank()) {
                        Text(
                            text = timer.label,
                            color = primaryDark,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    AutoSizeText(
                        text = TimerUtils.formatRemaining(remainingMs),
                        maxFontSize = 40.sp,
                        minFontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        color = onSurfaceDark,
                        contentAlignment = Alignment.CenterStart
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                TimerCardActionButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    containerColor = outlineVariantDark,
                    contentColor = onSurfaceVariantDark,
                    onClick = onReset
                )
                Spacer(modifier = Modifier.width(8.dp))
                TimerCardActionButton(
                    icon = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (timer.isRunning) {
                        stringResource(R.string.pause)
                    } else {
                        stringResource(R.string.start)
                    },
                    containerColor = primaryDark,
                    contentColor = onPrimaryDark,
                    onClick = onToggle
                )
            }
            if (timer.isRunning || timer.hasStarted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = primaryDark,
                    trackColor = outlineVariantDark
                )
            }
        }
    }
}

@Composable
private fun TimerCardActionButton(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(containerColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
