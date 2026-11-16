package com.olivka.clock.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.WheelTimePicker
import com.olivka.clock.ui.theme.adaptiveDp
import com.olivka.clock.ui.theme.backgroundDark
import com.olivka.clock.ui.theme.clampedSp
import com.olivka.clock.ui.theme.errorDark
import com.olivka.clock.ui.theme.onPrimaryDark
import com.olivka.clock.ui.theme.onSurfaceDark
import com.olivka.clock.ui.theme.onSurfaceVariantDark
import com.olivka.clock.ui.theme.primaryDark
import com.olivka.clock.ui.theme.surfaceContainerHighDark
import com.olivka.clock.utils.WheelHaptics

/**
 * Second onboarding step: a live time-wheel demo so the user can feel the
 * feedback before starting to use the app. Devices without a haptic actuator
 * also get an explicit warning and the same wheel-vibration switch that is
 * available later in Customisation.
 */
@Composable
fun OnboardingTimeScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)
    }
    val hapticSupported = remember { WheelHaptics.hasHapticFeedback(context) }
    var wheelHapticsEnabled by remember { mutableStateOf(WheelHaptics.isEnabled(prefs)) }
    var hour by remember { mutableIntStateOf(10) }
    var minute by remember { mutableIntStateOf(10) }
    var second by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Horizontal +
                WindowInsetsSides.Top +
                WindowInsetsSides.Bottom
        ),
        containerColor = backgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(adaptiveDp(16.dp)))
            Text(
                text = stringResource(R.string.onboarding_time_title),
                fontSize = clampedSp(32f),
                fontWeight = FontWeight.Bold,
                color = onSurfaceDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(adaptiveDp(12.dp)))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
            ) {
                WheelTimePicker(
                    hour = hour,
                    minute = minute,
                    second = second,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it },
                    onSecondChange = { second = it },
                    showSeconds = true,
                    itemFontSize = 36.sp,
                    hapticsEnabled = wheelHapticsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_time_keyboard_hint),
                color = onSurfaceVariantDark,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            if (!hapticSupported) {
                Spacer(modifier = Modifier.height(adaptiveDp(12.dp)))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = errorDark
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.onboarding_time_haptics_warning_title
                                ),
                                color = onSurfaceDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.onboarding_time_haptics_warning_body
                                ),
                                color = onSurfaceVariantDark,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRowSwitch(
                    icon = Icons.Outlined.Vibration,
                    title = stringResource(R.string.wheel_vibration),
                    subtitle = stringResource(R.string.wheel_haptics_hint),
                    checked = wheelHapticsEnabled,
                    onCheckedChange = {
                        wheelHapticsEnabled = it
                        prefs.edit().putBoolean(WheelHaptics.PREF_ENABLED, it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adaptiveDp(56.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryDark,
                    contentColor = onPrimaryDark
                )
            ) {
                Text(
                    text = stringResource(R.string.continue_action),
                    fontSize = clampedSp(18f, 1.5f),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(adaptiveDp(8.dp)))
        }
    }
}
