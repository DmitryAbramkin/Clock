package com.olivka.clock.ui

import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivka.clock.R
import com.olivka.clock.ui.theme.ClockScaffold

private data class LicenseEntry(
    val id: String,
    val title: String,
    val copyright: String,
    val license: String,
    @RawRes val textRes: Int
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val entries = listOf(
        LicenseEntry(
            id = "clock",
            title = stringResource(R.string.license_clock_title),
            copyright = stringResource(R.string.license_clock_copyright),
            license = stringResource(R.string.license_mit),
            textRes = R.raw.license_clock
        ),
        LicenseEntry(
            id = "mascot",
            title = stringResource(R.string.license_mascot_title),
            copyright = stringResource(R.string.license_mascot_copyright),
            license = stringResource(R.string.license_mit),
            textRes = R.raw.license_mascot
        ),
        LicenseEntry(
            id = "material-clock",
            title = stringResource(R.string.license_material_clock_title),
            copyright = stringResource(R.string.license_material_clock_copyright),
            license = stringResource(R.string.license_mit),
            textRes = R.raw.license_material_clock
        ),
        LicenseEntry(
            id = "androidx",
            title = stringResource(R.string.license_androidx_title),
            copyright = stringResource(R.string.license_androidx_copyright),
            license = stringResource(R.string.license_apache_2_0),
            textRes = R.raw.license_apache_2_0
        ),
        LicenseEntry(
            id = "kotlin",
            title = stringResource(R.string.license_kotlin_title),
            copyright = stringResource(R.string.license_kotlin_copyright),
            license = stringResource(R.string.license_apache_2_0),
            textRes = R.raw.license_apache_2_0
        )
    )

    ClockScaffold(
        title = stringResource(R.string.licenses_title),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.licenses_intro),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
            items(entries, key = { it.id }) { entry ->
                LicenseCard(entry)
            }
        }
    }
}

@Composable
private fun LicenseCard(entry: LicenseEntry) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = entry.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.copyright,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.license,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (expanded) R.string.license_hide else R.string.license_show
                ),
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            )
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                val context = LocalContext.current
                val text = remember(entry.textRes) {
                    context.resources.openRawResource(entry.textRes)
                        .bufferedReader()
                        .use { it.readText() }
                }
                SelectionContainer {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
