package com.olivka.clock.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.onSurfaceVariantDark

/** Small reset action placed inside a settings row. */
@Composable
fun ResetIconButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.reset),
            tint = onSurfaceVariantDark,
            modifier = Modifier.size(20.dp)
        )
    }
}
