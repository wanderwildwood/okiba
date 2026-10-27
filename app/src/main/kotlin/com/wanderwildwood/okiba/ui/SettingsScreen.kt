package com.wanderwildwood.okiba.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.UiState
import com.wanderwildwood.okiba.core.Units

/** The few settings, one row each, showing what they are set to. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onUnits: (Units) -> Unit,
) {
    var unitsOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.settings_title)) },
                navigationIcon = { BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
            )
        },
    ) { padding ->
        PagedColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item(key = "units") {
                SettingRow(stringResource(R.string.settings_units), unitsLabel(state.units, state.regionImperial)) { unitsOpen = true }
            }
        }
    }

    if (unitsOpen) {
        ChoiceDialog(
            title = stringResource(R.string.settings_units),
            choices = Units.entries.map { it to unitsLabel(it, state.regionImperial) },
            chosen = state.units,
            onChoose = { onUnits(it); unitsOpen = false },
            onDismiss = { unitsOpen = false },
        )
    }
}

@Composable
private fun unitsLabel(u: Units, regionImperial: Boolean): String = when (u) {
    Units.Auto -> stringResource(
        R.string.units_auto,
        stringResource(if (regionImperial) R.string.units_feet else R.string.units_metres),
    )
    Units.Metric -> stringResource(R.string.units_metres)
    Units.Imperial -> stringResource(R.string.units_feet)
}

/** A setting: its name, and under it what it is set to. The whole row takes the press. */
@Composable
fun SettingRow(title: String, value: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        TextMMD(text = title, style = MaterialTheme.typography.bodyMedium)
        if (value != null) TextMMD(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** One of a few, the chosen one in bold. A press chooses and closes. */
@Composable
fun <T> ChoiceDialog(title: String, choices: List<Pair<T, String>>, chosen: T, onChoose: (T) -> Unit, onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        for ((value, label) in choices) {
            TextMMD(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (value == chosen) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onChoose(value) }
                    .padding(vertical = 13.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        DialogButton(stringResource(R.string.cancel)) { onDismiss() }
    }
}
