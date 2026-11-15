package com.olivka.clock.ui.theme

import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Vertical wheels for hours, minutes and seconds. The value on the centre
 * line is the selected one; each wheel snaps to it and scrolls endlessly
 * through repeated value sets, so there is never an empty row at 00 or 59.
 *
 * The wheel follows the system font scale (up to a layout-safe cap) and the
 * column width is clamped to the available space, so the numbers stay large
 * and readable in a dialog as well as inline on the alarm screen. Values
 * farther from the centre line are dimmed and scaled down.
 *
 * A wheel can be hidden with [showHours], [showMinutes] or [showSeconds].
 * Hidden wheels always report 0. [itemFontSize] is the selected row size in
 * sp. [CenteredTimeInput] remains the keyboard alternative.
 */
@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    second: Int = 0,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onSecondChange: (Int) -> Unit = {},
    showHours: Boolean = true,
    showMinutes: Boolean = true,
    showSeconds: Boolean = false,
    itemFontSize: TextUnit = 32.sp,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // A hidden wheel has no meaning, so its value is always zero.
    LaunchedEffect(showHours) { if (!showHours) onHourChange(0) }
    LaunchedEffect(showMinutes) { if (!showMinutes) onMinuteChange(0) }
    LaunchedEffect(showSeconds) { if (!showSeconds) onSecondChange(0) }

    val density = LocalDensity.current
    val fontScale = density.fontScale.coerceAtLeast(0.01f)
    // At large font scales show three rows instead of five so the picker
    // still fits the dialog vertically.
    val visibleItems = if (fontScale > 1.5f) 3 else 5
    val centerOffset = visibleItems / 2

    val selectedFontSize = clampedSp(itemFontSize.value, 1.75f)
    val labelFontSize = clampedSp(itemFontSize.value * 0.32f, 1.75f)
    val itemHeight = with(density) { (selectedFontSize * 1.35f).toDp() }
    // +5 dp of horizontal room for the selected number and its pill.
    val desiredWidth = with(density) { (selectedFontSize * 1.6f).toDp() } + 5.dp
    val separatorWidth = 6.dp
    val highlightWidthPadding = 1.dp
    val highlightHeightPadding = 2.dp

    val showFirstSeparator = showHours && (showMinutes || showSeconds)
    val showSecondSeparator = showMinutes && showSeconds
    val visibleColumns = buildList {
        if (showHours) add(0)
        if (showMinutes) add(1)
        if (showSeconds) add(2)
    }
    val columnCount = visibleColumns.size.coerceAtLeast(1)
    val visibleSeparators = listOf(showFirstSeparator, showSecondSeparator).count { it }

    fun leftRadius(column: Int, largeRadius: Dp) =
        if (visibleColumns.first() == column) largeRadius else 8.dp

    fun rightRadius(column: Int, largeRadius: Dp) =
        if (visibleColumns.last() == column) largeRadius else 8.dp

    BoxWithConstraints(modifier = modifier.blockParentScroll()) {
        val availableForColumns = maxWidth - (separatorWidth * visibleSeparators)
        val columnWidth = if (maxWidth == Dp.Infinity) {
            desiredWidth
        } else {
            minOf(desiredWidth, availableForColumns / columnCount)
        }
        val highlightHeight = (itemHeight * 0.86f) + (highlightHeightPadding * 2)
        val largeRadius = highlightHeight / 2

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Short field labels above the wheels.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showHours) {
                    WheelLabel(stringResource(R.string.hours_label), columnWidth, labelFontSize)
                }
                if (showFirstSeparator) {
                    WheelGap(separatorWidth)
                }
                if (showMinutes) {
                    WheelLabel(stringResource(R.string.minutes_label), columnWidth, labelFontSize)
                }
                if (showSecondSeparator) {
                    WheelGap(separatorWidth)
                }
                if (showSeconds) {
                    WheelLabel(stringResource(R.string.seconds_label), columnWidth, labelFontSize)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showHours) {
                    WheelColumn(
                        value = hour,
                        range = 0..23,
                        onValueChange = onHourChange,
                        hapticsEnabled = hapticsEnabled,
                        selectedFontSize = selectedFontSize,
                        itemHeight = itemHeight,
                        visibleItems = visibleItems,
                        centerOffset = centerOffset,
                        width = columnWidth,
                        highlightWidthPadding = highlightWidthPadding,
                        highlightHeightPadding = highlightHeightPadding,
                        leftRadius = leftRadius(0, largeRadius),
                        rightRadius = rightRadius(0, largeRadius)
                    )
                }
                if (showFirstSeparator) {
                    WheelGap(separatorWidth)
                }
                if (showMinutes) {
                    WheelColumn(
                        value = minute,
                        range = 0..59,
                        onValueChange = onMinuteChange,
                        hapticsEnabled = hapticsEnabled,
                        selectedFontSize = selectedFontSize,
                        itemHeight = itemHeight,
                        visibleItems = visibleItems,
                        centerOffset = centerOffset,
                        width = columnWidth,
                        highlightWidthPadding = highlightWidthPadding,
                        highlightHeightPadding = highlightHeightPadding,
                        leftRadius = leftRadius(1, largeRadius),
                        rightRadius = rightRadius(1, largeRadius)
                    )
                }
                if (showSecondSeparator) {
                    WheelGap(separatorWidth)
                }
                if (showSeconds) {
                    WheelColumn(
                        value = second,
                        range = 0..59,
                        onValueChange = onSecondChange,
                        hapticsEnabled = hapticsEnabled,
                        selectedFontSize = selectedFontSize,
                        itemHeight = itemHeight,
                        visibleItems = visibleItems,
                        centerOffset = centerOffset,
                        width = columnWidth,
                        highlightWidthPadding = highlightWidthPadding,
                        highlightHeightPadding = highlightHeightPadding,
                        leftRadius = leftRadius(2, largeRadius),
                        rightRadius = rightRadius(2, largeRadius)
                    )
                }
            }
        }
    }
}

