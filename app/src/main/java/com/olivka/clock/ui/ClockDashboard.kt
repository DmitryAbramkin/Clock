package com.olivka.clock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import com.olivka.clock.ui.theme.ExpressiveSwitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.data.AlarmEntity
import com.olivka.clock.ui.theme.*
import com.olivka.clock.utils.AlarmUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockDashboard(viewModel: AlarmViewModel? = null, onNavigateToSetup: () -> Unit = {}, onEditAlarm: (com.olivka.clock.data.AlarmEntity) -> Unit = {}) {
    val rawAlarms by viewModel?.allAlarms?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val alarms = remember(rawAlarms) {
        rawAlarms.sortedBy { alarm ->
            if (alarm.isActive) {
                AlarmUtils.getNextTriggerTime(alarm) ?: Long.MAX_VALUE
            } else {
                Long.MAX_VALUE
            }
        }
    }
    val nextAlarm = alarms.firstOrNull { alarm ->
        alarm.isActive && AlarmUtils.getNextTriggerTime(alarm) != null
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val navigationInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
        ),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSetup,
                shape = RoundedCornerShape(24.dp), // Squircle shape from design
                containerColor = tertiaryDark, // Pinkish color from design
                contentColor = onTertiaryDark,
                modifier = Modifier
                    .padding(bottom = navigationInset + 72.dp, end = 16.dp)
                    .size(80.dp) // Large FAB
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm), modifier = Modifier.size(32.dp))
            }
        },
        containerColor = backgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.alarms),
                color = onSurfaceDark,
                fontSize = clampedSp(28f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item(key = "next_alarm") {
                NextAlarmCard(nextAlarm = nextAlarm, now = now)
            }

            items(alarms, key = { it.id }) { alarm ->
                var showDeleteConfirm by remember { mutableStateOf(false) }
                var showSkipDialog by remember { mutableStateOf(false) }
                var skipTargetAt by remember { mutableStateOf<Long?>(null) }
                val localContext = androidx.compose.ui.platform.LocalContext.current
                val localPrefs = localContext.getSharedPreferences("strict_clock_prefs", android.content.Context.MODE_PRIVATE)
                val noSwipeConfirm = localPrefs.getBoolean("no_swipe_delete_confirm", false)
                
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                            if (noSwipeConfirm) {
                                viewModel?.delete(alarm)
                                true
                            } else {
                                showDeleteConfirm = true
                                false
                            }
                        } else {
                            false
                        }
                    }
                )
                
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text(stringResource(R.string.delete_alarm), color = onSurfaceDark) },
                        text = { Text(stringResource(R.string.delete_alarm_question), color = onSurfaceVariantDark) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel?.delete(alarm)
                                showDeleteConfirm = false
                            }) {
                                Text(stringResource(R.string.delete), color = errorDark)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text(stringResource(R.string.cancel), color = primaryDark)
                            }
                        },
                        containerColor = surfaceContainerHighDark,
                        titleContentColor = onSurfaceDark,
                        textContentColor = onSurfaceVariantDark
                    )
                }
                
                val targetAt = skipTargetAt
                if (showSkipDialog && targetAt != null) {
                    val locale = appLocale()
                    val pattern = stringResource(R.string.next_alarm_date_pattern)
                    val skipDate = formatTriggerDate(targetAt, locale, pattern)
                    val skipShortDate = formatDateOnly(targetAt, locale)
                    AlertDialog(
                        onDismissRequest = { showSkipDialog = false },
                        title = { Text(stringResource(R.string.skip_alarm_title), color = onSurfaceDark) },
                        text = { Text(stringResource(R.string.skip_alarm_question, skipDate), color = onSurfaceVariantDark) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel?.update(
                                    alarm.copy(isActive = true, skipNextTriggerAt = targetAt)
                                )
                                showSkipDialog = false
                                android.widget.Toast.makeText(
                                    localContext,
                                    localContext.getString(R.string.alarm_skipped),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text(stringResource(R.string.skip_next, skipShortDate), color = primaryDark)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                viewModel?.update(
                                    alarm.copy(isActive = false, skipNextTriggerAt = 0L)
                                )
                                showSkipDialog = false
                            }) {
                                Text(stringResource(R.string.disable_alarm), color = errorDark)
                            }
                        },
                        containerColor = surfaceContainerHighDark,
                        titleContentColor = onSurfaceDark,
                        textContentColor = onSurfaceVariantDark
                    )
                }

                val timeStr = String.format("%02d:%02d", alarm.timeHour, alarm.timeMinute)
                val skipPending = alarm.isActive && alarm.skipNextTriggerAt > now
                val displayActive = alarm.isActive && !skipPending
                val nextTrigger = if (alarm.isActive) AlarmUtils.getNextTriggerTime(alarm) else null
                val countdown = when {
                    skipPending -> {
                        val locale = appLocale()
                        val skippedDate = formatDateOnly(alarm.skipNextTriggerAt, locale)
                        val nextDate = nextTrigger?.let { formatDateOnly(it, locale) }
                            ?: stringResource(R.string.alarm_no_upcoming)
                        stringResource(R.string.alarm_skip_status, skippedDate, nextDate)
                    }
                    alarm.isActive -> alarmCountdownText(nextTrigger, now)
                    else -> stringResource(R.string.alarm_inactive)
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = errorContainerDark
                        val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(28.dp))
                                .padding(horizontal = 24.dp),
                            contentAlignment = alignment
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete), tint = onErrorContainerDark, modifier = Modifier.size(32.dp))
                        }
                    }
                ) {
                    AlarmCard(
                        time = timeStr,
                        label = alarm.label,
                        summary = alarmSummary(alarm),
                        countdown = countdown,
                        isActive = displayActive,
                        onToggle = { isChecked ->
                            if (isChecked) {
                                val updatedAlarm = alarm.copy(
                                    isActive = true,
                                    skipNextTriggerAt = 0L
                                )
                                viewModel?.update(updatedAlarm)
                                val triggerTime = AlarmUtils.getNextTriggerTime(updatedAlarm)
                                val toastMsg = AlarmUtils.formatTimeUntil(localContext, triggerTime)
                                android.widget.Toast.makeText(localContext, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val isRepeating = alarm.shiftEnabled || alarm.daysOfWeek != "Never"
                                val trigger = AlarmUtils.getNextTriggerTime(alarm)
                                if (isRepeating && trigger != null) {
                                    skipTargetAt = trigger
                                    showSkipDialog = true
                                } else {
                                    viewModel?.update(
                                        alarm.copy(isActive = false, skipNextTriggerAt = 0L)
                                    )
                                }
                            }
                        },
                        onClick = { onEditAlarm(alarm) }
                    )
                }
            }
            
            item {
                // Room to scroll the last alarm above the FAB and the nav pill.
                Spacer(modifier = Modifier.height(navigationInset + 160.dp))
            }
            }
        }
    }
}

