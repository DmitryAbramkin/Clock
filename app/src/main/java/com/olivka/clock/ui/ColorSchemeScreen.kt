package com.olivka.clock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*

@Composable
fun ColorSchemeScreen(onBack: () -> Unit) {
    val activeScheme by ThemeManager.activeScheme.collectAsState()

    ClockPage(
        title = stringResource(R.string.color_scheme),
        onBack = onBack
    ) {
        SettingsSectionTitle(stringResource(R.string.section_dynamic_colors))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
        ) {
            SchemeOption(
                title = stringResource(R.string.dynamic_material_you),
                subtitle = stringResource(R.string.dynamic_hint),
                isSelected = activeScheme == "Dynamic",
                onClick = { ThemeManager.setScheme("Dynamic") }
            )
        }

        Spacer(modifier = Modifier.height(adaptiveDp(24.dp)))

        SettingsSectionTitle(stringResource(R.string.presets))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceContainerHighDark)
        ) {
            Column {
                val presets = listOf(
                    Triple("Strict Default", R.string.scheme_strict_default, R.string.scheme_strict_default_desc),
                    Triple("Noctali", R.string.scheme_noctali, R.string.scheme_noctali_desc),
                    Triple("Lavender", R.string.scheme_lavender, R.string.scheme_lavender_desc),
                    Triple("Pink", R.string.scheme_pink, R.string.scheme_pink_desc),
                    Triple("Mocha", R.string.scheme_mocha, R.string.scheme_mocha_desc)
                )

                presets.forEachIndexed { index, (schemeId, titleRes, subtitleRes) ->
                    SchemeOption(
                        title = stringResource(titleRes),
                        subtitle = stringResource(subtitleRes),
                        isSelected = activeScheme == schemeId,
                        onClick = { ThemeManager.setScheme(schemeId) }
                    )

                    if (index < presets.lastIndex) {
                        HorizontalDivider(
                            color = outlineVariantDark,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SchemeOption(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
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
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = primaryDark,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}
