package com.olivka.clock.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R

/**
 * A large centered time with a small pencil pinned to the top-right corner
 * of the text itself. The pencil is absolutely positioned from the measured
 * text bounds, so it never shifts the time away from the centre.
 */
@Composable
fun EditableTimeText(
    time: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 12.sp,
    color: Color = onSurfaceDark,
    fontWeight: FontWeight? = null
) {
    var textSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.clickable(onClick = onEdit),
        contentAlignment = Alignment.Center
    ) {
        AutoSizeText(
            text = time,
            maxFontSize = maxFontSize,
            minFontSize = minFontSize,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            color = color,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            onTextLayout = { textSize = it.size }
        )

        if (textSize.width > 0 && textSize.height > 0) {
            // 32 dp touch target with an 18 dp icon: seven dp of padding on
            // each side. The visible icon's top-left corner is 4 dp to the
            // right of the text and level with the top of the text layout.
            val dx = with(density) {
                (textSize.width / 2f + 13.dp.toPx()).toDp()
            }
            val dy = with(density) {
                // The text layout box includes leading above the visible
                // glyphs; lower the pencil just enough to sit on the visual
                // top line of the digits.
                (-textSize.height / 2f + 9.dp.toPx() + 12.dp.toPx()).toDp()
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = dx, y = dy)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit_time),
                    tint = onSurfaceVariantDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