@Composable
private fun NextAlarmCard(nextAlarm: AlarmEntity?, now: Long) {
    val locale = appLocale()
    val datePattern = stringResource(R.string.next_alarm_date_pattern)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = primaryContainerDark),
        border = BorderStroke(2.dp, primaryDark.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.next_alarm),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp,
                color = onPrimaryContainerDark.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (nextAlarm != null) {
                AutoSizeText(
                    text = alarmCountdownText(AlarmUtils.getNextTriggerTime(nextAlarm), now),
                    maxFontSize = 22.sp,
                    minFontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    color = onPrimaryContainerDark,
                    fontWeight = FontWeight.Bold,
                    contentAlignment = Alignment.CenterStart
                )
                Spacer(modifier = Modifier.height(6.dp))
                val triggerDate = AlarmUtils.getNextTriggerTime(nextAlarm)
                    ?.let { formatTriggerDate(it, locale, datePattern) }
                    ?: stringResource(R.string.alarm_no_upcoming)
                Text(
                    text = triggerDate,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = onPrimaryContainerDark.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = stringResource(R.string.no_active_alarms),
                    fontSize = clampedSp(20f, 1.5f),
                    lineHeight = clampedSp(24f, 1.5f),
                    fontWeight = FontWeight.Medium,
                    color = onPrimaryContainerDark,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlarmCard(
    time: String,
    label: String = "",
    summary: String,
    countdown: String,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceContainerHighDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (label.isNotBlank()) {
                    Text(
                        text = label,
                        color = primaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                AutoSizeText(
                    text = time,
                    maxFontSize = 36.sp,
                    minFontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isActive) onSurfaceDark else onSurfaceVariantDark,
                    contentAlignment = Alignment.CenterStart
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = onSurfaceVariantDark
                    ),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = countdown,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isActive) primaryDark else onSurfaceVariantDark
                    ),
                    maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            ExpressiveSwitch(
                checked = isActive,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun alarmSummary(alarm: AlarmEntity): String = when {
    alarm.shiftEnabled -> stringResource(
        R.string.alarm_shift,
        pluralStringResource(
            R.plurals.shift_days_count,
            alarm.shiftLengthDays,
            alarm.shiftLengthDays
        )
    )
    alarm.daysOfWeek == "Never" -> stringResource(R.string.alarm_once)
    alarm.daysOfWeek == "Daily" -> stringResource(R.string.alarm_every_day)
    alarm.daysOfWeek == "Mon-Fri" -> stringResource(R.string.alarm_weekdays)
    alarm.daysOfWeek == "Sat-Sun" -> stringResource(R.string.alarm_weekends)
    else -> localizedDays(alarm.daysOfWeek)
}

@Composable
private fun localizedDays(days: String): String {
    return days.split(", ").mapNotNull { day ->
        when (day) {
            "Mon" -> stringResource(R.string.day_mon_short)
            "Tue" -> stringResource(R.string.day_tue_short)
            "Wed" -> stringResource(R.string.day_wed_short)
            "Thu" -> stringResource(R.string.day_thu_short)
            "Fri" -> stringResource(R.string.day_fri_short)
            "Sat" -> stringResource(R.string.day_sat_short)
            "Sun" -> stringResource(R.string.day_sun_short)
            else -> null
        }
    }.joinToString(", ")
}

@Composable
private fun alarmCountdownText(targetTime: Long?, now: Long): String {
    if (targetTime == null) return stringResource(R.string.alarm_no_upcoming)
    val diff = targetTime - now
    if (diff < 60_000L) {
        return stringResource(
            R.string.countdown_in,
            stringResource(R.string.countdown_less_than_minute)
        )
    }
    val totalMinutes = diff / 60_000L
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    val parts = mutableListOf<String>()
    if (days > 0) parts.add(stringResource(R.string.countdown_days, days))
    if (hours > 0 || days > 0) parts.add(stringResource(R.string.countdown_hours, hours))
    parts.add(stringResource(R.string.countdown_minutes, minutes))
    return stringResource(R.string.countdown_in, parts.joinToString(" "))
}

private fun formatTriggerDate(target: Long, locale: Locale, pattern: String): String {
    return Instant.ofEpochMilli(target)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, locale))
}

private fun formatDateOnly(target: Long, locale: Locale): String {
    return Instant.ofEpochMilli(target)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM", locale))
}
