package com.olivka.clock.ui

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.service.TimerService
import com.olivka.clock.ui.theme.*
import com.olivka.clock.utils.RingtoneNames
import com.olivka.clock.utils.VibrationUtils
import com.olivka.clock.utils.WheelHaptics
import java.util.Locale

/**
 * Full-screen timer setup, mirroring [SetupAlarmScreen]: time wheels, an
 * optional label and a top-bar Save action. Saving starts the new timer and
 * returns to the list.
 */
@Composable
fun TimerSetupScreen(
    viewModel: TimerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)
    val useKeyboardTimeInput = remember {
        prefs.getBoolean("use_keyboard_time_input", false)
    }
    val wheelHapticsEnabled = remember {
        WheelHaptics.isEnabled(prefs)
    }
    val systemVibration = rememberSystemVibrationState()

    var selectedHour by remember { mutableIntStateOf(prefs.getInt("timer_hour", 0)) }
    var selectedMinute by remember { mutableIntStateOf(prefs.getInt("timer_minute", 15)) }
    var selectedSecond by remember { mutableIntStateOf(prefs.getInt("timer_second", 0)) }
    val rawGlobalTimerSound = remember {
        prefs.getString("timer_sound", "") ?: ""
    }
    val globalTimerSoundUri = remember(rawGlobalTimerSound) {
        when {
            rawGlobalTimerSound == "Silent" ||
                rawGlobalTimerSound.isNotEmpty() -> rawGlobalTimerSound
            else -> RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )?.toString() ?: ""
        }
    }
    val globalTimerSoundName = remember(rawGlobalTimerSound) {
        when {
            rawGlobalTimerSound == "Silent" -> "Silent"
            rawGlobalTimerSound.isEmpty() ->
                RingtoneNames.defaultAlarmDisplayName(context) ?: "Default"
            else -> RingtoneNames.displayName(context, rawGlobalTimerSound)
                ?: "Default"
        }
    }
    var soundUri by remember { mutableStateOf(globalTimerSoundUri) }
    var soundName by remember { mutableStateOf(globalTimerSoundName) }
    var soundManual by remember { mutableStateOf(false) }
    var soundForceLink by remember { mutableStateOf(false) }
    val globalTimerVibration = remember {
        prefs.getBoolean(VibrationUtils.PREF_TIMER_DEFAULT, true)
    }
    var vibrationEnabled by remember {
        mutableStateOf(globalTimerVibration)
    }
    var vibrationManual by remember { mutableStateOf(false) }
    var vibrationForceLink by remember { mutableStateOf(false) }
    val soundIsLinked = soundForceLink || !soundManual
    val vibrationIsLinked = vibrationForceLink || !vibrationManual
    val defaultSoundLabel = stringResource(R.string.sound_default)
    val silentSoundLabel = stringResource(R.string.sound_silent)
    val selectedSoundLabel = stringResource(R.string.selected_sound)
    val globalTimerSoundDisplayName = when (globalTimerSoundName) {
        "Silent" -> silentSoundLabel
        "Default" -> defaultSoundLabel
        else -> globalTimerSoundName
    }
    val soundDisplayName = if (soundIsLinked) {
        if (globalTimerSoundDisplayName == defaultSoundLabel) {
            defaultSoundLabel
        } else {
            stringResource(
                R.string.sound_default_with_name,
                globalTimerSoundDisplayName
            )
        }
    } else {
        when (soundName) {
            "Default" -> defaultSoundLabel
            "Silent" -> silentSoundLabel
            else -> soundName
        }
    }
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            if (uri != null) {
                soundUri = uri.toString()
                soundName = RingtoneNames.displayName(context, soundUri)
                    ?: selectedSoundLabel
            } else {
                soundUri = "Silent"
                soundName = "Silent"
            }
            soundForceLink = false
            soundManual = soundUri != globalTimerSoundUri ||
                soundName != globalTimerSoundName
        }
    }
    var label by remember { mutableStateOf("") }
    var labelFocused by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    fun saveTimer() {
        if (isSaving) return
        val totalMs = (selectedHour * 60L * 60L +
            selectedMinute * 60L +
            selectedSecond) * 1000L
        if (totalMs <= 0L) {
            validationError = context.getString(R.string.timer_duration_required)
            return
        }
        isSaving = true
        val soundLinked = soundIsLinked
        val savedSoundName = if (soundLinked) "Default" else soundName
        val savedSoundUri = if (soundLinked) "" else soundUri

        val vibrationLinked = vibrationIsLinked
        val savedVibrationEnabled = if (vibrationLinked) {
            globalTimerVibration
        } else {
            vibrationEnabled
        }

        viewModel.createTimer(
            durationMs = totalMs,
            label = label,
            soundName = savedSoundName,
            soundUri = savedSoundUri,
            vibrationEnabled = savedVibrationEnabled,
            vibrationFollowsGlobal = vibrationLinked
        ) { created ->
            if (created == null) {
                isSaving = false
                validationError = context.getString(R.string.duplicate_timer)
            } else {
                prefs.edit()
                    .putInt("timer_hour", selectedHour)
                    .putInt("timer_minute", selectedMinute)
                    .putInt("timer_second", selectedSecond)
                    .apply()
                TimerService.start(context, TimerService.ACTION_START, created.id)
                onBack()
            }
        }
    }

    ClockScaffold(
        title = stringResource(R.string.set_timer),
        onBack = onBack,
        actions = {
            Button(
                onClick = { saveTimer() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryContainerDark,
                    contentColor = onPrimaryContainerDark
                ),
                modifier = Modifier.padding(end = 8.dp).heightIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    stringResource(R.string.save),
                    fontSize = clampedSp(14f),
                    maxLines = 1
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (useKeyboardTimeInput) {
                val timeStr = String.format(
                    Locale.US,
                    "%02d:%02d:%02d",
                    selectedHour,
                    selectedMinute,
                    selectedSecond
                )
                EditableTimeText(
                    time = timeStr,
                    onEdit = { showTimePickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 8.dp),
                    maxFontSize = 64.sp,
                    minFontSize = 28.sp,
                    color = primaryDark,
                    fontWeight = FontWeight.Medium
                )
            } else {
                WheelTimePicker(
                    hour = selectedHour,
                    minute = selectedMinute,
                    second = selectedSecond,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it },
                    onSecondChange = { selectedSecond = it },
                    showSeconds = true,
                    itemFontSize = 40.sp,
                    hapticsEnabled = wheelHapticsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsRow(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.timer_sound),
                subtitle = soundDisplayName,
                showArrow = true,
                trailingContent = if (soundManual) {
                    {
                        ResetIconButton {
                            soundUri = globalTimerSoundUri
                            soundName = globalTimerSoundName
                            soundManual = false
                            soundForceLink = true
                        }
                    }
                } else {
                    null
                }
            ) {
                ringtoneLauncher.launch(
                    com.olivka.clock.utils.AlarmSoundPicker.createIntent(soundUri)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (systemVibration.masterEnabled) {
                SettingsRowSwitch(
                    icon = Icons.Outlined.Vibration,
                    title = stringResource(R.string.vibration),
                    subtitle = stringResource(R.string.vibration_when_ringing),
                    checked = vibrationEnabled,
                    trailingContent = if (vibrationManual) {
                        {
                            ResetIconButton {
                                vibrationEnabled = globalTimerVibration
                                vibrationManual = false
                                vibrationForceLink = true
                            }
                        }
                    } else {
                        null
                    },
                    onCheckedChange = {
                        vibrationEnabled = it
                        vibrationForceLink = false
                        // Touching the switch is an explicit override, even when
                        // the resulting value matches the global setting.
                        vibrationManual = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val labelExpanded = labelFocused || label.isNotEmpty()
            val labelFocusRequester = remember { FocusRequester() }
            val labelFocusManager = LocalFocusManager.current
            val labelColor by animateColorAsState(
                targetValue = if (labelFocused) primaryDark else onSurfaceVariantDark,
                animationSpec = tween(160),
                label = "labelColor"
            )
            val inputRowHeight by animateDpAsState(
                targetValue = if (labelExpanded) 42.dp else 56.dp,
                animationSpec = tween(200),
                label = "inputRowHeight"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceContainerHighDark)
                    .clickable { labelFocusRequester.requestFocus() },
                horizontalAlignment = Alignment.Start
            ) {
                AnimatedVisibility(
                    visible = labelExpanded,
                    enter = fadeIn(tween(150)) +
                        expandVertically(tween(200), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(100)) +
                        shrinkVertically(tween(180), shrinkTowards = Alignment.Top)
                ) {
                    Text(
                        text = stringResource(R.string.alarm_label),
                        color = labelColor,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(inputRowHeight)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (label.isEmpty()) {
                        Text(
                            text = stringResource(R.string.alarm_label_empty),
                            color = onSurfaceVariantDark,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = label,
                        onValueChange = { label = it.take(40) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(labelFocusRequester)
                            .onFocusChanged { labelFocused = it.isFocused },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = onSurfaceDark,
                            lineHeight = 20.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(primaryDark),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { labelFocusManager.clearFocus() }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text(stringResource(R.string.set_timer), color = onSurfaceDark) },
            text = {
                if (useKeyboardTimeInput) {
                    CenteredTimeInput(
                        hour = selectedHour,
                        minute = selectedMinute,
                        second = selectedSecond,
                        onHourChange = { selectedHour = it },
                        onMinuteChange = { selectedMinute = it },
                        onSecondChange = { selectedSecond = it },
                        autoFocus = true
                    )
                } else {
                    WheelTimePicker(
                        hour = selectedHour,
                        minute = selectedMinute,
                        second = selectedSecond,
                        onHourChange = { selectedHour = it },
                        onMinuteChange = { selectedMinute = it },
                        onSecondChange = { selectedSecond = it },
                        showSeconds = true,
                        hapticsEnabled = wheelHapticsEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.ok), color = primaryDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }

    validationError?.let { message ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = { Text(stringResource(R.string.cannot_save_timer), color = onSurfaceDark) },
            text = { Text(message, color = onSurfaceVariantDark) },
            confirmButton = {
                TextButton(onClick = { validationError = null }) {
                    Text(stringResource(R.string.ok), color = primaryDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }
}
