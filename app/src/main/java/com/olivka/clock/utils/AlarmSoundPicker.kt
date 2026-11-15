package com.olivka.clock.utils

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

/** Builds a ringtone picker intent that shows the currently selected sound. */
object AlarmSoundPicker {

    fun createIntent(currentUri: String?): Intent {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val existingUri = when {
            currentUri.isNullOrBlank() -> defaultUri
            currentUri == "Silent" -> null
            else -> Uri.parse(currentUri)
        }

        return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            if (defaultUri != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
            }
            if (existingUri != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
            }
        }
    }
}
