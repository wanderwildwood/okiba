package com.wanderwildwood.okiba.meter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.okiba.MainActivity
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.data.Store
import com.wanderwildwood.okiba.ui.houseTypography
import com.wanderwildwood.okiba.ui.monochrome
import java.lang.ref.WeakReference
import com.wanderwildwood.okiba.ui.ArmedScope

/**
 * The ringing screen, over the lock screen: what rang, the time the meter ends, the note of the
 * spot, and Stop. Coming up is what turns the screen on, which on a Kompakt is what makes a
 * ring seen as well as heard. The sound is [RingService]'s, so this screen being closed, or
 * drawn again by Android, never ends a ring; only Stop does.
 */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        live = WeakReference(this)
        val store = Store(this)
        if (store.meter() == null) {
            finish()
            return
        }
        setContent {
            // Read again on each ring, so a screen left up for the warning says so when the end
            // rings. The activity is never recreated for it (0.2.9 did, and on the Kompakt the
            // ringing screen closed within a second).
            val changed by Ringing.changed.collectAsState()
            val m = remember(changed) { store.meter() }
            val note = remember(changed) { store.load().shown?.note.orEmpty() }
            ThemeMMD(colorScheme = monochrome, typography = houseTypography) {
                ArmedScope {
                    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                        if (m != null) {
                            val ring = Ringing.what(m)
                            Ring(Ringing.title(this, m, ring), Ringing.text(this, m, ring), note, onStop = {
                                Ringing.stop(this)
                                finish()
                            }, onOpen = {
                                Ringing.stop(this)
                                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                finish()
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (live?.get() === this) live = null
        super.onDestroy()
    }

    companion object {
        private var live: WeakReference<RingActivity>? = null

        fun intent(context: Context): Intent = Intent(context, RingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

        /**
         * Brings the screen up straight away, for a phone in use with this app in front, where
         * Android shows the full-screen intent only as a notification at the top.
         */
        fun show(context: Context) {
            try {
                context.startActivity(intent(context))
            } catch (e: Exception) {
                Log.w("okiba", "ringing screen not started directly: ${e.message}")
            }
        }

        fun closeAll() {
            live?.get()?.finish()
            live = null
        }
    }
}

@Composable
private fun Ring(title: String, text: String, note: String, onStop: () -> Unit, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        TextMMD(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        TextMMD(text = text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (note.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            TextMMD(text = note, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = onStop, modifier = Modifier.fillMaxWidth().height(72.dp)) {
            TextMMD(text = stringResource(R.string.ring_stop), style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            TextMMD(text = stringResource(R.string.ring_open), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
