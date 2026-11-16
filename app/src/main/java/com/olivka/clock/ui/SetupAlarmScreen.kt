package com.olivka.clock.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.data.AlarmEntity
import com.olivka.clock.ui.theme.*
import com.olivka.clock.utils.AlarmUtils
import com.olivka.clock.utils.RingtoneNames
import com.olivka.clock.utils.VibrationUtils
import com.olivka.clock.utils.WheelHaptics
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

private const val MODE_ONCE = "Once"
private const val MODE_DAILY = "Daily"
private const val MODE_WEEKDAYS = "Weekdays"
private const val MODE_WEEKENDS = "Weekends"
private const val MODE_CUSTOM = "Custom"
private const val MODE_SHIFT = "Shift"

internal data class ShiftSlotState(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int
)

private data class SetupSnapshot(
    val timeHour: Int,
    val timeMinute: Int,
    val repeatMode: String,
    val customDays: Set<Int>,
    val shiftLength: Int,
    val shiftStartDate: LocalDate,
    val shiftSlots: List<ShiftSlotState>,
    val soundUri: String,
    val soundName: String,
    val vibrationEnabled: Boolean,
    val label: String
)

private val DAY_OPTIONS = listOf(
    1 to "M",
    2 to "T",
    3 to "W",
    4 to "T",
    5 to "F",
    6 to "S",
    7 to "S"
)

private val ISO_DAY_NAMES = mapOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

