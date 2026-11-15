package com.olivka.clock.ui.theme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R

/**
 * Keyboard time input with two or three centred fields. A field starts editing
 * with the caret at the beginning, so typing replaces the leading digits; when
 * two digits are entered the focus moves to the next field automatically and
 * the last field dismisses the keyboard. The labels sit on the field border,
 * keeping the rounded left corner visible.
 *
 * When [second] and [onSecondChange] are provided a third field appears, which
 * is what the timer needs: a timer counts seconds, not just hours and minutes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenteredTimeInput(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    second: Int? = null,
    onSecondChange: ((Int) -> Unit)? = null,
    autoFocus: Boolean = false
) {
    val withSeconds = second != null && onSecondChange != null
    val fieldWidth = if (withSeconds) 64.dp else 96.dp
    val fieldHeight = if (withSeconds) 64.dp else 72.dp
    val fieldFontSize = clampedSp(if (withSeconds) 28f else 38f, 1.25f)
    val separatorWidth = if (withSeconds) 12.dp else 28.dp

    var hourValue by remember {
        mutableStateOf(TextFieldValue(hour.toString().padStart(2, '0')))
    }
    var minuteValue by remember {
        mutableStateOf(TextFieldValue(minute.toString().padStart(2, '0')))
    }
    var secondValue by remember {
        mutableStateOf(TextFieldValue((second ?: 0).toString().padStart(2, '0')))
    }
    var hourFresh by remember { mutableStateOf(false) }
    var minuteFresh by remember { mutableStateOf(false) }
    var secondFresh by remember { mutableStateOf(false) }
    val hourFocus = remember { FocusRequester() }
    val minuteFocus = remember { FocusRequester() }
    val secondFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var pendingFocus by remember { mutableStateOf<TimeFocusTarget?>(null) }

    // Never request focus directly from onValueChange: changing focus while
    // the text field is still applying an invalid/clamped value can leave the
    // IME stuck and all fields unresponsive. Let composition settle first.
    LaunchedEffect(pendingFocus) {
        val target = pendingFocus ?: return@LaunchedEffect
        pendingFocus = null
        when (target) {
            TimeFocusTarget.Hour -> hourFocus.requestFocus()
            TimeFocusTarget.Minute -> minuteFocus.requestFocus()
            TimeFocusTarget.Second -> secondFocus.requestFocus()
            TimeFocusTarget.Clear -> focusManager.clearFocus()
        }
    }
    LaunchedEffect(Unit) {
        if (autoFocus) hourFocus.requestFocus()
    }

    fun focusAfterMinute() {
        pendingFocus = if (withSeconds) {
            TimeFocusTarget.Second
        } else {
            TimeFocusTarget.Clear
        }
    }

    fun selectionFor(raw: TextFieldValue, text: String): TextRange =
        TextRange(raw.selection.start.coerceIn(0, text.length))

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            TimeInputField(
                value = hourValue,
                onValueChange = { raw ->
                    val currentText = hourValue.text
                    if (raw.text == currentText) {
                        // Selection-only change from a tap/focus. A fresh field
                        // always starts at the beginning; otherwise keep the caret.
                        hourValue = if (hourFresh) {
                            raw.copy(selection = TextRange(0))
                        } else {
                            raw
                        }
                    } else {
                        val digits = inputDigits(raw, currentText, hourFresh)
                        hourFresh = false
                        val parsed = digits.toIntOrNull()
                        when {
                            digits.isEmpty() ->
                                hourValue = raw.copy(text = "", selection = TextRange(0))

                            parsed != null && parsed > 23 -> {
                                hourValue = raw.copy(text = "23", selection = TextRange(2))
                                onHourChange(23)
                                pendingFocus = TimeFocusTarget.Minute
                            }

                            else -> {
                                hourValue = raw.copy(
                                    text = digits,
                                    selection = selectionFor(raw, digits)
                                )
                                parsed?.let(onHourChange)
                                if (digits.length == 2) {
                                    pendingFocus = TimeFocusTarget.Minute
                                }
                            }
                        }
                    }
                },
                onFocused = {
                    hourFresh = true
                    hourValue = hourValue.copy(selection = TextRange(0))
                },
                label = stringResource(R.string.hours_label),
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(
                    onNext = { pendingFocus = TimeFocusTarget.Minute }
                ),
                focusRequester = hourFocus,
                width = fieldWidth,
                height = fieldHeight,
                fontSize = fieldFontSize
            )

            TimeSeparator(width = separatorWidth, height = fieldHeight)

            TimeInputField(
                value = minuteValue,
                onValueChange = { raw ->
                    val currentText = minuteValue.text
                    if (raw.text == currentText) {
                        minuteValue = if (minuteFresh) {
                            raw.copy(selection = TextRange(0))
                        } else {
                            raw
                        }
                    } else {
                        val digits = inputDigits(raw, currentText, minuteFresh)
                        minuteFresh = false
                        val parsed = digits.toIntOrNull()
                        when {
                            digits.isEmpty() ->
                                minuteValue = raw.copy(text = "", selection = TextRange(0))

                            parsed != null && parsed > 59 -> {
                                minuteValue = raw.copy(text = "59", selection = TextRange(2))
                                onMinuteChange(59)
                                focusAfterMinute()
                            }

                            else -> {
                                minuteValue = raw.copy(
                                    text = digits,
                                    selection = selectionFor(raw, digits)
                                )
                                parsed?.let(onMinuteChange)
                                if (digits.length == 2) {
                                    focusAfterMinute()
                                }
                            }
                        }
                    }
                },
                onFocused = {
                    minuteFresh = true
                    minuteValue = minuteValue.copy(selection = TextRange(0))
                },
                label = stringResource(R.string.minutes_label),
                imeAction = if (withSeconds) ImeAction.Next else ImeAction.Done,
                keyboardActions = if (withSeconds) {
                    KeyboardActions(
                        onNext = { pendingFocus = TimeFocusTarget.Second }
                    )
                } else {
                    KeyboardActions(
                        onDone = { pendingFocus = TimeFocusTarget.Clear }
                    )
                },
                focusRequester = minuteFocus,
                width = fieldWidth,
                height = fieldHeight,
                fontSize = fieldFontSize
            )

            if (withSeconds) {
                TimeSeparator(width = separatorWidth, height = fieldHeight)

                TimeInputField(
                    value = secondValue,
                    onValueChange = { raw ->
                        val currentText = secondValue.text
                        if (raw.text == currentText) {
                            secondValue = if (secondFresh) {
                                raw.copy(selection = TextRange(0))
                            } else {
                                raw
                            }
                        } else {
                            val digits = inputDigits(raw, currentText, secondFresh)
                            secondFresh = false
                            val parsed = digits.toIntOrNull()
                            when {
                                digits.isEmpty() ->
                                    secondValue = raw.copy(text = "", selection = TextRange(0))

                                parsed != null && parsed > 59 -> {
                                    secondValue = raw.copy(text = "59", selection = TextRange(2))
                                    onSecondChange?.invoke(59)
                                    pendingFocus = TimeFocusTarget.Clear
                                }

                                else -> {
                                    secondValue = raw.copy(
                                        text = digits,
                                        selection = selectionFor(raw, digits)
                                    )
                                    parsed?.let { onSecondChange?.invoke(it) }
                                    if (digits.length == 2) {
                                        pendingFocus = TimeFocusTarget.Clear
                                    }
                                }
                            }
                        }
                    },
                    onFocused = {
                        secondFresh = true
                        secondValue = secondValue.copy(selection = TextRange(0))
                    },
                    label = stringResource(R.string.seconds_label),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = { pendingFocus = TimeFocusTarget.Clear }
                    ),
                    focusRequester = secondFocus,
                    width = fieldWidth,
                    height = fieldHeight,
                    fontSize = fieldFontSize
                )
            }
        }
    }
}

private enum class TimeFocusTarget {
    Hour,
    Minute,
    Second,
    Clear
}

/**
 * The first keystroke in a freshly focused field replaces the old value, so
 * the user can type 1-2-3-0 and get 12:30 instead of merging with "00".
 */
