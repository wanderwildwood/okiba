package com.wanderwildwood.okiba.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.delay

/** An icon in a top bar, with a target a thumb can find. */
@Composable
fun BarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}


/**
 * A press that asks once, in its own face: the first press arms it and changes what it says,
 * the second does it. It disarms itself after six seconds, so a stray press does not leave
 * a live trigger for whoever picks the phone up next.
 */
@Composable
fun rememberArmed(): Armed {
    val a = remember { Armed() }
    LaunchedEffect(a.on) {
        if (!a.on) return@LaunchedEffect
        delay(6000)
        a.on = false
    }
    return a
}

class Armed {
    var on by mutableStateOf(false)

    /** Arms on the first press, and runs [action] on the second. */
    fun press(action: () -> Unit) {
        if (on) {
            on = false
            action()
        } else {
            on = true
        }
    }
}


@Composable
fun ArmedRow(label: String, armedLabel: String, onConfirmed: () -> Unit) {
    val armed = rememberArmed()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { armed.press(onConfirmed) }
            .padding(vertical = 14.dp),
    ) {
        TextMMD(
            text = if (armed.on) armedLabel else label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (armed.on) FontWeight.Bold else FontWeight.Normal,
        )
    }
}


/** A full-width button inside a dialog. */
@Composable
fun DialogButton(text: String, filled: Boolean = false, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(48.dp)
    if (filled) {
        ButtonMMD(onClick = onClick, modifier = modifier) { TextMMD(text = text, style = MaterialTheme.typography.bodyMedium) }
    } else {
        OutlinedButtonMMD(onClick = onClick, modifier = modifier) { TextMMD(text = text, style = MaterialTheme.typography.bodyMedium) }
    }
}

