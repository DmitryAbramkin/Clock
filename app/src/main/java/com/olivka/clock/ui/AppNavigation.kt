package com.olivka.clock.ui

/**
 * A navigation request delivered from a notification. [screen] uses the same
 * screen names as [StrictClockApp]; [timerId] is used by "TimerDetail".
 */
data class AppNavigationRequest(
    val screen: String,
    val timerId: Int = -1
)
