package com.wanderwildwood.okiba.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.okiba.Far
import com.wanderwildwood.okiba.Job
import com.wanderwildwood.okiba.Notice
import com.wanderwildwood.okiba.Say
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.UiState
import com.wanderwildwood.okiba.core.Age
import com.wanderwildwood.okiba.core.Geo
import com.wanderwildwood.okiba.core.MeterChoice
import com.wanderwildwood.okiba.core.NOTE_MAX
import com.wanderwildwood.okiba.core.Spot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The whole app: one screen, one button, the spot it is holding, and the few before it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState,
    onMark: () -> Unit,
    onHowFar: () -> Unit,
    onStop: () -> Unit,
    onShow: (Spot) -> Unit,
    onForget: (Spot) -> Unit,
    onUndo: () -> Unit,
    onNote: (Spot, String) -> Unit,
    onSay: (Say?) -> Unit,
    onSettings: () -> Unit,
    onStartMeter: (MeterChoice) -> Unit,
    onCancelMeter: () -> Unit,
) {
    val context = LocalContext.current
    var aboutOpen by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<Spot?>(null) }
    val shown = state.history.shown
    val imperial = state.imperial
    // Read again when the configuration changes, so a clock format changed in Android's settings shows at once.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val is24 = remember(configuration) { DateFormat.is24HourFormat(context) }

    // Showing a different spot, or marking one, starts the screen over from its top.
    val list = rememberLazyListState()
    LaunchedEffect(shown?.timeMillis) { list.scrollToItem(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.app_name)) },
                actions = {
                    BarButton(Icons.Settings, stringResource(R.string.cd_settings), onSettings)
                    BarButton(Icons.Info, stringResource(R.string.cd_about)) { aboutOpen = true }
                },
            )
        },
    ) { padding ->
        PagedColumn(
            state = list,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 12.dp),
        ) {
            item(key = "mark") {
                val working = state.working
                ButtonMMD(
                    onClick = { if (working == null) onMark() else onStop() },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(72.dp),
                ) {
                    TextMMD(
                        text = stringResource(
                            when (working) {
                                null -> R.string.mark
                                else -> R.string.finding_stop
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            state.notice?.let { notice ->
                item(key = "notice") {
                    TextMMD(
                        text = stringResource(
                            when (notice) {
                                Notice.NoFix -> R.string.notice_no_fix
                                Notice.LocationOff -> R.string.notice_off
                                Notice.NeedPrecise -> R.string.notice_precise
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (notice != Notice.NoFix) {
                    // A permission refused for good has no other way back than Android's settings.
                    item(key = "notice-settings") {
                        TextMMD(
                            text = stringResource(R.string.open_app_settings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openAppSettings(context) }
                                .heightIn(min = 48.dp)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                }
            }

            if (state.undo != null) {
                item(key = "undo") {
                    TextMMD(
                        text = stringResource(R.string.forgotten_undo),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(onClick = onUndo)
                            .padding(top = 14.dp, bottom = 10.dp),
                    )
                }
            }

            if (shown == null) {
                item(key = "empty") {
                    TextMMD(
                        text = stringResource(R.string.empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
                item(key = "meter") { MeterBlock(state.meter, state.now, state.warnMinutes, onStartMeter, onCancelMeter) }
            } else {
                item(key = "spot") {
                    SpotBlock(shown, state.now, is24, imperial) { noteFor = shown }
                }
                item(key = "far") {
                    FarBlock(
                        working = state.working == Job.Far,
                        far = state.far?.takeIf { it.spotTime == shown.timeMillis },
                        imperial = imperial,
                        onHowFar = { if (state.working == null) onHowFar() else onStop() },
                    )
                }
                item(key = "actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val label = stringResource(R.string.spot_label)
                        val whenText = whenText(shown.timeMillis, is24)
                        OutlinedButtonMMD(
                            onClick = { onSay(if (openInMaps(context, shown, label)) null else Say.NoMaps) },
                            modifier = Modifier.weight(3f).height(52.dp),
                        ) { TextMMD(text = stringResource(R.string.open_in_maps), style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                        OutlinedButtonMMD(
                            onClick = { onSay(if (share(context, shown, label, whenText)) null else Say.NoShare) },
                            modifier = Modifier.weight(2f).height(52.dp),
                        ) { TextMMD(text = stringResource(R.string.share), style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                    }
                }
                state.say?.let { say ->
                    item(key = "say") {
                        TextMMD(
                            text = stringResource(
                                when (say) {
                                    Say.NoMaps -> R.string.no_maps
                                    Say.NoShare -> R.string.no_share
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
                item(key = "meter") { MeterBlock(state.meter, state.now, state.warnMinutes, onStartMeter, onCancelMeter) }
                item(key = "forget-${shown.timeMillis}") {
                    ArmedRow(
                        label = stringResource(R.string.forget),
                        armedLabel = stringResource(R.string.forget_armed),
                        onConfirmed = { onForget(shown) },
                    )
                }
                val others = state.history.spots.filter { it.timeMillis != shown.timeMillis }
                if (others.isNotEmpty()) {
                    item(key = "earlier") {
                        TextMMD(
                            text = stringResource(R.string.earlier),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                        )
                    }
                    items(others.size, key = { others[it].timeMillis }) { i ->
                        val s = others[i]
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onShow(s) }.padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            TextMMD(
                                text = ageText(Age.of(state.now - s.timeMillis)) + " · " + whenText(s.timeMillis, is24),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (s.note.isNotBlank()) {
                                TextMMD(text = s.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (aboutOpen) AboutDialog { aboutOpen = false }
    noteFor?.let { spot ->
        NoteDialog(spot.note, onSave = { onNote(spot, it); noteFor = null }, onDismiss = { noteFor = null })
    }
}

@Composable
private fun SpotBlock(spot: Spot, now: Long, is24: Boolean, imperial: Boolean, onNote: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        TextMMD(
            text = ageText(Age.of(now - spot.timeMillis)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        TextMMD(text = whenText(spot.timeMillis, is24), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        TextMMD(text = Geo.formatCoordinates(spot.latitude, spot.longitude), style = MaterialTheme.typography.bodyMedium)
        if (spot.accuracyMeters > 0f) {
            TextMMD(
                text = stringResource(R.string.good_to, Geo.formatAccuracy(spot.accuracyMeters, imperial)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Column(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onNote).padding(top = 10.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (spot.note.isBlank()) {
                TextMMD(text = stringResource(R.string.add_note), style = MaterialTheme.typography.bodyMedium)
            } else {
                TextMMD(text = spot.note, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun FarBlock(working: Boolean, far: Far?, imperial: Boolean, onHowFar: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedButtonMMD(onClick = onHowFar, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            TextMMD(
                text = stringResource(if (working) R.string.finding_stop else R.string.how_far),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (far != null && !working) {
            Spacer(Modifier.height(10.dp))
            val acc = Geo.formatAccuracy(far.accuracyMeters, imperial)
            if (far.atTheSpot) {
                TextMMD(text = stringResource(R.string.far_here), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            } else {
                val dir = stringArrayResource(R.array.compass)[far.compass.ordinal]
                TextMMD(
                    text = stringResource(R.string.far_result, Geo.formatDistance(far.meters, imperial), dir),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (far.accuracyMeters > 0f) {
                TextMMD(text = stringResource(R.string.far_accuracy, acc), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NoteDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.note_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        TextFieldMMD(
            value = text,
            onValueChange = { text = it.take(NOTE_MAX) },
            singleLine = true,
            placeholder = { TextMMD(text = stringResource(R.string.note_hint), style = MaterialTheme.typography.labelSmall) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().textActions(),
        )
        Spacer(Modifier.height(14.dp))
        DialogButton(stringResource(R.string.note_save), filled = true) { onSave(text) }
        DialogButton(stringResource(R.string.cancel)) { onDismiss() }
    }
}

/** Why the location is asked for, said before Android asks. */
@Composable
fun RationaleDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = stringResource(R.string.rationale_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        TextMMD(text = stringResource(R.string.rationale), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        DialogButton(stringResource(R.string.rationale_go), filled = true) { onAllow() }
        DialogButton(stringResource(R.string.cancel)) { onDismiss() }
    }
}

@Composable
private fun ageText(age: Age): String = when (age) {
    Age.JustNow -> stringResource(R.string.age_now)
    is Age.Minutes -> stringResource(R.string.age_min, age.m)
    is Age.Hours -> if (age.m == 0) stringResource(R.string.age_h, age.h) else stringResource(R.string.age_h_min, age.h, age.m)
    is Age.Days -> if (age.h == 0) stringResource(R.string.age_d, age.d) else stringResource(R.string.age_d_h, age.d, age.h)
}

private fun whenText(millis: Long, is24: Boolean): String {
    // The phone's own order of day, month and time for its language, with the 12 or 24 hour
    // clock the reader chose, not an English pattern for everyone.
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, if (is24) "EEEdMMMHm" else "EEEdMMMhm")
    return DateTimeFormatter.ofPattern(pattern, locale)
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

/** Hands the spot to a maps app; false when the phone has none, for the screen to say so. */
private fun openInMaps(context: android.content.Context, spot: Spot, label: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spot.geoUri(label))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}

private fun share(context: android.content.Context, spot: Spot, label: String, whenText: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, spot.shareText(label, whenText))
    return runCatching {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
