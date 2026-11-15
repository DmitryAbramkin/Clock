package com.olivka.clock.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R

/**
 * One fixed header for every secondary screen: round back button, title on
 * the left, optional actions. Screens no longer mix scrolling and pinned
 * headers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            AutoSizeText(
                text = title,
                maxFontSize = 20.sp,
                minFontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                color = onSurfaceDark,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                contentAlignment = androidx.compose.ui.Alignment.CenterStart
            )
        },
        navigationIcon = {
            if (onBack != null) {
                RoundBackButton(onClick = onBack)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundDark,
            titleContentColor = onSurfaceDark
        )
    )
}

@Composable
fun RoundBackButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = primaryContainerDark,
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
            .size(40.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = onPrimaryContainerDark,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Scaffold with the shared header and safe top/horizontal/bottom insets.
 * Use for screens that need custom content (grids, lazy lists).
 */
@Composable
fun ClockScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scaffold: @Composable () -> Unit = {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars.only(
                WindowInsetsSides.Horizontal +
                    WindowInsetsSides.Top +
                    WindowInsetsSides.Bottom
            ),
            topBar = { ClockTopBar(title = title, onBack = onBack, actions = actions) },
            containerColor = backgroundDark,
            content = content
        )
    }
    if (onBack != null) {
        PredictiveBackContainer(onBack = onBack) {
            scaffold()
        }
    } else {
        scaffold()
    }
}

/**
 * Standard scrolling secondary page. Handles the system back gesture, the
 * fixed header and the bottom inset so content never slides under the
 * gesture bar.
 */
@Composable
fun ClockPage(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    ClockScaffold(title = title, onBack = onBack, actions = actions) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            content = content
        )
    }
}

/** Consistent section label used by Settings and its sub-pages. */
@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = onSurfaceVariantDark,
        fontWeight = FontWeight.SemiBold,
        fontSize = clampedSp(14f, 1.5f),
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
    )
}
