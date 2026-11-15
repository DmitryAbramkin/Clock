package com.olivka.clock

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.olivka.clock.service.AlarmService
import com.olivka.clock.ui.AppNavigationRequest
import com.olivka.clock.ui.StrictClockApp
import com.olivka.clock.ui.theme.StrictClockTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val navigationRequests = MutableSharedFlow<AppNavigationRequest>(
        replay = 1,
        extraBufferCapacity = 1
    )

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("strict_clock_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("app_language", "system") ?: "system"
        if (language == "en" || language == "ru") {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(Locale.forLanguageTag(language))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val isWakeUp = intent.getBooleanExtra("IS_WAKE_UP", false) || AlarmService.isRinging
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val soundUri = intent.getStringExtra("SOUND_URI") ?: ""
        val vibrationEnabled = intent.getBooleanExtra("VIBRATION_ENABLED", true)
        val isTimer = intent.getBooleanExtra("IS_TIMER", false)
        val timerId = intent.getIntExtra(
            "TIMER_ID",
            intent.getIntExtra("OPEN_TIMER_ID", -1)
        )
        val timerLabel = intent.getStringExtra("TIMER_LABEL") ?: ""
        val openScreen = intent.getStringExtra("OPEN_SCREEN")

        if (openScreen != null) {
            navigationRequests.tryEmit(AppNavigationRequest(openScreen, timerId))
        }

        if (isWakeUp) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setContent {
            StrictClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StrictClockApp(
                        isWakeUp = isWakeUp,
                        initialAlarmId = alarmId,
                        initialSoundUri = soundUri,
                        initialVibrationEnabled = vibrationEnabled,
                        initialIsTimer = isTimer,
                        initialTimerId = timerId,
                        initialTimerLabel = timerLabel,
                        initialScreen = openScreen,
                        navigationRequests = navigationRequests
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openScreen = intent.getStringExtra("OPEN_SCREEN") ?: return
        val timerId = intent.getIntExtra(
            "OPEN_TIMER_ID",
            intent.getIntExtra("TIMER_ID", -1)
        )
        navigationRequests.tryEmit(AppNavigationRequest(openScreen, timerId))
    }
}
