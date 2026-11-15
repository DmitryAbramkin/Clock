package com.olivka.clock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun WakeUpScreen(
    snoozeMinutes: Int = 5,
    showSnoozeButton: Boolean = true,
    label: String = "",
    onStopAlarm: () -> Unit = {},
    onSnoozeAlarm: () -> Unit = {}
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val locale = appLocale()
    val timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dayStr = now.format(
        DateTimeFormatter.ofPattern("EEEE, MMM d", locale)
    )

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Horizontal +
                WindowInsetsSides.Top +
                WindowInsetsSides.Bottom
        ),
        containerColor = backgroundDark
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val compact = maxHeight < 480.dp
            val buttonHeight = if (compact) 56.dp else 64.dp
            val outerSpacing = if (compact) 16.dp else 32.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (compact) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (compact) 16.dp else 64.dp))

                AutoSizeText(
                    text = timeStr,
                    maxFontSize = if (compact) 72.sp else 96.sp,
                    minFontSize = 36.sp,
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryDark,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    dayStr,
                    color = onSurfaceVariantDark,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                if (label.isNotBlank()) {
                    Spacer(modifier = Modifier.height(outerSpacing))
                    Text(
                        text = label,
                        color = onSurfaceDark,
                        fontSize = clampedSp(30f, 1.5f),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!compact) {
                    Spacer(modifier = Modifier.height(64.dp))
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Button(
                    onClick = onStopAlarm,
                    modifier = Modifier.fillMaxWidth().height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryDark,
                        contentColor = onPrimaryDark
                    ),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.dismiss),
                        fontSize = clampedSp(18f, 1.5f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(outerSpacing))

                if (showSnoozeButton) {
                    Button(
                        onClick = onSnoozeAlarm,
                        modifier = Modifier.fillMaxWidth().height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = surfaceContainerHighDark,
                            contentColor = onSurfaceDark
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Icon(Icons.Outlined.Snooze, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.snooze_minutes, snoozeMinutes),
                            fontSize = clampedSp(18f, 1.5f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxWidth().height(buttonHeight))
                }

                Spacer(modifier = Modifier.height(outerSpacing))
            }
        }
    }
}
