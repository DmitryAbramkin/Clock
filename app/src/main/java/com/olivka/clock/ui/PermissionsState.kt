package com.olivka.clock.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.olivka.clock.utils.DeviceRestrictions

/**
 * Snapshot of every system permission and background restriction that can
 * silently break an alarm.
 */
data class PermissionsState(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val exactAlarmsAutoGranted: Boolean,
    val fullScreenIntent: Boolean,
    val overlay: Boolean,
    val batteryUnrestricted: Boolean,
    val backgroundActivityAllowed: Boolean,
    val autoStartAvailable: Boolean
) {
    val essentialGranted: Boolean
        get() = notifications && exactAlarms

    /** Items that can be verified programmatically. Autostart cannot. */
    val checks: List<Boolean>
        get() = buildList {
            add(notifications)
            add(exactAlarms)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(fullScreenIntent)
            }
            add(overlay)
            add(batteryUnrestricted)
            add(backgroundActivityAllowed)
        }

    val grantedCount: Int
        get() = checks.count { it }

    val totalCount: Int
        get() = checks.size

    val allGranted: Boolean
        get() = checks.all { it }
}

fun readPermissionsState(context: Context): PermissionsState {
    val notificationsRuntime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val notificationManager =
        context.getSystemService(NotificationManager::class.java)
    val notificationsEnabled = notificationManager?.areNotificationsEnabled() != false
    val alarmChannel = notificationManager?.getNotificationChannel("alarm_channel_v3")
    val alarmChannelEnabled = alarmChannel == null ||
        alarmChannel.importance != NotificationManager.IMPORTANCE_NONE
    val notifications = notificationsRuntime && notificationsEnabled && alarmChannelEnabled

    val exactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    } else {
        true
    }

    val fullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java)
            ?.canUseFullScreenIntent() != false
    } else {
        true
    }

    return PermissionsState(
        notifications = notifications,
        exactAlarms = exactAlarms,
        exactAlarmsAutoGranted = exactAlarms &&
            !DeviceRestrictions.isExactAlarmAppOpAllowed(context),
        fullScreenIntent = fullScreenIntent,
        overlay = Settings.canDrawOverlays(context),
        batteryUnrestricted = DeviceRestrictions.isBatteryUnrestricted(context),
        backgroundActivityAllowed = !DeviceRestrictions.isBackgroundActivityRestricted(context),
        autoStartAvailable = DeviceRestrictions.hasAutoStartSettings(context)
    )
}
