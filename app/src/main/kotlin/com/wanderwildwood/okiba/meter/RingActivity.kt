package com.wanderwildwood.okiba.meter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.wanderwildwood.okiba.core.MeterRing
import com.wanderwildwood.okiba.data.Store
import com.wanderwildwood.okiba.ui.monochrome
import java.lang.ref.WeakReference

/**
 * The ringing screen, over the lock screen: what rang, the time the meter ends, the note of the
 * spot, and Stop. Coming up is what turns the screen on, which on a Kompakt is what makes a
 * ring seen as well as heard.
 */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        live = WeakReference(this)
        val store = Store(this)
        val m = store.meter()
        if (m == null) {
            finish()
            return
        }
        val ring = if (m.isOver(System.currentTimeMillis())) MeterRing.End else MeterRing.Warn
        val title = Ringing.title(this, m, ring)
        val text = Ringing.text(this, m, ring)
        val note = store.load().shown?.note.orEmpty()
        setContent {
            ThemeMMD(colorScheme = monochrome) {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                    Ring(title, text, note, onStop = {
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

    /** Already up for the warning when the end rings: drawn again, so it says what rang now. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recreate()
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
         * Brings the screen up straight away. With the phone locked or asleep the notification's
         * full-screen intent does it anyway; this is for a phone in use.
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