private fun inputDigits(
    raw: TextFieldValue,
    currentText: String,
    fresh: Boolean
): String {
    val added = raw.text.length - currentText.length
    val candidate = if (fresh && added > 0) raw.text.take(added) else raw.text
    return candidate.filter { it.isDigit() }.take(2)
}

@Composable
private fun TimeSeparator(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ":",
            fontSize = clampedSp(32f, 1.25f),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions,
    onFocused: () -> Unit,
    focusRequester: FocusRequester? = null,
    width: Dp = 96.dp,
    height: Dp = 72.dp,
    fontSize: TextUnit = 38.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary
    )
    val textStyle = LocalTextStyle.current.copy(
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        lineHeight = fontSize * 1.16f,
        color = MaterialTheme.colorScheme.onSurface
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(width)
            .height(height)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { state ->
                if (state.isFocused) onFocused()
            },
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value.text,
            visualTransformation = VisualTransformation.None,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            contentPadding = PaddingValues(horizontal = 10.dp),
            interactionSource = interactionSource,
            colors = colors,
            label = {
                Text(
                    text = label,
                    fontSize = clampedSp(12f, 1.25f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            container = {
                OutlinedTextFieldDefaults.ContainerBox(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    shape = RoundedCornerShape(8.dp),
                    colors = colors
                )
            }
        )
    }
}
