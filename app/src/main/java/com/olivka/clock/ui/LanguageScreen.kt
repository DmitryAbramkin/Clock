package com.olivka.clock.ui

import android.app.Activity
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("strict_clock_prefs", android.content.Context.MODE_PRIVATE)
    var selected by remember { mutableStateOf(prefs.getString("app_language", "system") ?: "system") }

    fun applyLanguage(tag: String) {
        selected = tag
        prefs.edit().putString("app_language", tag).commit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
            localeManager?.applicationLocales = if (tag == "system") {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
        }
        (context as? Activity)?.recreate()
    }

    ClockPage(
        title = stringResource(R.string.language),
        onBack = onBack
    ) {
        SettingsSectionTitle(stringResource(R.string.section_language))
        LanguageOption(
            title = stringResource(R.string.language_system),
            selected = selected == "system",
            onClick = { applyLanguage("system") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        LanguageOption(
            title = stringResource(R.string.language_english),
            selected = selected == "en",
            onClick = { applyLanguage("en") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        LanguageOption(
            title = stringResource(R.string.language_russian),
            selected = selected == "ru",
            onClick = { applyLanguage("ru") }
        )
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) primaryContainerDark else surfaceContainerDark,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Language,
                contentDescription = null,
                tint = if (selected) onPrimaryContainerDark else onSurfaceVariantDark
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = if (selected) onPrimaryContainerDark else onSurfaceDark,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = onPrimaryContainerDark,
                    unselectedColor = onSurfaceVariantDark
                )
            )
        }
    }
}
