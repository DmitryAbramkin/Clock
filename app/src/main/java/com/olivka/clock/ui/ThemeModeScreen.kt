package com.olivka.clock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*

@Composable
fun ThemeModeScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeMode by ThemeManager.themeMode.collectAsState()

    ClockPage(
        title = stringResource(R.string.theme_mode),
        onBack = onBack
    ) {
        SettingsSectionTitle(stringResource(R.string.section_theme_mode))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
        ) {
            Column {
                ThemeModeOption(
                    title = stringResource(R.string.system_default),
                    subtitle = stringResource(R.string.follow_system_hint),
                    isSelected = themeMode == "System",
                    onClick = { ThemeManager.setThemeMode("System") }
                )
                HorizontalDivider(
                    color = outlineVariantDark,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ThemeModeOption(
                    title = stringResource(R.string.light_mode),
                    subtitle = stringResource(R.string.light_mode_hint),
                    isSelected = themeMode == "Light",
                    onClick = { ThemeManager.setThemeMode("Light") }
                )
                HorizontalDivider(
                    color = outlineVariantDark,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                ThemeModeOption(
                    title = stringResource(R.string.dark_mode),
                    subtitle = stringResource(R.string.dark_mode_hint),
                    isSelected = themeMode == "Dark",
                    onClick = { ThemeManager.setThemeMode("Dark") }
                )
            }
        }

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        SettingsSectionTitle(stringResource(R.string.section_display))
        val isAmoled by ThemeManager.isAmoled.collectAsState()
        val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
        val isDarkTheme = themeMode == "Dark" || (themeMode == "System" && isSystemDark)
        val amoledError = stringResource(R.string.amoled_light_error)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.amoled_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDarkTheme) onSurfaceDark else onSurfaceVariantDark
                    )
                },
                supportingContent = {
                    Text(
                        stringResource(R.string.amoled_mode_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = onSurfaceVariantDark
                    )
                },
                trailingContent = {
                    ExpressiveSwitch(
                        checked = isAmoled,
                        enabled = isDarkTheme,
                        onCheckedChange = { ThemeManager.setAmoled(it) }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isDarkTheme) {
                            ThemeManager.setAmoled(!isAmoled)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                amoledError,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@Composable
fun ThemeModeOption(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) primaryDark else onSurfaceDark
            )
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = onSurfaceVariantDark
            )
        },
        trailingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = primaryDark,
                    unselectedColor = onSurfaceVariantDark
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
