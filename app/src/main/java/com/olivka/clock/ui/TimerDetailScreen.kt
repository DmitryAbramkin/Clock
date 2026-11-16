package com.olivka.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.olivka.clock.R
import com.olivka.clock.data.TimerEntity
import com.olivka.clock.service.TimerService
import com.olivka.clock.ui.theme.AutoSizeText
import com.olivka.clock.ui.theme.ClockScaffold
import com.olivka.clock.ui.theme.KeepScreenOnChip
import com.olivka.clock.ui.theme.PrimaryActionButton
import com.olivka.clock.ui.theme.RoundControlButton
import com.olivka.clock.ui.theme.RoundTextControlButton
import com.olivka.clock.ui.theme.adaptiveDp
import com.olivka.clock.ui.theme.dialSize
import com.olivka.clock.ui.theme.onSurfaceDark
import com.olivka.clock.ui.theme.onSurfaceVariantDark
import com.olivka.clock.ui.theme.primaryDark
import com.olivka.clock.utils.TimerUtils
import kotlinx.coroutines.delay

@Composable
fun TimerDetailScreen(
    timerId: Int,
    viewModel: TimerViewModel,
    keepScreenOn: Boolean = false,
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val timers by viewModel.allTimers.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val timer = timers.firstOrNull { it.id == timerId }
    val title = when {
        timer == null -> stringResource(R.string.timer)
        timer.label.isNotBlank() -> timer.label
        else -> stringResource(
            R.string.timer_on_duration,
            TimerUtils.formatDuration(
                timer.durationMs,
                stringResource(R.string.duration_hours),
                stringResource(R.string.duration_minutes),
                stringResource(R.string.duration_seconds)
            )
        )
    }
    val context = LocalContext.current

    ClockScaffold(title = title, onBack = onBack) { innerPadding ->
        if (timer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!loaded) {
                    CircularProgressIndicator(color = primaryDark)
                } else {
                    Text(
                        text = stringResource(R.string.timer_not_found),
                        color = onSurfaceVariantDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            TimerDetailContent(
                timer = timer,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onReset = {
                    TimerService.start(context, TimerService.ACTION_RESET, timer.id)
                },
                onToggle = {
                    val action = if (timer.isRunning) {
                        TimerService.ACTION_PAUSE
                    } else {
                        TimerService.ACTION_START
                    }
                    TimerService.start(context, action, timer.id)
                },
                onAddMinute = {
                    TimerService.start(context, TimerService.ACTION_ADD_MINUTE, timer.id)
                }
            )
        }
    }
}

@Composable
private fun TimerDetailContent(
    timer: TimerEntity,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit
) {
    var now by remember(timer.id) { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(timer.isRunning, timer.endAtMillis, lifecycleOwner) {
        if (!timer.isRunning) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(500L)
            }
        }
    }
    val remaining = if (timer.isRunning) {
        (timer.endAtMillis - now).coerceAtLeast(0L)
    } else {
        timer.remainingMs
    }
    val progress = TimerUtils.progress(remaining, timer.durationMs)
    val timeText = TimerUtils.formatRemaining(remaining)
    val isAtStart = remaining >= timer.durationMs

    BoxWithConstraints(modifier = modifier) {
        val ringSize = dialSize(maxWidth, maxHeight)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                KeepScreenOnChip(
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                val progressColor = primaryDark
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val radius = size.minDimension / 2f - stroke / 2f
                    drawCircle(
                        color = trackColor,
                        radius = radius,
                        style = Stroke(width = stroke)
                    )
                    if (progress > 0f) {
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                AutoSizeText(
                    text = timeText,
                    maxFontSize = 72.sp,
                    minFontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(0.72f),
                    color = onSurfaceDark,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (TimerUtils.isReset(timer)) {
                    // Keep the middle button centred when there is nothing
                    // to reset.
                    Spacer(modifier = Modifier.size(56.dp))
                } else {
                    RoundControlButton(
                        icon = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.reset),
                        onClick = onReset
                    )
                }

                PrimaryActionButton(
                    active = timer.isRunning,
                    icon = if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = when {
                        timer.isRunning -> stringResource(R.string.pause)
                        isAtStart -> stringResource(R.string.start)
                        else -> stringResource(R.string.resume)
                    },
                    onClick = onToggle
                )

                RoundTextControlButton(
                    text = "+1",
                    contentDescription = stringResource(R.string.add_minute),
                    onClick = onAddMinute
                )
            }

            Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
        }
    }
}
