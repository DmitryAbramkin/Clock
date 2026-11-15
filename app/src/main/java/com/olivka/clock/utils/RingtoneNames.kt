package com.olivka.clock.utils

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

/** Resolves ringtone display names without the system "Default (...)" wrapper. */
object RingtoneNames {

    fun displayName(context: Context, value: String?): String? {
        if (value.isNullOrEmpty() || value == "Silent") return null
        val uri = Uri.parse(value)
        uri.getQueryParameter("title")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }

    fun defaultAlarmDisplayName(context: Context): String? {
        // The settings URI resolves to a "Default (Name)" label, so read the
        // underlying media URI first to get the plain ringtone title.
        Settings.System.getString(
            context.contentResolver,
            Settings.System.ALARM_ALERT
        )?.let { resolved ->
            displayName(context, resolved)?.let { return it }
        }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return null
        return displayName(context, uri.toString())
    }
}