private const val WheelRepeats = 1000

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    hapticsEnabled: Boolean,
    selectedFontSize: TextUnit,
    itemHeight: Dp,
    visibleItems: Int,
    centerOffset: Int,
    width: Dp,
    highlightWidthPadding: Dp,
    highlightHeightPadding: Dp,
    leftRadius: Dp,
    rightRadius: Dp
) {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    var lastHapticAt by remember { mutableLongStateOf(0L) }
    val count = range.count()
    // A huge repeated list gives truly endless scrolling. We start in the
    // middle so neither end is reachable in practice.
    val itemCount = count * WheelRepeats
    val baseIndex = count * (WheelRepeats / 2)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex =
            (baseIndex + value - centerOffset).coerceAtLeast(0),
        initialFirstVisibleItemScrollOffset = 0
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centeredIndex by remember(range) {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter =
                (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { item ->
                abs(item.offset + item.size / 2 - viewportCenter)
            }?.index ?: (baseIndex + value)
        }
    }
    val centeredValue = floorMod(centeredIndex, count)

    // Signed distance of each visible item from the centre line. The item
    // style is interpolated from this value while the wheel moves, so the
    // weight/colour change is a transition instead of a discrete jump.
    val itemCenters by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.associate { item ->
                item.index to (item.offset + item.size / 2f - center)
            }
        }
    }

    // While the user scrolls, report the value that is on the centre line.
    LaunchedEffect(centeredValue) {
        if (centeredValue != value) {
            if (hapticsEnabled && listState.isScrollInProgress) {
                val now = SystemClock.uptimeMillis()
                // A fast fling crosses many values; keep the ticks light.
                if (now - lastHapticAt >= 40L) {
                    lastHapticAt = now
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            onValueChange(centeredValue)
        }
    }

    // Follow external changes without jumping back to the middle of the
    // repeated list: move to the nearest occurrence of the new value.
    LaunchedEffect(value) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val currentValue = floorMod(centeredIndex, count)
        if (currentValue == value) return@LaunchedEffect
        val delta = floorMod(value - currentValue, count)
        listState.scrollToItem(
            (centeredIndex + delta - centerOffset).coerceAtLeast(0)
        )
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // Each wheel gets its own segment of the selection pill: large outer
        // corners, small inner corners and a thin gap between columns. The
        // segment extends a little past the text area, giving the selected
        // value more breathing room.
        Box(
            modifier = Modifier
                .width(width + highlightWidthPadding * 2)
                .height((itemHeight * 0.86f) + highlightHeightPadding * 2)
                .clip(
                    RoundedCornerShape(
                        topStart = leftRadius,
                        topEnd = rightRadius,
                        bottomEnd = rightRadius,
                        bottomStart = leftRadius
                    )
                )
                .background(MaterialTheme.colorScheme.primary)
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(itemCount) { index ->
                val distancePx = itemCenters[index]
                val fraction = if (distancePx != null) {
                    (abs(distancePx) / itemHeightPx).coerceIn(0f, 2f)
                } else {
                    abs(index - centeredIndex).toFloat().coerceIn(0f, 2f)
                }
                val selectedness = (1f - fraction / 2f).coerceIn(0f, 1f)
                val scale = 0.68f + 0.32f * selectedness
                val alpha = 0.42f + 0.58f * selectedness
                val color = lerp(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.onPrimary,
                    selectedness
                ).copy(alpha = alpha)
                val weight = FontWeight((400f + 200f * selectedness).roundToInt())

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(java.util.Locale.getDefault(), "%02d", range.first + (index % count)),
                        color = color,
                        fontSize = selectedFontSize * scale,
                        fontWeight = weight,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelLabel(
    label: String,
    width: Dp,
    fontSize: TextUnit
) {
    Text(
        text = label,
        modifier = Modifier.width(width),
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

@Composable
private fun WheelGap(width: Dp) {
    Spacer(modifier = Modifier.width(width))
}

private fun floorMod(value: Int, modulus: Int): Int {
    val result = value % modulus
    return if (result < 0) result + modulus else result
}

/**
 * Swallows vertical nested-scroll leftovers from the wheels so the scrollable
 * setup page does not start moving when a wheel is flung. The wheel columns
 * still scroll normally: this connection only runs after a wheel has consumed
 * what it can, and it never intercepts pointer events.
 */
private val WheelNestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (available.y == 0f) return Offset.Zero
        return Offset(0f, available.y)
    }
}

private fun Modifier.blockParentScroll(): Modifier =
    nestedScroll(WheelNestedScrollConnection)
