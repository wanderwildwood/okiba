package com.wanderwildwood.okiba

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
            ThemeMMD(colorScheme = monochrome) {
                Parked()
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

    // The age shown is read when the app comes to the front. Nothing ticks while it sits there.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshClock()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    fun press(job: Job) {
        if (vm.hasPermission) vm.start(job) else { asking = job; rationale = true }
    }

    MainScreen(
        state = state,
        onMark = { press(Job.Mark) },
        onHowFar = { press(Job.Far) },
        onStop = vm::stop,
        onShow = vm::show,
        onForget = vm::forget,
        onNote = vm::setNote,
    )

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