private val NAME_TO_ISO = ISO_DAY_NAMES.entries.associate { (iso, name) -> name to iso }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupAlarmScreen(
    viewModel: AlarmViewModel? = null,
    alarm: AlarmEntity? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)
    val existingAlarms by viewModel?.allAlarms?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<AlarmEntity>()) }
    val currentTime = remember { Calendar.getInstance() }

    var selectedHour by remember {
        mutableIntStateOf(alarm?.timeHour ?: currentTime.get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by remember {
        mutableIntStateOf(alarm?.timeMinute ?: currentTime.get(Calendar.MINUTE))
    }

    val initialMode = remember {
        when {
            alarm == null -> MODE_ONCE
            alarm.shiftEnabled -> MODE_SHIFT
            alarm.daysOfWeek == "Never" -> MODE_ONCE
            alarm.daysOfWeek == "Daily" -> MODE_DAILY
            alarm.daysOfWeek == "Mon-Fri" -> MODE_WEEKDAYS
            alarm.daysOfWeek == "Sat-Sun" -> MODE_WEEKENDS
            else -> MODE_CUSTOM
        }
    }
    var repeatMode by remember { mutableStateOf(initialMode) }
    val initialCustomDays = remember {
        if (initialMode == MODE_CUSTOM) parseCustomDays(alarm?.daysOfWeek ?: "") else emptySet()
    }
    var customDays by remember { mutableStateOf(initialCustomDays) }

    var shiftLength by remember {
        mutableIntStateOf(alarm?.shiftLengthDays?.coerceIn(1, 60) ?: 4)
    }
    var shiftStartDate by remember {
        mutableStateOf(
            AlarmUtils.parseShiftDate(alarm?.shiftStartDate ?: "") ?: LocalDate.now()
        )
    }
    val initialShiftSlots = remember {
        val length = alarm?.shiftLengthDays?.coerceIn(1, 60) ?: 4
        val parsed = AlarmUtils.parseShiftSlots(alarm?.shiftSlots ?: "", length)
        val defaultHour = alarm?.timeHour ?: currentTime.get(Calendar.HOUR_OF_DAY)
        val defaultMinute = alarm?.timeMinute ?: currentTime.get(Calendar.MINUTE)
        List(length) { index ->
            val value = parsed.getOrNull(index)
            if (value != null) {
                ShiftSlotState(true, value / 60, value % 60)
            } else {
                ShiftSlotState(
                    enabled = alarm?.shiftEnabled != true,
                    hour = defaultHour,
                    minute = defaultMinute
                )
            }
        }
    }
    var shiftSlots by remember { mutableStateOf(initialShiftSlots) }
    var lastDefaultHour by remember { mutableIntStateOf(selectedHour) }
    var lastDefaultMinute by remember { mutableIntStateOf(selectedMinute) }

    // When the default alarm time changes, keep shift days that still use the
    // previous default in sync. Days with a custom time are left untouched.
    LaunchedEffect(selectedHour, selectedMinute) {
        val oldHour = lastDefaultHour
        val oldMinute = lastDefaultMinute
        val newHour = selectedHour
        val newMinute = selectedMinute
        if (oldHour != newHour || oldMinute != newMinute) {
            shiftSlots = shiftSlots.map { slot ->
                if (slot.hour == oldHour && slot.minute == oldMinute) {
                    slot.copy(hour = newHour, minute = newMinute)
                } else {
                    slot
                }
            }
            lastDefaultHour = newHour
            lastDefaultMinute = newMinute
        }
    }

    val rawGlobalAlarmSound = remember {
        prefs.getString("default_alarm_sound", "") ?: ""
    }
    val globalAlarmSoundUri = remember(rawGlobalAlarmSound) {
        when {
            rawGlobalAlarmSound == "Silent" ||
                rawGlobalAlarmSound.isNotEmpty() -> rawGlobalAlarmSound
            else -> RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )?.toString() ?: ""
        }
    }
    val globalAlarmSoundName = remember(rawGlobalAlarmSound) {
        when {
            rawGlobalAlarmSound == "Silent" -> "Silent"
            rawGlobalAlarmSound.isEmpty() ->
                RingtoneNames.defaultAlarmDisplayName(context) ?: "Default"
            else -> RingtoneNames.displayName(context, rawGlobalAlarmSound)
                ?: "Default"
        }
    }
    val globalVibration = remember {
        prefs.getBoolean(VibrationUtils.PREF_ALARM_DEFAULT, true)
    }
    val initialSoundLinked = alarm == null ||
        (alarm.soundUri.isEmpty() && alarm.soundName == "Default")
    val initialVibrationLinked = alarm == null || alarm.vibrationFollowsGlobal
    val initialSoundUri = if (alarm == null || initialSoundLinked) {
        globalAlarmSoundUri
    } else {
        alarm.soundUri
    }
    val initialSoundName = if (alarm == null || initialSoundLinked) {
        globalAlarmSoundName
    } else {
        alarm.soundName
    }
    val initialVibrationEnabled = if (alarm == null || initialVibrationLinked) {
        globalVibration
    } else {
        alarm.vibrationEnabled
    }
    var soundUri by remember { mutableStateOf(initialSoundUri) }
    var soundName by remember { mutableStateOf(initialSoundName) }
    var soundManual by remember { mutableStateOf(!initialSoundLinked) }
    var soundForceLink by remember { mutableStateOf(false) }
    var vibrationEnabled by remember { mutableStateOf(initialVibrationEnabled) }
    var vibrationManual by remember { mutableStateOf(!initialVibrationLinked) }
    var vibrationForceLink by remember { mutableStateOf(false) }
    val soundIsLinked = soundForceLink ||
        (!soundManual && initialSoundLinked)
    val vibrationIsLinked = vibrationForceLink ||
        (!vibrationManual && initialVibrationLinked)
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var labelFocused by remember { mutableStateOf(false) }
    val defaultSoundLabel = stringResource(R.string.sound_default)
    val silentSoundLabel = stringResource(R.string.sound_silent)
    val selectedSoundLabel = stringResource(R.string.selected_sound)
    val globalAlarmSoundDisplayName = when (globalAlarmSoundName) {
        "Silent" -> silentSoundLabel
        "Default" -> defaultSoundLabel
        else -> globalAlarmSoundName
    }
    val soundDisplayName = if (soundIsLinked) {
        if (globalAlarmSoundDisplayName == defaultSoundLabel) {
            defaultSoundLabel
        } else {
            stringResource(
                R.string.sound_default_with_name,
                globalAlarmSoundDisplayName
            )
        }
    } else {
        when (soundName) {
            "Default" -> defaultSoundLabel
            "Silent" -> silentSoundLabel
            else -> soundName
        }
    }

    val originalSnapshot = remember {
        SetupSnapshot(
            timeHour = alarm?.timeHour ?: currentTime.get(Calendar.HOUR_OF_DAY),
            timeMinute = alarm?.timeMinute ?: currentTime.get(Calendar.MINUTE),
            repeatMode = initialMode,
            customDays = initialCustomDays,
            shiftLength = shiftLength,
            shiftStartDate = shiftStartDate,
            shiftSlots = initialShiftSlots,
            soundUri = initialSoundUri,
            soundName = initialSoundName,
            vibrationEnabled = initialVibrationEnabled,
            label = alarm?.label ?: ""
        )
    }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    val useKeyboardTimeInput = remember {
        prefs.getBoolean("use_keyboard_time_input", false)
    }
    val wheelHapticsEnabled = remember {
        WheelHaptics.isEnabled(prefs)
    }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showShiftEditor by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                soundUri = uri.toString()
                soundName = RingtoneNames.displayName(context, soundUri)
                    ?: selectedSoundLabel
            } else {
                soundUri = "Silent"
                soundName = "Silent"
            }
            soundForceLink = false
            soundManual = !initialSoundLinked ||
                soundUri != globalAlarmSoundUri ||
                soundName != globalAlarmSoundName
        }
    }

    fun normalizedShiftSlots(): List<ShiftSlotState> {
        val result = shiftSlots.toMutableList()
        while (result.size < shiftLength) {
            result.add(
                ShiftSlotState(
                    enabled = true,
                    hour = selectedHour,
                    minute = selectedMinute
                )
            )
        }
        return result.take(shiftLength)
    }

    fun resizeShift(newLength: Int) {
        val length = newLength.coerceIn(1, 60)
        shiftSlots = List(length) { index ->
            shiftSlots.getOrNull(index) ?: ShiftSlotState(
                enabled = true,
                hour = selectedHour,
                minute = selectedMinute
            )
        }
        shiftLength = length
    }

    fun currentSnapshot(): SetupSnapshot = SetupSnapshot(
        timeHour = selectedHour,
        timeMinute = selectedMinute,
        repeatMode = repeatMode,
        customDays = customDays,
        shiftLength = shiftLength,
        shiftStartDate = shiftStartDate,
        shiftSlots = normalizedShiftSlots(),
        soundUri = soundUri,
        soundName = soundName,
        vibrationEnabled = vibrationEnabled,
        label = label
    )

    fun requestClose() {
        if (currentSnapshot() != originalSnapshot) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    if (showShiftEditor) {
        ShiftScheduleEditorScreen(
            shiftLength = shiftLength,
            shiftStartDate = shiftStartDate,
            shiftSlots = normalizedShiftSlots(),
            defaultHour = selectedHour,
            defaultMinute = selectedMinute,
            useKeyboardTimeInput = useKeyboardTimeInput,
            wheelHapticsEnabled = wheelHapticsEnabled,
            onBack = { showShiftEditor = false },
            onShiftLengthChange = { resizeShift(it) },
            onStartDateChange = { shiftStartDate = it },
            onSlotsChange = { shiftSlots = it }
        )
        return
    }


    fun selectMode(mode: String) {
        repeatMode = mode
        if (mode == MODE_SHIFT && shiftSlots.size != shiftLength) {
            resizeShift(shiftLength)
        }
        if (mode == MODE_CUSTOM && customDays.isEmpty()) {
            customDays = setOf(1, 2, 3, 4, 5)
        }
        showRepeatDialog = false
    }

    fun saveAlarm() {
        val slots = normalizedShiftSlots()
        if (repeatMode == MODE_CUSTOM && customDays.isEmpty()) {
            validationError = context.getString(R.string.shift_custom_days_required)
            return
        }
        if (repeatMode == MODE_SHIFT && slots.none { it.enabled }) {
            validationError = context.getString(R.string.shift_enable_at_least_one)
            return
        }

        val repeatDays = when (repeatMode) {
            MODE_ONCE, MODE_SHIFT -> "Never"
            MODE_DAILY -> "Daily"
            MODE_WEEKDAYS -> "Mon-Fri"
            MODE_WEEKENDS -> "Sat-Sun"
            MODE_CUSTOM -> customDays.sorted()
                .mapNotNull { ISO_DAY_NAMES[it] }
                .joinToString(", ")
            else -> "Never"
        }

        val soundLinked = soundIsLinked
        val savedSoundName = if (soundLinked) "Default" else soundName
        val savedSoundUri = if (soundLinked) "" else soundUri

        val vibrationLinked = vibrationIsLinked
        val savedVibrationEnabled = if (vibrationLinked) {
            globalVibration
        } else {
            vibrationEnabled
        }

        val newAlarm = AlarmEntity(
            id = alarm?.id ?: 0,
            timeHour = selectedHour,
            timeMinute = selectedMinute,
            daysOfWeek = repeatDays,
            isActive = true,
            soundName = savedSoundName,
            soundUri = savedSoundUri,
            vibrationEnabled = savedVibrationEnabled,
            vibrationFollowsGlobal = vibrationLinked,
            label = label.trim(),
            shiftEnabled = repeatMode == MODE_SHIFT,
            shiftLengthDays = shiftLength,
            shiftStartDate = shiftStartDate.toString(),
            shiftSlots = AlarmUtils.encodeShiftSlots(
                slots.map { if (it.enabled) it.hour * 60 + it.minute else null }
            )
        )
        val duplicate = existingAlarms.any { existing ->
            existing.id != newAlarm.id && AlarmUtils.isSameSchedule(existing, newAlarm)
        }
        if (duplicate) {
            validationError = context.getString(R.string.duplicate_alarm)
            return
        }
        if (alarm == null) {
            viewModel?.insert(newAlarm)
        } else {
            viewModel?.update(newAlarm)
        }
        val triggerTime = AlarmUtils.getNextTriggerTime(newAlarm)
        android.widget.Toast.makeText(
            context,
            AlarmUtils.formatTimeUntil(context, triggerTime),
            android.widget.Toast.LENGTH_LONG
        ).show()
        onBack()
    }

    val modeLabels = mapOf(
        MODE_ONCE to stringResource(R.string.repeat_once),
        MODE_DAILY to stringResource(R.string.repeat_daily),
        MODE_WEEKDAYS to stringResource(R.string.repeat_weekdays),
        MODE_WEEKENDS to stringResource(R.string.repeat_weekends),
        MODE_CUSTOM to stringResource(R.string.repeat_custom),
        MODE_SHIFT to stringResource(R.string.repeat_shift)
    )
    ClockScaffold(
        title = stringResource(R.string.alarm_setup_title),
        onBack = { requestClose() },
        actions = {
            Button(
                onClick = { saveAlarm() },
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
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )
                EditableTimeText(
                    time = timeStr,
                    onEdit = { showTimePickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 8.dp),
                    maxFontSize = 80.sp,
                    minFontSize = 32.sp,
                    color = primaryDark,
                    fontWeight = FontWeight.Medium
                )
            } else {
                WheelTimePicker(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it },
                    showSeconds = false,
                    itemFontSize = 44.sp,
                    hapticsEnabled = wheelHapticsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsRow(
                icon = Icons.Outlined.Repeat,
                title = stringResource(R.string.repeat),
                subtitle = modeLabels[repeatMode] ?: repeatMode,
                showArrow = true,
                onClick = { showRepeatDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (repeatMode) {
                MODE_WEEKDAYS -> Text(
                    stringResource(R.string.repeat_weekdays_hint),
                    color = onSurfaceVariantDark,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                MODE_WEEKENDS -> Text(
                    stringResource(R.string.repeat_weekends_hint),
                    color = onSurfaceVariantDark,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                MODE_CUSTOM -> {
                    Text(
                        stringResource(R.string.repeat_custom_hint),
                        color = onSurfaceVariantDark,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DaySelector(
                        selected = customDays,
                        onToggle = { iso ->
                            customDays = if (customDays.contains(iso)) {
                                customDays - iso
                            } else {
                                customDays + iso
                            }
                        }
                    )
                }
                MODE_SHIFT -> {
                    val slots = normalizedShiftSlots()
                    val offCount = slots.count { !it.enabled }
                    val offText = if (offCount == 0) {
                        stringResource(R.string.shift_all_days_on)
                    } else {
                        pluralStringResource(
                            R.plurals.shift_days_off,
                            offCount,
                            offCount
                        )
                    }
                    SettingsRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = stringResource(R.string.configure_cycle),
                        subtitle = stringResource(
                            R.string.shift_summary,
                            pluralStringResource(
                                R.plurals.shift_days_count,
                                shiftLength,
                                shiftLength
                            ),
                            offText
                        ),
                        showArrow = true,
                        onClick = { showShiftEditor = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.alarm_sound),
                subtitle = soundDisplayName,
                showArrow = true,
                trailingContent = if (soundManual) {
                    {
                        ResetIconButton {
                            soundUri = globalAlarmSoundUri
                            soundName = globalAlarmSoundName
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
            SettingsRowSwitch(
                icon = Icons.Outlined.Vibration,
                title = stringResource(R.string.vibration),
                subtitle = stringResource(R.string.vibration_when_ringing),
                checked = vibrationEnabled,
                trailingContent = if (vibrationManual) {
                    {
                        ResetIconButton {
                            vibrationEnabled = globalVibration
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

            Spacer(modifier = Modifier.height(16.dp))

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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            title = { Text(stringResource(R.string.set_alarm_time), color = onSurfaceDark) },
            text = {
                if (useKeyboardTimeInput) {
                    CenteredTimeInput(
                        hour = selectedHour,
                        minute = selectedMinute,
                        onHourChange = { selectedHour = it },
                        onMinuteChange = { selectedMinute = it },
                        autoFocus = true
                    )
                } else {
                    WheelTimePicker(
                        hour = selectedHour,
                        minute = selectedMinute,
                        onHourChange = { selectedHour = it },
                        onMinuteChange = { selectedMinute = it },
                        showSeconds = false,
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

    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text(stringResource(R.string.repeat), color = onSurfaceDark) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    modeLabels.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectMode(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = repeatMode == mode,
                                onClick = { selectMode(mode) }
                            )
                            Text(
                                text = label,
                                color = onSurfaceDark,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text(stringResource(R.string.close), color = primaryDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes), color = onSurfaceDark) },
            text = { Text(stringResource(R.string.unsaved_changes_message), color = onSurfaceVariantDark) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    saveAlarm()
                }) {
                    Text(stringResource(R.string.save), color = primaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.discard), color = onSurfaceVariantDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }

    validationError?.let { message ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = { Text(stringResource(R.string.cannot_save_alarm), color = onSurfaceDark) },
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

@Composable
private fun DaySelector(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        DAY_OPTIONS.forEach { (iso, letter) ->
            val isSelected = selected.contains(iso)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) primaryContainerDark else surfaceContainerHighDark,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable { onToggle(iso) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = localizedDayShort(iso),
                        color = if (isSelected) onPrimaryContainerDark else onSurfaceVariantDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun localizedDayShort(iso: Int): String = when (iso) {
    1 -> stringResource(R.string.day_mon_short)
    2 -> stringResource(R.string.day_tue_short)
    3 -> stringResource(R.string.day_wed_short)
    4 -> stringResource(R.string.day_thu_short)
    5 -> stringResource(R.string.day_fri_short)
    6 -> stringResource(R.string.day_sat_short)
    else -> stringResource(R.string.day_sun_short)
}

private fun parseCustomDays(days: String): Set<Int> {
    return days.split(",")
        .mapNotNull { NAME_TO_ISO[it.trim()] }
        .toSet()
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showArrow: Boolean = false,
    topStart: androidx.compose.ui.unit.Dp = 24.dp,
    topEnd: androidx.compose.ui.unit.Dp = 24.dp,
    bottomStart: androidx.compose.ui.unit.Dp = 24.dp,
    bottomEnd: androidx.compose.ui.unit.Dp = 24.dp,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val aTopStart by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else topStart, label = "")
    val aTopEnd by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else topEnd, label = "")
    val aBottomStart by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else bottomStart, label = "")
    val aBottomEnd by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else bottomEnd, label = "")

    Card(
        shape = RoundedCornerShape(aTopStart, aTopEnd, aBottomStart, aBottomEnd),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark),
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = onSurfaceVariantDark)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = onSurfaceDark,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = onSurfaceVariantDark,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (trailingContent != null) {
                trailingContent()
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (showArrow) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = onSurfaceVariantDark)
            }
        }
    }
}

@Composable
fun SettingsRowSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    topStart: androidx.compose.ui.unit.Dp = 24.dp,
    topEnd: androidx.compose.ui.unit.Dp = 24.dp,
    bottomStart: androidx.compose.ui.unit.Dp = 24.dp,
    bottomEnd: androidx.compose.ui.unit.Dp = 24.dp,
    trailingContent: (@Composable () -> Unit)? = null,
    onDisabledClick: () -> Unit = {},
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val aTopStart by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else topStart, label = "")
    val aTopEnd by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else topEnd, label = "")
    val aBottomStart by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else bottomStart, label = "")
    val aBottomEnd by androidx.compose.animation.core.animateDpAsState(if (isPressed) 24.dp else bottomEnd, label = "")

    Card(
        shape = RoundedCornerShape(aTopStart, aTopEnd, aBottomStart, aBottomEnd),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark),
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = { if (!enabled) onDisabledClick() else onCheckedChange(!checked) }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) onSurfaceVariantDark else onSurfaceVariantDark.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (enabled) onSurfaceDark else onSurfaceDark.copy(alpha = 0.5f),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = if (enabled) onSurfaceVariantDark else onSurfaceVariantDark.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (trailingContent != null) {
                trailingContent()
                Spacer(modifier = Modifier.width(4.dp))
            }
            ExpressiveSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { if (enabled) onCheckedChange(it) }
            )
        }
    }
}
