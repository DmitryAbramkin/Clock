package com.olivka.clock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * Locale used for app-level formatting. Only English and Russian are
 * translated; any other system language falls back to English, matching
 * the resource fallback.
 */
@Composable
fun appLocale(): Locale {
    val language = LocalConfiguration.current.locales[0].language
    return if (language == "ru") Locale.forLanguageTag("ru") else Locale.ENGLISH
}
