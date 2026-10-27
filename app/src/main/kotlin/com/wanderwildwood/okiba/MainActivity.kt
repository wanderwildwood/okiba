package com.wanderwildwood.okiba

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.lifecycle.repeatOnLifecycle
import com.wanderwildwood.okiba.core.Age
import com.wanderwildwood.okiba.ui.SettingsScreen
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.okiba.ui.MainScreen
import com.wanderwildwood.okiba.ui.RationaleDialog
import com.wanderwildwood.okiba.ui.monochrome

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A steady cursor: a blinking one is a repaint twice a second on an e-ink panel
            // for as long as the note field has focus.
            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                ThemeMMD(colorScheme = monochrome) {
                    Parked()
                }
            }
        }
    }
}

@Composable
private fun Parked(vm: OkibaViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    // What the button was pressed for, held while the person reads why and answers Android's ask.
    var asking by remember { mutableStateOf<Job?>(null) }
    var rationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val job = asking
        asking = null
        if (job != null && granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) vm.start(job) else vm.denied()
    }

    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    // "4 min ago" is read when the app comes to the front, and again each time it would change
    // while the app is in front: once a minute, on the minute it turns, and never in the
    // background. One redraw a minute is what an e-ink panel can spare.
    val owner = LocalLifecycleOwner.current
    val anchors = listOfNotNull(state.history.shown?.timeMillis, state.meter?.endAt, state.meter?.warnAt)
    LaunchedEffect(owner, anchors) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                vm.refreshClock()
                delay(Age.nextTickMillis(System.currentTimeMillis(), anchors) + 50L)
            }
        }
    }

    fun press(job: Job) {
        if (vm.hasPermission) vm.start(job) else { asking = job; rationale = true }
    }

    if (settingsOpen) {
        BackHandler { settingsOpen = false }
        SettingsScreen(state = state, onBack = { settingsOpen = false }, onUnits = vm::setUnits, onWarn = vm::setWarn)
    } else {
        MainScreen(
            state = state,
            onMark = { press(Job.Mark) },
            onHowFar = { press(Job.Far) },
            onStop = vm::stop,
            onShow = vm::show,
            onForget = vm::forget,
            onUndo = vm::undoForget,
            onNote = vm::setNote,
            onSay = vm::say,
            onSettings = { settingsOpen = true },
            onStartMeter = vm::startMeter,
            onCancelMeter = vm::cancelMeter,
        )
    }

    if (rationale) {
        RationaleDialog(
            onAllow = {
                rationale = false
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onDismiss = { rationale = false; asking = null },
        )
    }
}
