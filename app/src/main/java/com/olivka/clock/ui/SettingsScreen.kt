package com.olivka.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAlarmPreference: () -> Unit = {},
    onNavigateToColorScheme: () -> Unit = {},
    onNavigateToThemeMode: () -> Unit = {},
    onNavigateToCustomisation: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToSoundVibration: () -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundDark)
            .windowInsetsPadding(
                WindowInsets.systemBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                )
            )
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        val navigationInset = WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.settings),
            fontWeight = FontWeight.Bold,
            color = onSurfaceDark,
            fontSize = clampedSp(28f)
        )
        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        val context = androidx.compose.ui.platform.LocalContext.current
        val prefs = context.getSharedPreferences("strict_clock_prefs", android.content.Context.MODE_PRIVATE)
        var permissionsState by remember { mutableStateOf(readPermissionsState(context)) }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    permissionsState = readPermissionsState(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Alarms and timers
        SettingsSectionTitle(stringResource(R.string.alarms_and_timers))
        SettingsRow(
            icon = Icons.Outlined.MusicNote,
            title = stringResource(R.string.sound_and_vibration),
            subtitle = stringResource(R.string.sound_and_vibration_hint),
            showArrow = true,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onClick = onNavigateToSoundVibration
        )
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = Icons.Outlined.Settings,
            title = stringResource(R.string.alarm_preferences),
            subtitle = stringResource(R.string.alarm_preferences_hint),
            showArrow = true,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onClick = onNavigateToAlarmPreference
        )

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        // Appearance and input
        SettingsSectionTitle(stringResource(R.string.appearance_input))
        SettingsRow(
            icon = Icons.Outlined.DashboardCustomize,
            title = stringResource(R.string.input_navigation),
            subtitle = stringResource(R.string.input_navigation_hint),
            showArrow = true,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onClick = onNavigateToCustomisation
        )
        Spacer(modifier = Modifier.height(2.dp))
        val activeScheme by ThemeManager.activeScheme.collectAsState()
        val activeSchemeLabel = when (activeScheme) {
            "Dynamic" -> stringResource(R.string.dynamic_scheme)
            else -> activeScheme
        }
        SettingsRow(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.color_scheme),
            subtitle = activeSchemeLabel,
            showArrow = true,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onClick = onNavigateToColorScheme
        )
        Spacer(modifier = Modifier.height(2.dp))
        val themeMode by ThemeManager.themeMode.collectAsState()
        SettingsRow(
            icon = Icons.Outlined.DarkMode,
            title = stringResource(R.string.theme_mode),
            subtitle = when (themeMode) {
                "Light" -> stringResource(R.string.light_mode)
                "Dark" -> stringResource(R.string.dark_mode)
                else -> stringResource(R.string.system_default)
            },
            showArrow = true,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onClick = onNavigateToThemeMode
        )

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        // Permissions and background work
        SettingsSectionTitle(stringResource(R.string.settings_section_reliability))
        val permissionsSubtitle = if (permissionsState.allGranted) {
            stringResource(R.string.permissions_all_granted)
        } else {
            stringResource(
                R.string.permissions_summary,
                permissionsState.grantedCount,
                permissionsState.totalCount
            )
        }
        SettingsRow(
            icon = Icons.Outlined.VerifiedUser,
            title = stringResource(R.string.permissions_reliability),
            subtitle = permissionsSubtitle,
            showArrow = true,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onClick = onNavigateToPermissions
        )

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        // App-level settings
        SettingsSectionTitle(stringResource(R.string.app_section))
        val appLanguage = prefs.getString("app_language", "system") ?: "system"
        val languageSubtitle = when (appLanguage) {
            "en" -> stringResource(R.string.language_english)
            "ru" -> stringResource(R.string.language_russian)
            else -> stringResource(R.string.language_system)
        }
        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.language),
            subtitle = languageSubtitle,
            showArrow = true,
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
            onClick = onNavigateToLanguage
        )
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.about_app),
            subtitle = stringResource(R.string.about_app_hint),
            showArrow = true,
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp,
            onClick = onNavigateToAbout
        )
        Spacer(modifier = Modifier.height(navigationInset + 72.dp))
    }
}
