package com.olivka.clock.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.service.StopwatchManager
import com.olivka.clock.service.StopwatchService
import com.olivka.clock.ui.theme.AutoSizeText
import com.olivka.clock.ui.theme.KeepScreenOnChip
import com.olivka.clock.ui.theme.PrimaryActionButton
import com.olivka.clock.ui.theme.RoundControlButton
import com.olivka.clock.ui.theme.adaptiveDp
import com.olivka.clock.ui.theme.clampedSp
import com.olivka.clock.ui.theme.dialSize
import com.olivka.clock.ui.theme.onSurfaceDark

@Composable
fun StopwatchScreen(
    keepScreenOn: Boolean = false,
    onKeepScreenOnChange: (Boolean) -> Unit = {}
) {
    val navigationInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
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
            text = stringResource(R.string.stopwatch),
            color = onSurfaceDark,
            fontSize = clampedSp(28f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            KeepScreenOnChip(
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            StopwatchTabContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchTabContent() {
    val context = LocalContext.current
    val elapsedMillis by StopwatchManager.elapsedMillis.collectAsState()
    val isRunning by StopwatchManager.isRunning.collectAsState()
    val laps by StopwatchManager.laps.collectAsState()

    val minutes = (elapsedMillis / 1000 / 60)
    val seconds = (elapsedMillis / 1000) % 60
    val centiseconds = (elapsedMillis % 1000) / 10

    val topText = if (minutes > 0) {
        String.format("%02d:%02d", minutes, seconds)
    } else {
        String.format("%02d", seconds)
    }
    val bottomText = String.format("%02d", centiseconds)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val circleSize = dialSize(maxWidth, maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = adaptiveDp(24.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.size(circleSize),
                contentAlignment = Alignment.Center
            ) {
                val outlineColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = outlineColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AutoSizeText(
                        text = topText,
                        maxFontSize = 80.sp,
                        minFontSize = 28.sp,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    AutoSizeText(
                        text = bottomText,
                        maxFontSize = 48.sp,
                        minFontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(0.45f),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (laps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    laps.forEachIndexed { index, lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.lap_number, laps.size - index),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = formatStopwatchTime(lap),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundControlButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    onClick = {
                        val intent = Intent(context, StopwatchService::class.java)
                        intent.action = StopwatchService.ACTION_RESET
                        context.startService(intent)
                    }
                )

                PrimaryActionButton(
                    active = isRunning,
                    icon = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = when {
                        isRunning -> stringResource(R.string.pause)
                        elapsedMillis > 0 -> stringResource(R.string.resume)
                        else -> stringResource(R.string.start)
                    },
                    onClick = {
                        val intent = Intent(context, StopwatchService::class.java)
                        intent.action = if (isRunning) {
                            StopwatchService.ACTION_PAUSE
                        } else {
                            StopwatchService.ACTION_START
                        }
                        if (!isRunning && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                )

                RoundControlButton(
                    icon = Icons.Outlined.Timer,
                    contentDescription = stringResource(R.string.lap),
                    onClick = {
                        val intent = Intent(context, StopwatchService::class.java)
                        intent.action = StopwatchService.ACTION_LAP
                        context.startService(intent)
                    }
                )
            }
        }
    }
}

private fun formatStopwatchTime(millis: Long): String {
    val minutes = millis / 60000
    val seconds = (millis / 1000) % 60
    val centiseconds = (millis % 1000) / 10
    return if (minutes > 0) {
        String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    } else {
        String.format("%02d.%02d", seconds, centiseconds)
    }
}
