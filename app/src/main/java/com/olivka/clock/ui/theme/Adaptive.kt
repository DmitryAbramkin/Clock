package com.olivka.clock.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Large display text (clock digits, countdowns) is capped so it cannot grow
 * past a readable size and overlap neighbouring elements. Body text keeps
 * the full system font scale.
 */
private const val MaxDisplayFontScale = 1.35f

/** Spacing that follows text growth is scaled a little, but within limits. */
private const val MaxSpacingFontScale = 1.25f

/**
 * Returns a size in sp that renders as [value]sp multiplied by the system
 * font scale limited to [maxScale]. Useful for titles and navigation labels
 * that live inside fixed-height containers.
 */
@Composable
fun clampedSp(value: Float, maxScale: Float = MaxDisplayFontScale): TextUnit {
    val fontScale = LocalDensity.current.fontScale
    if (fontScale <= 0f) return value.sp
    val effective = fontScale.coerceAtMost(maxScale)
    return (value * effective / fontScale).sp
}

/**
 * Grows a spacing value with the system font scale within [minScale]..[maxScale].
 */
@Composable
fun adaptiveDp(
    value: Dp,
    maxScale: Float = MaxSpacingFontScale,
    minScale: Float = 1f
): Dp {
    val fontScale = LocalDensity.current.fontScale
    return (value.value * fontScale.coerceIn(minScale, maxScale)).dp
}

/**
 * Text that picks the largest font size between [minFontSize] and
 * [maxFontSize] that fits the available width (and height, when bounded).
 *
 * The largest allowed size is additionally capped by [MaxDisplayFontScale]
 * so a 72sp timer does not become 144sp at a 200% font scale. Long strings
 * such as "01:24:58" shrink until they fit, instead of overflowing the ring
 * or overlapping adjacent content.
 */
@Composable
fun AutoSizeText(
    text: String,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 12.sp,
    maxLines: Int = 1,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.Center,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val fontScale = density.fontScale.coerceAtLeast(0.01f)
    val cappedMax = (
        maxFontSize.value * min(fontScale, MaxDisplayFontScale) / fontScale
        ).sp
    val cappedMin = (
        minFontSize.value * min(fontScale, MaxDisplayFontScale) / fontScale
        ).coerceAtMost(cappedMax.value).sp

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        val maxWidthPx = constraints.maxWidth
        val maxHeightPx = constraints.maxHeight

        val fittedSize = if (maxWidthPx == Constraints.Infinity) {
            cappedMax
        } else {
            remember(
                text,
                maxWidthPx,
                maxHeightPx,
                cappedMax,
                cappedMin,
                maxLines,
                style,
                fontWeight,
                density
            ) {
                val baseStyle = style.copy(
                    fontSize = cappedMax,
                    fontWeight = fontWeight ?: style.fontWeight
                )
                fun fits(size: TextUnit): Boolean {
                    val singleLine = maxLines == 1
                    val layout = measurer.measure(
                        text = AnnotatedString(text),
                        style = baseStyle.copy(fontSize = size),
                        maxLines = maxLines,
                        softWrap = !singleLine,
                        overflow = TextOverflow.Clip,
                        constraints = if (singleLine) {
                            // Measure the natural width: didOverflowWidth is not
                            // reliable for non-wrapping single-line text.
                            Constraints()
                        } else {
                            Constraints(
                                maxWidth = maxWidthPx,
                                maxHeight = if (maxHeightPx == Constraints.Infinity) {
                                    Constraints.Infinity
                                } else {
                                    maxHeightPx
                                }
                            )
                        },
                        density = density
                    )
                    val widthFits = layout.size.width <= maxWidthPx
                    val heightFits = maxHeightPx == Constraints.Infinity ||
                        layout.size.height <= maxHeightPx
                    return widthFits &&
                        heightFits &&
                        (singleLine || (!layout.didOverflowWidth && !layout.didOverflowHeight))
                }

                if (fits(cappedMax)) {
                    cappedMax
                } else {
                    var low = cappedMin.value
                    var high = cappedMax.value
                    var best = low
                    var step = 0
                    while (step < 12 && high - low > 0.5f) {
                        val mid = (low + high) / 2f
                        if (fits(mid.sp)) {
                            best = mid
                            low = mid
                        } else {
                            high = mid
                        }
                        step++
                    }
                    best.sp
                }
            }
        }

        Text(
            text = text,
            modifier = Modifier,
            color = color,
            fontSize = fittedSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            softWrap = maxLines > 1,
            style = style,
            onTextLayout = { result -> onTextLayout?.invoke(result) }
        )
    }
}
