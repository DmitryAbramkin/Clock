package com.olivka.clock.utils

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.app.usage.UsageStatsManager
import android.Manifest
import android.provider.Settings

/**
 * Helpers for the system settings that keep alarm apps alive on different
 * OEM ROMs. None of these require a runtime permission except the battery
 * optimization request dialog.
 */
object DeviceRestrictions {

    private const val OPSTR_RUN_ANY_IN_BACKGROUND = "android:runAnyInBackground"

    // Hidden platform value used by HyperOS/MIUI for "No restrictions".
    private const val STANDBY_BUCKET_EXEMPTED = 5

    private val autoStartComponents = listOf(
        // Xiaomi / Redmi / POCO
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        ),
        // Huawei
        ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        ),
        ComponentName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
        ),
        // Honor
        ComponentName(
            "com.hihonor.systemmanager",
            "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        ),
        // Oppo / Realme / OnePlus (ColorOS)
        ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ),
        ComponentName(
            "com.coloros.safecenter",
            "com.coloros.safecenter.startupapp.StartupAppListActivity"
        ),
        ComponentName(
            "com.oppo.safe",
            "com.oppo.safe.permission.startup.StartupAppListActivity"
        ),
        // Vivo / iQOO
        ComponentName(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ),
        ComponentName(
            "com.iqoo.secure",
            "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
        ),
        // Samsung
        ComponentName(
            "com.samsung.android.lool",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        ),
        ComponentName(
            "com.samsung.android.sm",
            "com.samsung.android.sm.ui.battery.BatteryActivity"
        ),
        // Meizu
        ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity"),
        // Asus
        ComponentName(
            "com.asus.mobilemanager",
            "com.asus.mobilemanager.powersaver.PowerSavingActivity"
        ),
        // Letv
        ComponentName(
            "com.letv.android.letvsafe",
            "com.letv.android.letvsafe.AutobootManageActivity"
        )
    )

    /**
     * True when SCHEDULE_EXACT_ALARM was granted through the special access
     * list. When canScheduleExactAlarms() is true but this is false, the app
     * is allow-listed automatically (for example via USE_EXACT_ALARM).
     */
    fun isExactAlarmAppOpAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val op = AppOpsManager.permissionToOp(Manifest.permission.SCHEDULE_EXACT_ALARM)
            ?: return false
        val uid = context.applicationInfo.uid
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(op, uid, context.packageName)
            } else {
                appOps.checkOpNoThrow(op, uid, context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Best-effort battery state. HyperOS/MIUI often keeps its own "No
     * restrictions" switch outside the AOSP battery whitelist, so the
     * background restriction and standby bucket are used as a fallback.
     */
    fun isBatteryUnrestricted(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) return true
        if (!isMiuiFamily(context)) return false
        return appStandbyBucket(context) == STANDBY_BUCKET_EXEMPTED
    }

    fun isMiuiFamily(context: Context): Boolean =
        isPackageInstalled(context, "com.miui.powerkeeper")

    private fun appStandbyBucket(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return -1
        return try {
            context.getSystemService(UsageStatsManager::class.java)
                ?.appStandbyBucket ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun appOpMode(context: Context): Int {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return AppOpsManager.MODE_DEFAULT
        val uid = context.applicationInfo.uid
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    OPSTR_RUN_ANY_IN_BACKGROUND,
                    uid,
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    OPSTR_RUN_ANY_IN_BACKGROUND,
                    uid,
                    context.packageName
                )
            }
        } catch (_: Exception) {
            AppOpsManager.MODE_DEFAULT
        }
    }

    fun isBackgroundActivityRestricted(context: Context): Boolean {
        val managerRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
        } else {
            false
        }
        val mode = appOpMode(context)
        val opRestricted = mode != AppOpsManager.MODE_ALLOWED &&
            mode != AppOpsManager.MODE_DEFAULT
        return managerRestricted || opRestricted
    }

    /** True when the ROM exposes a dedicated autostart screen. */
    fun hasAutoStartSettings(context: Context): Boolean =
        autoStartComponents.any { isComponentAvailable(context, it) }

    /**
     * Opens the OEM autostart screen. Returns false when no known activity
     * could be launched.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        for (component in autoStartComponents) {
            if (!isComponentAvailable(context, component)) continue
            try {
                context.startActivity(Intent().setComponent(component))
                return true
            } catch (_: Exception) {
                // Try the next known activity.
            }
        }
        return try {
            context.startActivity(appDetailsIntent(context))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return openBatteryOptimizationSettings(context)
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        try {
            context.startActivity(direct)
            return true
        } catch (_: Exception) {
            return openBatteryOptimizationSettings(context)
        }
    }

    /** Opens the closest available app-specific battery screen. */
    fun openBatterySettings(context: Context): Boolean {
        if (isMiuiFamily(context)) {
            return requestIgnoreBatteryOptimizations(context)
        }
        if (isIgnoringBatteryOptimizations(context)) {
            return openAppDetails(context)
        }
        return requestIgnoreBatteryOptimizations(context)
    }

    /** Opens the OEM battery details screen where possible, app info otherwise. */
    fun openBackgroundActivitySettings(context: Context): Boolean {
        if (isMiuiFamily(context)) {
            return requestIgnoreBatteryOptimizations(context)
        }
        return openAppDetails(context)
    }

    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openAppDetails(context)
        }
    }

    fun openAppDetails(context: Context): Boolean = try {
        context.startActivity(appDetailsIntent(context))
        true
    } catch (_: Exception) {
        false
    }

    fun openNotificationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            openAppDetails(context)
        }
    }

    fun openDontKillMyApp(context: Context): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/"))
        )
        true
    } catch (_: Exception) {
        false
    }

    private fun appDetailsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )

    private fun isComponentAvailable(context: Context, component: ComponentName): Boolean {
        return try {
            context.packageManager.resolveActivity(
                Intent().setComponent(component),
                0
            ) != null
        } catch (_: Exception) {
            false
        }
    }
}
