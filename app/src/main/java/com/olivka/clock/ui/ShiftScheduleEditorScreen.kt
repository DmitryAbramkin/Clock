package com.olivka.clock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShiftScheduleEditorScreen(
    shiftLength: Int,
    shiftStartDate: LocalDate,
    shiftSlots: List<ShiftSlotState>,
    defaultHour: Int,
    defaultMinute: Int,
    useKeyboardTimeInput: Boolean,
    wheelHapticsEnabled: Boolean,
    onBack: () -> Unit,
    onShiftLengthChange: (Int) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onSlotsChange: (List<ShiftSlotState>) -> Unit
) {
    val context = LocalContext.current

    var showShiftLengthDialog by remember { mutableStateOf(false) }
    var shiftLengthInput by remember { mutableStateOf(shiftLength.toString()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var slotPickerIndex by remember { mutableStateOf<Int?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun normalizedSlots(): List<ShiftSlotState> {
        val result = shiftSlots.toMutableList()
        while (result.size < shiftLength) {
            result.add(ShiftSlotState(true, defaultHour, defaultMinute))
        }
        return result.take(shiftLength)
    }

    fun updateSlot(
        index: Int,
        enabled: Boolean? = null,
        hour: Int? = null,
        minute: Int? = null
    ) {
        val list = normalizedSlots().toMutableList()
        val current = list.getOrNull(index) ?: return
        list[index] = current.copy(
            enabled = enabled ?: current.enabled,
            hour = hour ?: current.hour,
            minute = minute ?: current.minute
        )
        onSlotsChange(list)
    }

    val today = remember { LocalDate.now() }
    val displayBase = if (shiftStartDate.isBefore(today)) today else shiftStartDate
    val displayDates = remember(shiftStartDate, shiftLength, today) {
        List(shiftLength) { displayBase.plusDays(it.toLong()) }
    }
    val locale = appLocale()
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    }
    val fullDateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy", locale)
    }

    ClockScaffold(
        title = stringResource(R.string.shift_screen_title),
        onBack = onBack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsSectionTitle(stringResource(R.string.section_cycle))

            SettingsRow(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.shift_length),
                subtitle = pluralStringResource(
                    R.plurals.shift_days_count,
                    shiftLength,
                    shiftLength
                ),
                showArrow = true,
                onClick = {
                    shiftLengthInput = shiftLength.toString()
                    showShiftLengthDialog = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.shift_cycle_start),
                subtitle = shiftStartDate.format(fullDateFormatter),
                showArrow = true,
                onClick = { showStartDatePicker = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.shift_hint,
                    pluralStringResource(
                        R.plurals.shift_days_count,
                        shiftLength,
                        shiftLength
                    ),
                    shiftStartDate.format(fullDateFormatter)
                ),
                color = onSurfaceVariantDark,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(adaptiveDp(20.dp)))
            SettingsSectionTitle(stringResource(R.string.section_schedule_days))

            displayDates.forEach { date ->
                val index = cycleIndex(shiftStartDate, date, shiftLength)
                val slot = normalizedSlots().getOrNull(index)
                    ?: ShiftSlotState(true, defaultHour, defaultMinute)
                ShiftScheduleDayRow(
                    date = date.format(dateFormatter),
                    dayLabel = stringResource(
                        R.string.shift_day_label,
                        index + 1,
                        shiftLength
                    ),
                    slot = slot,
                    onClick = { slotPickerIndex = index },
                    onToggle = { enabled -> updateSlot(index, enabled = enabled) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showShiftLengthDialog) {
        AlertDialog(
            onDismissRequest = { showShiftLengthDialog = false },
            title = { Text(stringResource(R.string.shift_length_dialog_title), color = onSurfaceDark) },
            text = {
                OutlinedTextField(
                    value = shiftLengthInput,
                    onValueChange = { value ->
                        shiftLengthInput = value.filter { it.isDigit() }.take(2)
                    },
                    label = { Text(stringResource(R.string.shift_length)) },
                    supportingText = { Text(stringResource(R.string.shift_length_support)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = shiftLengthInput.toIntOrNull()
                    if (value == null || value !in 1..60) {
                        validationError = context.getString(R.string.shift_length_invalid)
                    } else {
                        onShiftLengthChange(value)
                        showShiftLengthDialog = false
                    }
                }) {
                    Text(stringResource(R.string.ok), color = primaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftLengthDialog = false }) {
                    Text(stringResource(R.string.cancel), color = onSurfaceVariantDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = shiftStartDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onStartDateChange(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        )
                    }
                    showStartDatePicker = false
                }) {
                    Text(stringResource(R.string.ok), color = primaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = onSurfaceVariantDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val editingIndex = slotPickerIndex
    if (editingIndex != null) {
        val slot = normalizedSlots().getOrNull(editingIndex)
            ?: ShiftSlotState(true, defaultHour, defaultMinute)
        SlotTimePickerDialog(
            title = stringResource(R.string.shift_day_label, editingIndex + 1, shiftLength),
            initialHour = slot.hour,
            initialMinute = slot.minute,
            useKeyboardTimeInput = useKeyboardTimeInput,
            wheelHapticsEnabled = wheelHapticsEnabled,
            onConfirm = { hour, minute ->
                updateSlot(editingIndex, enabled = true, hour = hour, minute = minute)
                slotPickerIndex = null
            },
            onDismiss = { slotPickerIndex = null }
        )
    }

    validationError?.let { message ->
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = { Text(stringResource(R.string.shift_length_dialog_title), color = onSurfaceDark) },
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
private fun ShiftScheduleDayRow(
    date: String,
    dayLabel: String,
    slot: ShiftSlotState,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date, color = onSurfaceDark, fontSize = 16.sp)
                Text(
                    text = "$dayLabel · " + if (slot.enabled) {
                        String.format(Locale.getDefault(), "%02d:%02d", slot.hour, slot.minute)
                    } else {
                        stringResource(R.string.shift_off)
                    },
                    color = if (slot.enabled) primaryDark else onSurfaceVariantDark,
                    fontSize = 14.sp
                )
            }
            ExpressiveSwitch(
                checked = slot.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun SlotTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    useKeyboardTimeInput: Boolean,
    wheelHapticsEnabled: Boolean,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = onSurfaceDark) },
        text = {
            if (useKeyboardTimeInput) {
                CenteredTimeInput(
                    hour = hour,
                    minute = minute,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it },
                    autoFocus = true
                )
            } else {
                WheelTimePicker(
                    hour = hour,
                    minute = minute,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it },
                    showSeconds = false,
                    hapticsEnabled = wheelHapticsEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text(stringResource(R.string.ok), color = primaryDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = onSurfaceVariantDark)
            }
        },
        containerColor = surfaceContainerHighDark
    )
}

private fun cycleIndex(start: LocalDate, date: LocalDate, length: Int): Int {
    val n = length.coerceAtLeast(1)
    val days = ChronoUnit.DAYS.between(start, date)
    return (((days % n) + n) % n).toInt()
}
