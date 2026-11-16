package com.olivka.clock.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.olivka.clock.R
import com.olivka.clock.ui.theme.errorDark
import com.olivka.clock.ui.theme.onSurfaceDark
import com.olivka.clock.ui.theme.onSurfaceVariantDark
import com.olivka.clock.ui.theme.onTertiaryContainerDark
import com.olivka.clock.ui.theme.primaryDark
import com.olivka.clock.ui.theme.surfaceContainerHighDark
import com.olivka.clock.ui.theme.tertiaryContainerDark
import com.olivka.clock.utils.DeviceRestrictions
import com.olivka.clock.utils.SystemVibrationState
import com.olivka.clock.utils.readSystemVibrationState

/**
 * System vibration state that is refreshed when the user returns from the
 * system settings screen.
 */
@Composable
fun rememberSystemVibrationState(): SystemVibrationState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readSystemVibrationState(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = readSystemVibrationState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return state
}

/**
 * Warning card shown when system settings block vibration. The touch-only
 * warning is optional because alarm and timer vibration are not gated by the
 * touch feedback setting.
 */
@Composable
fun SystemVibrationWarning(
    state: SystemVibrationState,
    modifier: Modifier = Modifier,
    showTouchWarning: Boolean = true
) {
    if (state.masterEnabled && (!showTouchWarning || state.touchEnabled)) return
    val context = LocalContext.current
    val masterOff = !state.masterEnabled
    val title = stringResource(
        if (masterOff) {
            R.string.system_vibration_off_title
        } else {
            R.string.system_touch_vibration_off_title
        }
    )
    val body = stringResource(
        if (masterOff) {
            R.string.system_vibration_off_body
        } else {
            R.string.system_touch_vibration_off_body
        }
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = errorDark
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = onSurfaceDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = body,
                        color = onSurfaceVariantDark,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { DeviceRestrictions.openVibrationSettings(context) }
            ) {
                Text(
                    text = stringResource(R.string.system_vibration_open_settings),
                    color = primaryDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Optional onboarding banner for the permissions step. It does not block the
 * flow and is not an error: it simply explains what cannot vibrate while the
 * system master switch is off.
 */
@Composable
fun SystemVibrationOnboardingBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tertiaryContainerDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Vibration,
                    contentDescription = null,
                    tint = onTertiaryContainerDark
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_vibration_off_title),
                        color = onTertiaryContainerDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_vibration_off_body),
                        color = onTertiaryContainerDark.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = { DeviceRestrictions.openVibrationSettings(context) }
            ) {
                Text(
                    text = stringResource(R.string.system_vibration_open_settings),
                    color = onTertiaryContainerDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
