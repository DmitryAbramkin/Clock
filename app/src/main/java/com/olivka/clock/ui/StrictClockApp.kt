package com.olivka.clock.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olivka.clock.R
import com.olivka.clock.StrictClockApplication
import com.olivka.clock.data.AlarmEntity
import com.olivka.clock.service.AlarmService
import com.olivka.clock.service.TimerService
import com.olivka.clock.ui.theme.*
import com.olivka.clock.utils.WheelHaptics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun StrictClockApp(
    isWakeUp: Boolean = false,
    initialAlarmId: Int = -1,
    initialSoundUri: String = "",
    initialVibrationEnabled: Boolean = true,
    initialIsTimer: Boolean = false,
    initialTimerId: Int = -1,
    initialTimerLabel: String = "",
    initialScreen: String? = null,
    navigationRequests: SharedFlow<AppNavigationRequest>? = null
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val app = context.applicationContext as StrictClockApplication
    val prefs = remember {
        context.getSharedPreferences("strict_clock_prefs", android.content.Context.MODE_PRIVATE)
    }

    val hasAllPermissions = remember {
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        val hasExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(android.app.AlarmManager::class.java)
                ?.canScheduleExactAlarms() == true
        } else true
        hasNotif && hasExact
    }

    var showOverlayPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!isWakeUp && hasAllPermissions &&
            !android.provider.Settings.canDrawOverlays(context) &&
            !prefs.getBoolean("overlay_prompt_shown", false)
        ) {
            showOverlayPrompt = true
        }
    }

    val settingsScrollState = androidx.compose.foundation.rememberScrollState()
    var rootTab by rememberSaveable {
        mutableStateOf(prefs.getString("last_root_tab", "Alarms") ?: "Alarms")
    }
    val initialTargetScreen = when {
        isWakeUp -> "WakeUp"
        !hasAllPermissions -> "Permissions"
        initialScreen == "TimerDetail" && initialTimerId <= 0 -> "Timer"
        else -> initialScreen ?: rootTab
    }
    var currentScreen by remember { mutableStateOf(initialTargetScreen) }
    var selectedTimerId by remember { mutableIntStateOf(initialTimerId) }

    LaunchedEffect(navigationRequests) {
        navigationRequests?.collect { request ->
            if (currentScreen == "Permissions" ||
                currentScreen == "OnboardingTime" ||
                currentScreen == "WakeUp"
            ) {
                return@collect
            }
            when (request.screen) {
                "TimerDetail" -> {
                    selectedTimerId = request.timerId
                    currentScreen = if (request.timerId > 0) "TimerDetail" else "Timer"
                }
                "Alarms", "Timer", "Stopwatch", "Settings", "TimerSetup" ->
                    currentScreen = request.screen
            }
        }
    }

    var keepScreenOnTimer by remember {
        mutableStateOf(prefs.getBoolean("keep_screen_on_timer", false))
    }
    var keepScreenOnStopwatch by remember {
        mutableStateOf(prefs.getBoolean("keep_screen_on_stopwatch", false))
    }
    val keepScreenOn = when (currentScreen) {
        "TimerDetail" -> keepScreenOnTimer
        "Stopwatch" -> keepScreenOnStopwatch
        else -> false
    }
    KeepScreenOnEffect(enabled = keepScreenOn)

    LaunchedEffect(currentScreen) {
        if (currentScreen == "Alarms" || currentScreen == "Timer" ||
            currentScreen == "Stopwatch" || currentScreen == "Settings"
        ) {
            rootTab = currentScreen
            prefs.edit().putString("last_root_tab", currentScreen).apply()
        }
    }
    var selectedAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var permissionsFromSettings by remember { mutableStateOf(false) }

    var activeAlarmId by remember {
        mutableIntStateOf(
            if (AlarmService.isRinging) AlarmService.currentAlarmId else initialAlarmId
        )
    }
    var activeSoundUri by remember {
        mutableStateOf(
            if (AlarmService.isRinging) AlarmService.currentSoundUri else initialSoundUri
        )
    }
    var activeVibrationEnabled by remember {
        mutableStateOf(
            if (AlarmService.isRinging) AlarmService.currentVibrationEnabled else initialVibrationEnabled
        )
    }
    var activeIsTimer by remember {
        mutableStateOf(
            if (AlarmService.isRinging) AlarmService.currentIsTimer else initialIsTimer
        )
    }
    var activeAlarmLabel by remember { mutableStateOf("") }
    var activeTimerId by remember {
        mutableIntStateOf(
            if (AlarmService.isRinging) AlarmService.currentTimerId else initialTimerId
        )
    }
    var activeTimerLabel by remember {
        mutableStateOf(
            if (AlarmService.isRinging) AlarmService.currentTimerLabel else initialTimerLabel
        )
    }

    LaunchedEffect(activeAlarmId) {
        activeAlarmLabel = if (activeAlarmId != -1) {
            app.repository.getAlarmById(activeAlarmId)?.label.orEmpty()
        } else {
            ""
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && AlarmService.isRinging) {
                currentScreen = "WakeUp"
                activeAlarmId = AlarmService.currentAlarmId
                activeSoundUri = AlarmService.currentSoundUri
                activeVibrationEnabled = AlarmService.currentVibrationEnabled
                activeIsTimer = AlarmService.currentIsTimer
                activeTimerId = AlarmService.currentTimerId
                activeTimerLabel = AlarmService.currentTimerLabel
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // When the alarm starts while the app is already open, the system shows a
    // heads-up notification instead of launching the activity, so onResume
    // never fires. Observe the ringing state directly to switch to WakeUp.
    val ringing by AlarmService.ringing.collectAsState()
    LaunchedEffect(ringing) {
        if (ringing) {
            activeAlarmId = AlarmService.currentAlarmId
            activeSoundUri = AlarmService.currentSoundUri
            activeVibrationEnabled = AlarmService.currentVibrationEnabled
            activeIsTimer = AlarmService.currentIsTimer
            activeTimerId = AlarmService.currentTimerId
            activeTimerLabel = AlarmService.currentTimerLabel
            currentScreen = "WakeUp"
        }
    }

    val predictiveBackEnabled = prefs.getBoolean("predictive_back_enabled", true)
    val isRootScreen = currentScreen == "Alarms" ||
        currentScreen == "Timer" ||
        currentScreen == "Stopwatch" ||
        currentScreen == "Settings"

    val backProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(androidx.activity.BackEventCompat.EDGE_LEFT) }

    if (isRootScreen && !predictiveBackEnabled) {
        BackHandler(enabled = true) { activity?.finish() }
    }

    if (isRootScreen && predictiveBackEnabled) {
        PredictiveBackHandler { events ->
            try {
                events.collect { event ->
                    backSwipeEdge = event.swipeEdge
                    backProgress.snapTo(event.progress)
                }
                activity?.finish()
            } catch (_: CancellationException) {
                backProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(200)
                )
            }
        }
    }

    val alarmViewModel: AlarmViewModel = viewModel(
        factory = AlarmViewModelFactory(app.repository, app.scheduler)
    )
    val timerViewModel: TimerViewModel = viewModel(
        factory = TimerViewModelFactory(app.timerRepository)
    )

    // Restore notifications and exact alarms after the process was recreated.
    LaunchedEffect(Unit) {
        val storedTimers = app.timerRepository.allTimers.first()
        if (storedTimers.any { it.isRunning }) {
            TimerService.start(context, TimerService.ACTION_RESTORE)
        }
    }

    val snoozeMinutes = prefs.getInt("snooze_minutes", 5)
    val showSnoozeButton = prefs.getBoolean("show_snooze_button", true)

    val backValue = backProgress.value
    val backDirection = if (backSwipeEdge == androidx.activity.BackEventCompat.EDGE_LEFT) 1f else -1f
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1f - 0.08f * backValue
                scaleY = 1f - 0.08f * backValue
                translationX = backDirection * 24.dp.toPx() * backValue
                alpha = 1f - 0.12f * backValue
            }
    ) {
        val isTablet = maxWidth >= 600.dp
        val availableWidth = maxWidth
        val showNavigation = currentScreen == "Alarms" ||
            currentScreen == "Timer" ||
            currentScreen == "Stopwatch" ||
            currentScreen == "Settings"

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTablet && showNavigation) {
                MainNavigationRail(currentScreen = currentScreen, onNavigate = { currentScreen = it })
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = backgroundDark
            ) { innerPadding ->
                val horizontalMargin = if (isTablet) availableWidth * 0.1f else 0.dp
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = horizontalMargin)
                ) {
                    CompositionLocalProvider(
                        LocalPredictiveBackEnabled provides predictiveBackEnabled
                    ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val rootScreens = setOf("Alarms", "Timer", "Stopwatch", "Settings")
                            val openingSubpage =
                                initialState in rootScreens && targetState !in rootScreens
                            val closingSubpage =
                                initialState !in rootScreens && targetState in rootScreens
                            val gentle = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
                                durationMillis = 280,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                            when {
                                openingSubpage -> {
                                    (slideInHorizontally(gentle) { it / 6 } +
                                        fadeIn(androidx.compose.animation.core.tween(220))) togetherWith
                                        (slideOutHorizontally(gentle) { -it / 10 } +
                                            fadeOut(androidx.compose.animation.core.tween(180)))
                                }
                                closingSubpage -> {
                                    (slideInHorizontally(
                                        androidx.compose.animation.core.tween(
                                            300,
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        )
                                    ) { -it / 12 } +
                                        fadeIn(androidx.compose.animation.core.tween(220))) togetherWith
                                        (slideOutHorizontally(
                                            androidx.compose.animation.core.tween(
                                                320,
                                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                                            )
                                        ) { it / 4 } +
                                            fadeOut(androidx.compose.animation.core.tween(240)))
                                }
                                else -> {
                                    (fadeIn(
                                        androidx.compose.animation.core.tween(
                                            240,
                                            easing = NavPillEasing
                                        )
                                    ) + scaleIn(
                                        animationSpec = androidx.compose.animation.core.tween(
                                            300,
                                            easing = NavPillEasing
                                        ),
                                        initialScale = 0.98f
                                    )) togetherWith
                                        (fadeOut(
                                            androidx.compose.animation.core.tween(
                                                200,
                                                easing = NavPillEasing
                                            )
                                        ) + scaleOut(
                                            animationSpec = androidx.compose.animation.core.tween(
                                                300,
                                                easing = NavPillEasing
                                            ),
                                            targetScale = 0.98f
                                        ))
                                }
                            }
                        },
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            "Permissions" -> PermissionsScreen(
                                onComplete = {
                                    currentScreen = when {
                                        permissionsFromSettings -> "Settings"
                                        WheelHaptics.hasHapticFeedback(context) -> "Alarms"
                                        else -> "OnboardingTime"
                                    }
                                },
                                onBack = if (permissionsFromSettings) {
                                    { currentScreen = "Settings" }
                                } else {
                                    null
                                }
                            )
                            "OnboardingTime" -> OnboardingTimeScreen(
                                onComplete = { currentScreen = "Alarms" }
                            )
                            "Alarms" -> ClockDashboard(
                                viewModel = alarmViewModel,
                                onNavigateToSetup = {
                                    selectedAlarm = null
                                    currentScreen = "Setup"
                                },
                                onEditAlarm = { alarm ->
                                    selectedAlarm = alarm
                                    currentScreen = "Setup"
                                }
                            )
                            "Setup" -> SetupAlarmScreen(
                                viewModel = alarmViewModel,
                                alarm = selectedAlarm,
                                onBack = { currentScreen = "Alarms" }
                            )
                            "WakeUp" -> WakeUpScreen(
                                snoozeMinutes = snoozeMinutes,
                                showSnoozeButton = showSnoozeButton && !activeIsTimer,
                                label = if (activeIsTimer) {
                                    activeTimerLabel.ifBlank {
                                        stringResource(R.string.timer_finished)
                                    }
                                } else {
                                    activeAlarmLabel
                                },
                                onStopAlarm = {
                                    val wasTimer = if (AlarmService.isRinging) {
                                        AlarmService.currentIsTimer
                                    } else {
                                        activeIsTimer
                                    }
                                    val ringingTimerId = if (AlarmService.isRinging) {
                                        AlarmService.currentTimerId
                                    } else {
                                        activeTimerId
                                    }
                                    val ringingAlarmId = if (AlarmService.isRinging) {
                                        AlarmService.currentAlarmId
                                    } else {
                                        activeAlarmId
                                    }
                                    context.stopService(
                                        android.content.Intent(context, AlarmService::class.java)
                                    )
                                    if (wasTimer) {
                                        // A finished timer is not an alarm: remove
                                        // it and return to the timer list instead
                                        // of closing the app.
                                        if (ringingTimerId > 0) {
                                            TimerService.start(
                                                context,
                                                TimerService.ACTION_DELETE,
                                                ringingTimerId
                                            )
                                        }
                                        activeIsTimer = false
                                        activeTimerId = -1
                                        activeTimerLabel = ""
                                        activeAlarmId = -1
                                        currentScreen = "Timer"
                                    } else {
                                        app.ioScope.launch {
                                            val alarm = if (ringingAlarmId != -1) {
                                                app.repository.getAlarmById(ringingAlarmId)
                                            } else {
                                                null
                                            }
                                            if (alarm != null) {
                                                if (!alarm.shiftEnabled && alarm.daysOfWeek == "Never") {
                                                    if (prefs.getBoolean("auto_delete_one_time", false)) {
                                                        app.repository.delete(alarm)
                                                    } else {
                                                        app.repository.update(alarm.copy(isActive = false))
                                                    }
                                                    app.scheduler.cancel(alarm)
                                                } else {
                                                    app.scheduler.schedule(alarm)
                                                }
                                            }
                                            activity?.runOnUiThread { activity.finish() }
                                        }
                                    }
                                },
                                onSnoozeAlarm = {
                                    val ringingId = if (AlarmService.isRinging) {
                                        AlarmService.currentAlarmId
                                    } else {
                                        activeAlarmId
                                    }
                                    val sound = activeSoundUri.ifEmpty {
                                        AlarmService.currentSoundUri
                                    }
                                    val vibration = activeVibrationEnabled
                                    context.stopService(
                                        android.content.Intent(context, AlarmService::class.java)
                                    )
                                    app.scheduler.scheduleSnooze(
                                        alarmId = ringingId,
                                        soundUri = sound,
                                        vibrationEnabled = vibration,
                                        minutes = snoozeMinutes
                                    )
                                    activity?.finish()
                                }
                            )
                            "Timer" -> TimerScreen(
                                viewModel = timerViewModel,
                                onAddTimer = { currentScreen = "TimerSetup" },
                                onOpenTimer = { timerId ->
                                    selectedTimerId = timerId
                                    currentScreen = "TimerDetail"
                                }
                            )
                            "TimerSetup" -> TimerSetupScreen(
                                viewModel = timerViewModel,
                                onBack = { currentScreen = "Timer" }
                            )
                            "TimerDetail" -> TimerDetailScreen(
                                timerId = selectedTimerId,
                                viewModel = timerViewModel,
                                keepScreenOn = keepScreenOnTimer,
                                onKeepScreenOnChange = { enabled ->
                                    keepScreenOnTimer = enabled
                                    prefs.edit()
                                        .putBoolean("keep_screen_on_timer", enabled)
                                        .apply()
                                },
                                onBack = { currentScreen = "Timer" }
                            )
                            "Stopwatch" -> StopwatchScreen(
                                keepScreenOn = keepScreenOnStopwatch,
                                onKeepScreenOnChange = { enabled ->
                                    keepScreenOnStopwatch = enabled
                                    prefs.edit()
                                        .putBoolean("keep_screen_on_stopwatch", enabled)
                                        .apply()
                                }
                            )
                            "Settings" -> SettingsScreen(
                                onNavigateToAlarmPreference = { currentScreen = "AlarmPreference" },
                                onNavigateToColorScheme = { currentScreen = "ColorScheme" },
                                onNavigateToThemeMode = { currentScreen = "ThemeMode" },
                                onNavigateToCustomisation = { currentScreen = "Customisation" },
                                onNavigateToAbout = { currentScreen = "About" },
                                onNavigateToLanguage = { currentScreen = "Language" },
                                onNavigateToSoundVibration = {
                                    currentScreen = "SoundVibration"
                                },
                                onNavigateToPermissions = {
                                    permissionsFromSettings = true
                                    currentScreen = "Permissions"
                                },
                                scrollState = settingsScrollState
                            )
                            "Language" -> LanguageScreen(onBack = { currentScreen = "Settings" })
                            "SoundVibration" -> SoundVibrationScreen(
                                onBack = { currentScreen = "Settings" }
                            )
                            "AlarmPreference" -> AlarmPreferenceScreen(onBack = { currentScreen = "Settings" })
                            "ColorScheme" -> ColorSchemeScreen(onBack = { currentScreen = "Settings" })
                            "ThemeMode" -> ThemeModeScreen(onBack = { currentScreen = "Settings" })
                            "Customisation" -> CustomisationScreen(onBack = { currentScreen = "Settings" })
                            "About" -> AboutScreen(
                                onBack = { currentScreen = "Settings" },
                                onOpenLicenses = { currentScreen = "Licenses" }
                            )
                            "Licenses" -> LicensesScreen(
                                onBack = { currentScreen = "About" }
                            )
                            else -> ClockDashboard(
                                onNavigateToSetup = {
                                    selectedAlarm = null
                                    currentScreen = "Setup"
                                }
                            )
                        }
                    }
                    }

                    if (!isTablet && showNavigation) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            MainNavigationBar(
                                currentScreen = currentScreen,
                                onNavigate = { currentScreen = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOverlayPrompt) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("overlay_prompt_shown", true).apply()
                showOverlayPrompt = false
            },
            title = { Text(stringResource(R.string.overlay_permission), color = onSurfaceDark) },
            text = { Text(stringResource(R.string.overlay_prompt_message), color = onSurfaceVariantDark) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("overlay_prompt_shown", true).apply()
                    showOverlayPrompt = false
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                    )
                }) {
                    Text(stringResource(R.string.allow), color = primaryDark)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("overlay_prompt_shown", true).apply()
                    showOverlayPrompt = false
                }) {
                    Text(stringResource(R.string.not_now), color = onSurfaceVariantDark)
                }
            },
            containerColor = surfaceContainerHighDark
        )
    }
}

