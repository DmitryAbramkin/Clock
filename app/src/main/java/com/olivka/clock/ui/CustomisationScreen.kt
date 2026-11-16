package com.olivka.clock.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*
import com.olivka.clock.utils.WheelHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomisationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)

    var useKeyboardTimeInput by remember {
        mutableStateOf(prefs.getBoolean("use_keyboard_time_input", false))
    }
    var wheelHapticsEnabled by remember {
        mutableStateOf(WheelHaptics.isEnabled(prefs))
    }
    val hapticFeedbackSupported = remember {
        WheelHaptics.hasHapticFeedback(context)
    }
    var predictiveBack by remember {
        mutableStateOf(prefs.getBoolean("predictive_back_enabled", true))
    }

    ClockPage(
        title = stringResource(R.string.customisation),
        onBack = onBack
    ) {
        SettingsSectionTitle(stringResource(R.string.section_time_input))
        SettingsRowSwitch(
            icon = Icons.Outlined.Keyboard,
            title = stringResource(R.string.keyboard_time_input),
            subtitle = stringResource(R.string.keyboard_time_input_hint),
            checked = useKeyboardTimeInput,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onCheckedChange = {
                useKeyboardTimeInput = it
                prefs.edit().putBoolean("use_keyboard_time_input", it).apply()
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        SettingsRowSwitch(
            icon = Icons.Outlined.Vibration,
            title = stringResource(R.string.wheel_haptics),
            subtitle = stringResource(R.string.wheel_haptics_hint),
            checked = wheelHapticsEnabled,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onCheckedChange = {
                wheelHapticsEnabled = it
                prefs.edit().putBoolean(WheelHaptics.PREF_ENABLED, it).apply()
            }
        )
        if (!hapticFeedbackSupported) {
            Text(
                text = stringResource(R.string.wheel_haptics_fallback_hint),
                color = onSurfaceVariantDark,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        SettingsSectionTitle(stringResource(R.string.section_navigation))
        SettingsRowSwitch(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            title = stringResource(R.string.predictive_back),
            subtitle = stringResource(R.string.predictive_back_hint),
            checked = predictiveBack,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onCheckedChange = {
                predictiveBack = it
                prefs.edit().putBoolean("predictive_back_enabled", it).apply()
            }
        )
    }
}
