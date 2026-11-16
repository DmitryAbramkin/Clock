package com.olivka.clock.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmPreferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)

    var autoDelete by remember { mutableStateOf(prefs.getBoolean("auto_delete_one_time", false)) }
    var noSwipeConfirm by remember {
        mutableStateOf(prefs.getBoolean("no_swipe_delete_confirm", false))
    }
    var snoozeMinutes by remember { mutableIntStateOf(prefs.getInt("snooze_minutes", 5)) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showSnoozeButton by remember {
        mutableStateOf(prefs.getBoolean("show_snooze_button", true))
    }

    ClockPage(
        title = stringResource(R.string.alarm_preferences),
        onBack = onBack
    ) {
        SettingsSectionTitle(stringResource(R.string.section_snooze))
        SettingsRowSwitch(
            icon = Icons.Outlined.Snooze,
            title = stringResource(R.string.show_snooze_button),
            subtitle = stringResource(R.string.show_snooze_button_hint),
            checked = showSnoozeButton,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onCheckedChange = {
                showSnoozeButton = it
                prefs.edit().putBoolean("show_snooze_button", it).apply()
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        SettingsRow(
            icon = Icons.Outlined.Snooze,
            title = stringResource(R.string.snooze_duration),
            subtitle = androidx.compose.ui.res.pluralStringResource(
                R.plurals.snooze_duration_value,
                snoozeMinutes,
                snoozeMinutes
            ),
            showArrow = true,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onClick = { showSnoozeDialog = true }
        )

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        SettingsSectionTitle(stringResource(R.string.section_after_ringing))
        SettingsRowSwitch(
            icon = Icons.Outlined.DeleteSweep,
            title = stringResource(R.string.auto_delete_one_time),
            subtitle = stringResource(R.string.auto_delete_one_time_hint),
            checked = autoDelete,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onCheckedChange = {
                autoDelete = it
                prefs.edit().putBoolean("auto_delete_one_time", it).apply()
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        SettingsRowSwitch(
            icon = Icons.Outlined.Delete,
            title = stringResource(R.string.no_swipe_delete),
            subtitle = stringResource(R.string.no_swipe_delete_hint),
            checked = noSwipeConfirm,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onCheckedChange = {
                noSwipeConfirm = it
                prefs.edit().putBoolean("no_swipe_delete_confirm", it).apply()
            }
        )
    }

    if (showSnoozeDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text(stringResource(R.string.snooze_duration), color = onSurfaceDark) },
            text = {
                Column {
                    Text(
                        androidx.compose.ui.res.pluralStringResource(
                            R.plurals.snooze_duration_value,
                            snoozeMinutes,
                            snoozeMinutes
                        ),
                        color = primaryDark,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = snoozeMinutes.toFloat(),
                        onValueChange = { snoozeMinutes = it.roundToInt() },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                    Text(
                        stringResource(R.string.snooze_duration_hint),
                        color = onSurfaceVariantDark,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putInt("snooze_minutes", snoozeMinutes).apply()
                    showSnoozeDialog = false
                }) {
                    Text(stringResource(R.string.ok), color = primaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnoozeDialog = false }) {
                    Text(stringResource(R.string.cancel), color = onSurfaceVariantDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }
}