private val NavPillEasing =
    androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
private fun MainNavigationItem(
    destination: String,
    label: String,
    selected: Boolean,
    labelMaxWidth: Dp,
    onClick: () -> Unit
) {
    val pillColor = MaterialTheme.colorScheme.primary
    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    val background by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) pillColor else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(300, easing = NavPillEasing),
        label = "navItemBackground"
    )
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) selectedContentColor else contentColor,
        animationSpec = androidx.compose.animation.core.tween(300, easing = NavPillEasing),
        label = "navItemTint"
    )
    val horizontalPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 16.dp else 12.dp,
        animationSpec = androidx.compose.animation.core.tween(300, easing = NavPillEasing),
        label = "navItemPadding"
    )

    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(background)
            .clickable(
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.Crossfade(
            targetState = selected,
            animationSpec = androidx.compose.animation.core.tween(180),
            label = "navItemIcon"
        ) { isSelected ->
            Icon(
                imageVector = when (destination) {
                    "Alarms" -> if (isSelected) Icons.Filled.Alarm else Icons.Outlined.Alarm
                    "Timer" -> if (isSelected) Icons.Filled.HourglassEmpty else Icons.Outlined.HourglassEmpty
                    "Stopwatch" -> if (isSelected) Icons.Filled.Timer else Icons.Outlined.Timer
                    else -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                },
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = tint
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
            modifier = Modifier.clipToBounds(),
            enter = androidx.compose.animation.expandHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300, easing = NavPillEasing),
                expandFrom = Alignment.Start
            ),
            exit = androidx.compose.animation.shrinkHorizontally(
                animationSpec = androidx.compose.animation.core.tween(300, easing = NavPillEasing),
                shrinkTowards = Alignment.Start
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    modifier = Modifier.widthIn(max = labelMaxWidth),
                    fontSize = clampedSp(14f, maxScale = 1.25f),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = tint,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MainNavigationBar(currentScreen: String, onNavigate: (String) -> Unit) {
    val destinations = listOf("Alarms", "Timer", "Stopwatch", "Settings")
    val selectedIndex = destinations.indexOf(currentScreen).takeIf { it >= 0 } ?: 0

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth
        Surface(
            modifier = Modifier.widthIn(max = availableWidth),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Three unselected buttons are fixed at 48 dp + 4 dp of outer
                // padding. The selected button may use the rest, but its label
                // is capped and ellipsizes so the fourth button never leaves
                // the pill at large font scales.
                val selectedItemMaxWidth =
                    (availableWidth - 168.dp).coerceAtLeast(56.dp)
                val selectedLabelMaxWidth =
                    (selectedItemMaxWidth - 64.dp).coerceAtLeast(0.dp)
                destinations.forEachIndexed { index, destination ->
                    val destinationLabel = when (destination) {
                        "Alarms" -> stringResource(R.string.alarms)
                        "Timer" -> stringResource(R.string.timers)
                        "Stopwatch" -> stringResource(R.string.stopwatch)
                        else -> stringResource(R.string.settings)
                    }
                    Box(modifier = Modifier.padding(horizontal = 2.dp)) {
                        MainNavigationItem(
                            destination = destination,
                            label = destinationLabel,
                            selected = index == selectedIndex,
                            labelMaxWidth = selectedLabelMaxWidth,
                            onClick = { onNavigate(destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationRail(currentScreen: String, onNavigate: (String) -> Unit) {
    val showRailLabels =
        androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.05f
    NavigationRail(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
        ),
        containerColor = surfaceContainerLowDark
    ) {
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = currentScreen == "Alarms",
            onClick = { onNavigate("Alarms") },
            icon = {
                androidx.compose.animation.Crossfade(targetState = currentScreen == "Alarms") { isSelected ->
                    Icon(
                        if (isSelected) Icons.Filled.Alarm else Icons.Outlined.Alarm,
                        contentDescription = stringResource(R.string.alarms)
                    )
                }
            },
            label = if (showRailLabels) {
                {
                    Text(
                        stringResource(R.string.alarms),
                        fontSize = clampedSp(12f, 1.3f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            } else {
                null
            },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = onPrimaryContainerDark,
                selectedTextColor = onSurfaceDark,
                indicatorColor = primaryContainerDark,
                unselectedIconColor = onSurfaceVariantDark,
                unselectedTextColor = onSurfaceVariantDark
            )
        )
        NavigationRailItem(
            selected = currentScreen == "Timer",
            onClick = { onNavigate("Timer") },
            icon = {
                androidx.compose.animation.Crossfade(targetState = currentScreen == "Timer") { isSelected ->
                    Icon(
                        if (isSelected) Icons.Filled.HourglassEmpty else Icons.Outlined.HourglassEmpty,
                        contentDescription = stringResource(R.string.timers)
                    )
                }
            },
            label = if (showRailLabels) {
                {
                    Text(
                        stringResource(R.string.timers),
                        fontSize = clampedSp(12f, 1.3f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            } else {
                null
            },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = onPrimaryContainerDark,
                selectedTextColor = onSurfaceDark,
                indicatorColor = primaryContainerDark,
                unselectedIconColor = onSurfaceVariantDark,
                unselectedTextColor = onSurfaceVariantDark
            )
        )
        NavigationRailItem(
            selected = currentScreen == "Stopwatch",
            onClick = { onNavigate("Stopwatch") },
            icon = {
                androidx.compose.animation.Crossfade(targetState = currentScreen == "Stopwatch") { isSelected ->
                    Icon(
                        if (isSelected) Icons.Filled.Timer else Icons.Outlined.Timer,
                        contentDescription = stringResource(R.string.stopwatch)
                    )
                }
            },
            label = if (showRailLabels) {
                {
                    Text(
                        stringResource(R.string.stopwatch),
                        fontSize = clampedSp(12f, 1.3f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            } else {
                null
            },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = onPrimaryContainerDark,
                selectedTextColor = onSurfaceDark,
                indicatorColor = primaryContainerDark,
                unselectedIconColor = onSurfaceVariantDark,
                unselectedTextColor = onSurfaceVariantDark
            )
        )
        NavigationRailItem(
            selected = currentScreen == "Settings",
            onClick = { onNavigate("Settings") },
            icon = {
                val rotation by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (currentScreen == "Settings") 180f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "SettingsRotation"
                )
                androidx.compose.animation.Crossfade(targetState = currentScreen == "Settings") { isSelected ->
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.graphicsLayer(rotationZ = rotation)
                    )
                }
            },
            label = if (showRailLabels) {
                {
                    Text(
                        stringResource(R.string.settings),
                        fontSize = clampedSp(12f, 1.3f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            } else {
                null
            },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = onPrimaryContainerDark,
                selectedTextColor = onSurfaceDark,
                indicatorColor = primaryContainerDark,
                unselectedIconColor = onSurfaceVariantDark,
                unselectedTextColor = onSurfaceVariantDark
            )
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
