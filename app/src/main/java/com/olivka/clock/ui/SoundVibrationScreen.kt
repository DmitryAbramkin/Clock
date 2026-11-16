package com.olivka.clock.ui

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.ClockPage
import com.olivka.clock.ui.theme.SettingsSectionTitle
import com.olivka.clock.ui.theme.adaptiveDp
import com.olivka.clock.ui.theme.onSurfaceVariantDark
import com.olivka.clock.utils.DeviceRestrictions
import com.olivka.clock.utils.RingtoneNames
import com.olivka.clock.utils.VibrationUtils

@Composable
fun SoundVibrationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("strict_clock_prefs", android.content.Context.MODE_PRIVATE)

    val defaultRingtoneLabel = stringResource(R.string.default_ringtone)
    val selectedSoundLabel = stringResource(R.string.selected_sound)
    val silentSoundLabel = stringResource(R.string.sound_silent)

    fun soundName(uriStr: String?): String = when {
        uriStr == "Silent" -> silentSoundLabel
        uriStr.isNullOrEmpty() -> RingtoneNames
            .defaultAlarmDisplayName(context) ?: defaultRingtoneLabel
        else -> RingtoneNames.displayName(context, uriStr)
            ?: selectedSoundLabel
    }

    fun pickedSoundValue(uri: Uri?): String {
        if (uri == null) return "Silent"
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        return if (uri == defaultUri) "" else uri.toString()
    }

    var defaultRingtoneName by remember {
        mutableStateOf(soundName(prefs.getString("default_alarm_sound", "")))
    }
    var timerSoundName by remember {
        mutableStateOf(soundName(prefs.getString("timer_sound", "")))
    }
    var alarmVibration by remember {
        mutableStateOf(prefs.getBoolean(VibrationUtils.PREF_ALARM_DEFAULT, true))
    }
    var timerVibration by remember {
        mutableStateOf(prefs.getBoolean(VibrationUtils.PREF_TIMER_DEFAULT, true))
    }
    val systemVibration = rememberSystemVibrationState()

    val defaultRingtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            val uriStr = pickedSoundValue(uri)
            prefs.edit().putString("default_alarm_sound", uriStr).apply()
            defaultRingtoneName = soundName(uriStr)
        }
    }

    val timerSoundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            val uriStr = pickedSoundValue(uri)
            prefs.edit().putString("timer_sound", uriStr).apply()
            timerSoundName = soundName(uriStr)
        }
    }

    ClockPage(
        title = stringResource(R.string.sound_and_vibration),
        onBack = onBack
    ) {
        SettingsSectionTitle(stringResource(R.string.sound_section_title))
        SettingsRow(
            icon = Icons.Outlined.MusicNote,
            title = stringResource(R.string.alarm_sound),
            subtitle = defaultRingtoneName,
            showArrow = true,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onClick = {
                defaultRingtoneLauncher.launch(
                    com.olivka.clock.utils.AlarmSoundPicker.createIntent(
                        prefs.getString("default_alarm_sound", "")
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = Icons.Outlined.Timer,
            title = stringResource(R.string.timer_sound),
            subtitle = timerSoundName,
            showArrow = true,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onClick = {
                timerSoundLauncher.launch(
                    com.olivka.clock.utils.AlarmSoundPicker.createIntent(
                        prefs.getString("timer_sound", "")
                    )
                )
            }
        )
        Text(
            text = stringResource(R.string.sound_defaults_hint),
            color = onSurfaceVariantDark,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        )

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        SettingsSectionTitle(stringResource(R.string.vibration))
        SystemVibrationWarning(
            state = systemVibration,
            showTouchWarning = false,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingsRowSwitch(
            icon = Icons.Outlined.Vibration,
            title = stringResource(R.string.alarm_vibration),
            subtitle = stringResource(R.string.alarm_vibration_hint),
            checked = alarmVibration,
            enabled = systemVibration.masterEnabled,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onDisabledClick = { DeviceRestrictions.openVibrationSettings(context) },
            onCheckedChange = {
                alarmVibration = it
                prefs.edit()
                    .putBoolean(VibrationUtils.PREF_ALARM_DEFAULT, it)
                    .apply()
            }
        )
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRowSwitch(
            icon = Icons.Outlined.Vibration,
            title = stringResource(R.string.timer_vibration),
            subtitle = stringResource(R.string.timer_vibration_hint),
            checked = timerVibration,
            enabled = systemVibration.masterEnabled,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onDisabledClick = { DeviceRestrictions.openVibrationSettings(context) },
            onCheckedChange = {
                timerVibration = it
                prefs.edit()
                    .putBoolean(VibrationUtils.PREF_TIMER_DEFAULT, it)
                    .apply()
            }
        )
        Text(
            text = stringResource(R.string.vibration_defaults_hint),
            color = onSurfaceVariantDark,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        )

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
    }
}
