package com.olivka.clock.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.olivka.clock.R
import com.olivka.clock.ui.theme.ClockScaffold
import com.olivka.clock.ui.theme.SettingsSectionTitle
import com.olivka.clock.ui.theme.adaptiveDp
import com.olivka.clock.ui.theme.clampedSp
import com.olivka.clock.ui.theme.backgroundDark
import com.olivka.clock.ui.theme.errorDark
import com.olivka.clock.ui.theme.onPrimaryContainerDark
import com.olivka.clock.ui.theme.onPrimaryDark
import com.olivka.clock.ui.theme.onSurfaceDark
import com.olivka.clock.ui.theme.onSurfaceVariantDark
import com.olivka.clock.ui.theme.onTertiaryContainerDark
import com.olivka.clock.ui.theme.primaryContainerDark
import com.olivka.clock.ui.theme.primaryDark
import com.olivka.clock.ui.theme.surfaceContainerHighDark
import com.olivka.clock.ui.theme.tertiaryContainerDark
import com.olivka.clock.utils.DeviceRestrictions

/**
 * Permission center, also used as the onboarding screen.
 *
 * Onboarding has no back button and explains the essentials. When opened
 * from Settings it gets the standard fixed header and can be revisited at
 * any time.
 */
@Composable
fun PermissionsScreen(
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)
    }
    var state by remember { mutableStateOf(readPermissionsState(context)) }
    var notificationsRequested by remember {
        mutableStateOf(prefs.getBoolean("notifications_requested", false))
    }
    val systemVibration = rememberSystemVibrationState()

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsRequested = true
        prefs.edit().putBoolean("notifications_requested", true).apply()
        state = readPermissionsState(context)
    }

    val refresh = { state = readPermissionsState(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openFullScreenIntentSettings: () -> Unit = {
        val packageUri = Uri.parse("package:${context.packageName}")
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
            )
        } catch (_: Exception) {
            DeviceRestrictions.openAppDetails(context)
        }
    }

    val requestNotifications: () -> Unit = {
        if (state.notifications) {
            DeviceRestrictions.openNotificationSettings(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationsRequested) {
                DeviceRestrictions.openNotificationSettings(context)
            } else {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            DeviceRestrictions.openNotificationSettings(context)
        }
    }

    val requestExactAlarms = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            } catch (_: Exception) {
                DeviceRestrictions.openAppDetails(context)
            }
        }
    }

    val openOverlay: () -> Unit = {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        } catch (_: Exception) {
            DeviceRestrictions.openAppDetails(context)
        }
    }

    val requestAll = {
        requestNotifications()
        if (!state.exactAlarms) requestExactAlarms()
        if (!state.fullScreenIntent &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            openFullScreenIntentSettings()
        }
    }

    val continueAction = {
        if (state.essentialGranted) {
            onComplete()
        } else {
            requestAll()
        }
    }

    val onboarding = onBack == null
    val body: @Composable ColumnScope.() -> Unit = {
        if (onboarding) {
            Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
            Text(
                text = stringResource(R.string.welcome),
                fontSize = clampedSp(32f),
                fontWeight = FontWeight.Bold,
                color = onSurfaceDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(adaptiveDp(12.dp)))
            Text(
                text = stringResource(R.string.permissions_intro),
                color = onSurfaceVariantDark,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
        }

        if (onboarding && !systemVibration.masterEnabled) {
            SystemVibrationOnboardingBanner()
            Spacer(modifier = Modifier.height(adaptiveDp(16.dp)))
        }

        PermissionsSummaryCard(state)

        Spacer(modifier = Modifier.height(adaptiveDp(16.dp)))
        SettingsSectionTitle(stringResource(R.string.permissions_section_alarm))
        PermissionCard(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.notifications),
            subtitle = stringResource(R.string.notifications_hint),
            granted = state.notifications,
            onClick = requestNotifications
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PermissionCard(
                icon = Icons.Outlined.Fullscreen,
                title = stringResource(R.string.full_screen_intent),
                subtitle = stringResource(R.string.full_screen_intent_hint),
                granted = state.fullScreenIntent,
                onClick = openFullScreenIntentSettings
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        PermissionCard(
            icon = Icons.Outlined.Layers,
            title = stringResource(R.string.overlay_permission),
            subtitle = stringResource(R.string.overlay_permission_hint),
            granted = state.overlay,
            onClick = openOverlay
        )

        Spacer(modifier = Modifier.height(adaptiveDp(16.dp)))
        SettingsSectionTitle(stringResource(R.string.permissions_section_precision))
        val exactAutoMessage = stringResource(R.string.exact_alarms_auto_toast)
        PermissionCard(
            icon = Icons.Outlined.Alarm,
            title = stringResource(R.string.exact_alarms),
            subtitle = when {
                !state.exactAlarms -> stringResource(R.string.exact_alarms_hint)
                state.exactAlarmsAutoGranted -> stringResource(R.string.exact_alarms_auto_hint)
                else -> stringResource(R.string.exact_alarms_hint)
            },
            granted = state.exactAlarms,
            onClick = {
                if (state.exactAlarmsAutoGranted) {
                    android.widget.Toast.makeText(
                        context,
                        exactAutoMessage,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    requestExactAlarms()
                }
            }
        )

        Spacer(modifier = Modifier.height(adaptiveDp(16.dp)))
        SettingsSectionTitle(stringResource(R.string.permissions_section_background))
        PermissionCard(
            icon = Icons.Outlined.BatterySaver,
            title = stringResource(R.string.battery_optimization),
            subtitle = if (state.batteryUnrestricted) {
                stringResource(R.string.battery_optimization_ok)
            } else {
                stringResource(R.string.battery_optimization_hint)
            },
            granted = state.batteryUnrestricted,
            onClick = { DeviceRestrictions.openBatterySettings(context) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionCard(
            icon = Icons.Outlined.ErrorOutline,
            title = stringResource(R.string.background_activity),
            subtitle = if (state.backgroundActivityAllowed) {
                stringResource(R.string.background_activity_ok)
            } else {
                stringResource(R.string.background_activity_hint)
            },
            granted = state.backgroundActivityAllowed,
            onClick = { DeviceRestrictions.openBackgroundActivitySettings(context) }
        )
        if (state.autoStartAvailable) {
            Spacer(modifier = Modifier.height(8.dp))
            PermissionCard(
                icon = Icons.Outlined.RestartAlt,
                title = stringResource(R.string.autostart),
                subtitle = stringResource(R.string.autostart_hint),
                granted = null,
                onClick = { DeviceRestrictions.openAutoStartSettings(context) }
            )
        }

        Spacer(modifier = Modifier.height(adaptiveDp(16.dp)))
        SettingsSectionTitle(stringResource(R.string.permissions_section_help))
        DontKillMyAppCard()

        if (onboarding) {
            Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
            Button(
                onClick = continueAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adaptiveDp(56.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.essentialGranted) primaryDark else primaryContainerDark,
                    contentColor = if (state.essentialGranted) {
                        onPrimaryDark
                    } else {
                        onPrimaryContainerDark
                    }
                )
            ) {
                Text(
                    text = if (state.essentialGranted) {
                        stringResource(R.string.continue_action)
                    } else {
                        stringResource(R.string.allow_permissions)
                    },
                    fontSize = clampedSp(18f, 1.5f),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(adaptiveDp(8.dp)))
            TextButton(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.permissions_check_again), color = primaryDark)
            }
        }

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))
    }

    if (onboarding) {
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
                horizontalAlignment = Alignment.Start,
                content = body
            )
        }
    } else {
        ClockScaffold(
            title = stringResource(R.string.permissions_reliability),
            onBack = onBack
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                content = body
            )
        }
    }
}

