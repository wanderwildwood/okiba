package com.wanderwildwood.okiba.ui

import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.core.Meter
import com.wanderwildwood.okiba.core.MeterChoice
import com.wanderwildwood.okiba.core.MeterMath
import com.wanderwildwood.okiba.data.Settings
import com.wanderwildwood.okiba.meter.Problem
import com.wanderwildwood.okiba.meter.Ringing
import com.wanderwildwood.okiba.meter.problems
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The parking meter: four ways to set it when none is running; while one is, when it ends, how
 * long is left and when it will warn, and a Cancel that asks in its own face. Under it, anything
 * that would stop it ringing.
 */
@Composable
fun MeterBlock(meter: Meter?, now: Long, warnMinutes: Int, onStart: (MeterChoice) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var lengthOpen by remember { mutableStateOf(false) }
    var endsAtOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        TextMMD(
            text = stringResource(R.string.meter_heading),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        when {
            meter == null -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (m in listOf(30, 60, 120)) {
                        OutlinedButtonMMD(onClick = { onStart(MeterChoice.Minutes(m)) }, modifier = Modifier.weight(1f).height(52.dp)) {
                            TextMMD(text = lengthText(m), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButtonMMD(onClick = { lengthOpen = true }, modifier = Modifier.weight(1f).height(52.dp)) {
                        TextMMD(text = stringResource(R.string.meter_other), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                    OutlinedButtonMMD(onClick = { endsAtOpen = true }, modifier = Modifier.weight(1f).height(52.dp)) {
                        TextMMD(text = stringResource(R.string.meter_ends_at_choice), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
            meter.isOver(now) -> {
                TextMMD(
                    text = stringResource(R.string.meter_ended, Ringing.clock(context, meter.endAt)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                // Nothing is lost by clearing a meter that has ended, so it does not ask.
                TextMMD(
                    text = stringResource(R.string.meter_clear),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onCancel).padding(vertical = 14.dp),
                )
            }
            else -> {
                TextMMD(
                    text = stringResource(R.string.meter_ends, Ringing.clock(context, meter.endAt)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextMMD(text = lengthLeft(meter.minutesLeft(now)), style = MaterialTheme.typography.bodyMedium)
                val w = meter.warnAt
                if (w != null && !meter.warned) {
                    TextMMD(text = stringResource(R.string.meter_warn_at, Ringing.clock(context, w)), style = MaterialTheme.typography.bodyMedium)
                }
                ArmedRow(
                    label = stringResource(R.string.meter_cancel),
                    armedLabel = stringResource(R.string.meter_cancel_armed),
                    onConfirmed = onCancel,
                )
            }
        }
        ProblemRows(now)
    }

    if (lengthOpen) {
        LengthDialog(onSet = { lengthOpen = false; onStart(MeterChoice.Minutes(it)) }, onDismiss = { lengthOpen = false })
    }
    if (endsAtOpen) {
        EndsAtDialog(now, onSet = { h, m -> endsAtOpen = false; onStart(MeterChoice.EndsAt(h, m)) }, onDismiss = { endsAtOpen = false })
    }
}

/** What would stop the meter ringing, each with the button that mends it. */
@Composable
private fun ProblemRows(now: Long) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var bump by remember { mutableIntStateOf(0) }
    // Read again each minute the screen ticks and after a press, so a mended one goes.
    val list = remember(now, bump) { problems(context, settings) { bump++ } }
    var failed by remember { mutableStateOf(false) }
    for (p in list) ProblemRow(p) {
        failed = runCatching { context.startActivity(p.open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isFailure
    }
    if (failed) TextMMD(text = stringResource(R.string.setup_cannot_open), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ProblemRow(p: Problem, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        TextMMD(text = stringResource(p.text), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        ButtonMMD(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            TextMMD(text = stringResource(p.button), style = MaterialTheme.typography.bodyMedium)
        }
        if (p.done != null && p.onDone != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButtonMMD(onClick = p.onDone, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                TextMMD(text = stringResource(p.done), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A length of the meter, chosen a minute or ten at a time. */
@Composable
private fun LengthDialog(onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(45) }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.meter_other_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        TextMMD(text = lengthText(minutes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Steps(
            listOf("−10" to -10, "−1" to -1, "+1" to 1, "+10" to 10),
        ) { minutes = (minutes + it).coerceIn(1, MeterMath.MAX_MINUTES) }
        Spacer(Modifier.height(14.dp))
        DialogButton(stringResource(R.string.meter_start), filled = true) { onSet(minutes) }
        DialogButton(stringResource(R.string.cancel)) { onDismiss() }
    }
}

/** The time printed on the ticket, chosen an hour or five minutes at a time. */
@Composable
private fun EndsAtDialog(now: Long, onSet: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // An hour from now, on the next five minutes: the commonest ticket.
    val start = remember {
        val t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime().plusHours(1)
        (t.hour * 60 + (t.minute + 4) / 5 * 5) % (24 * 60)
    }
    var at by remember { mutableIntStateOf(start) }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.meter_ends_at_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        val h = at / 60
        val m = at % 60
        val shown = DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", Locale.getDefault())
            .format(LocalTime.of(h, m))
        TextMMD(text = shown, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Steps(listOf("−1 h" to -60, "−5" to -5, "+5" to 5, "+1 h" to 60)) {
            at = Math.floorMod(at + it, 24 * 60)
        }
        Spacer(Modifier.height(14.dp))
        DialogButton(stringResource(R.string.meter_start), filled = true) { onSet(at / 60, at % 60) }
        DialogButton(stringResource(R.string.cancel)) { onDismiss() }
    }
}

@Composable
private fun Steps(steps: List<Pair<String, Int>>, onStep: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((label, d) in steps) {
            OutlinedButtonMMD(onClick = { onStep(d) }, modifier = Modifier.weight(1f).height(52.dp)) {
                TextMMD(text = label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun lengthText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> stringResource(R.string.length_min, m)
        m == 0 -> stringResource(R.string.length_h, h)
        else -> stringResource(R.string.length_h_min, h, m)
    }
}

@Composable
private fun lengthLeft(minutes: Long): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return when {
        h == 0 -> stringResource(R.string.left_min, m)
        m == 0 -> stringResource(R.string.left_h, h)
        else -> stringResource(R.string.left_h_min, h, m)
    }
}
