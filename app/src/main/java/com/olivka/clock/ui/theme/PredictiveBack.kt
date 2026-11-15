package com.olivka.clock.ui.theme

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

val LocalPredictiveBackEnabled = androidx.compose.runtime.staticCompositionLocalOf { true }

/**
 * Handles back for one in-app page. With predictive back enabled the page
 * follows the gesture and slides out; otherwise back is handled immediately.
 */
@Composable
fun PredictiveBackContainer(
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val enabled = LocalPredictiveBackEnabled.current
    val progress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableIntStateOf(androidx.activity.BackEventCompat.EDGE_LEFT) }
    var completed by remember { mutableStateOf(false) }

    if (enabled) {
        PredictiveBackHandler { events ->
            try {
                events.collect { event ->
                    completed = false
                    swipeEdge = event.swipeEdge
                    progress.snapTo(event.progress)
                }
                completed = true
                onBack()
            } catch (_: CancellationException) {
                progress.animateTo(0f, tween(260))
            }
        }
    } else {
        BackHandler(onBack = onBack)
    }

    // During a normal navigation the page is disposed while the screen
    // transition runs, so the gesture offset is kept and hands off to the
    // slide-out animation. If onBack only opens a dialog (unsaved changes),
    // the page is still composed and returns to rest after a short delay.
    LaunchedEffect(completed) {
        if (completed) {
            delay(450)
            progress.animateTo(0f, tween(280))
            completed = false
        }
    }

    val value = progress.value
    val direction = if (swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT) -1f else 1f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = direction * size.width * 0.18f * value
                scaleX = 1f - 0.03f * value
                scaleY = 1f - 0.03f * value
                alpha = 1f - 0.06f * value
            }
    ) {
        content()
    }
}