@Composable
private fun PermissionsSummaryCard(state: PermissionsState) {
    val ready = state.allGranted
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) primaryContainerDark else surfaceContainerHighDark
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = if (ready) onPrimaryContainerDark else errorDark,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (ready) {
                        stringResource(R.string.permissions_overall_ready)
                    } else {
                        stringResource(R.string.permissions_overall_attention)
                    },
                    color = onSurfaceDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = clampedSp(18f, 1.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.permissions_summary,
                        state.grantedCount,
                        state.totalCount
                    ),
                    color = onSurfaceVariantDark,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
        LinearProgressIndicator(
            progress = state.grantedCount.toFloat() / state.totalCount.coerceAtLeast(1),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            color = primaryDark,
            trackColor = surfaceContainerHighDark
        )
    }
}

@Composable
private fun DontKillMyAppCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val manufacturer = Build.MANUFACTURER
        .replaceFirstChar { it.uppercase() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tertiaryContainerDark),
        onClick = {
            if (!DeviceRestrictions.openDontKillMyApp(context)) {
                try {
                    uriHandler.openUri("https://dontkillmyapp.com/")
                } catch (_: Exception) {
                    // No browser installed; the guide stays reachable by URL.
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.OpenInNew,
                contentDescription = null,
                tint = onTertiaryContainerDark
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.dont_kill_my_app),
                    color = onTertiaryContainerDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    stringResource(R.string.dont_kill_my_app_device, manufacturer),
                    color = onTertiaryContainerDark.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when (granted) {
                    true -> primaryDark
                    false -> errorDark
                    null -> onSurfaceVariantDark
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = onSurfaceDark,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = onSurfaceVariantDark,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = when (granted) {
                    true -> Icons.Filled.CheckCircle
                    false -> Icons.Outlined.ChevronRight
                    null -> Icons.Outlined.ChevronRight
                },
                contentDescription = when (granted) {
                    true -> stringResource(R.string.granted)
                    else -> stringResource(R.string.open_settings)
                },
                tint = if (granted == true) primaryDark else onSurfaceVariantDark,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
