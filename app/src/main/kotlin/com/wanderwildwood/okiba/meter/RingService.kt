package com.wanderwildwood.okiba.meter

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.wanderwildwood.okiba.data.Store

/**
 * Plays the meter's ring until Stop, or until [Ringing.RING_MS] goes by: the phone's alarm sound
 * in a loop on the alarm stream and a buzz, with the ringing notification held up as this
 * service's own, so nothing but Stop takes it away. Time's RingService, cut down to one sound.
 *
 * During a call it rings quietly, at a tenth of its volume, so the call is not drowned.
 */
class RingService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var focus: AudioFocusRequest? = null
    private var wake: PowerManager.WakeLock? = null
    private var detached = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val m = Store(this).meter()
        if (m == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == QUIETEN) {
            // Nobody answered: the sound ends, the notification is left up, quietly.
            stopSound()
            @Suppress("DEPRECATION")
            val quiet = intent.getParcelableExtra<Notification>(EXTRA_NOTICE)
            if (quiet != null) getSystemService(NotificationManager::class.java).notify(Ringing.ID, quiet)
            stopForeground(STOP_FOREGROUND_DETACH)
            detached = true
            stopSelf()
            return START_NOT_STICKY
        }
        Ringing.channels(this)
        startForeground(Ringing.ID, Ringing.loud(this, m, Ringing.what(m)), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        running = true
        if (wake == null) {
            wake = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "okiba:ringing")
                .apply { acquire(Ringing.RING_MS + 60_000L) }
        }
        // The warning still ringing when the end comes: the notification above says so, and the
        // sound goes on as it was.
        if (player == null && vibrator == null) startSound()
        return START_NOT_STICKY
    }

    private fun startSound() {
        val audio = getSystemService(AudioManager::class.java)
        val inCall = audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .build()
            .also { audio.requestAudioFocus(it) }
        player = candidates().firstNotNullOfOrNull { open(it, attrs) }
        val p = player
        if (p == null) {
            Log.e("okiba", "no alarm sound could be played")
        } else {
            if (inCall) p.setVolume(0.1f, 0.1f)
            p.start()
            Log.i("okiba", "ringing${if (inCall) " quietly, in a call" else ""}")
        }
        vibrator = getSystemService(Vibrator::class.java).also {
            @Suppress("DEPRECATION")
            it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600, 1400), 0), attrs)
        }
    }

    /** The phone's alarm sound, then its default alarm, ringtone and notification sounds. */
    private fun candidates(): List<Uri> = listOfNotNull(
        RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
    ).distinct()

    private fun open(uri: Uri, attrs: AudioAttributes): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(attrs)
            setDataSource(this@RingService, uri)
            isLooping = true
            setWakeMode(this@RingService, PowerManager.PARTIAL_WAKE_LOCK)
            prepare()
        }
    } catch (e: Exception) {
        Log.w("okiba", "cannot play $uri: ${e.message}")
        null
    }

    private fun stopSound() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }

    override fun onDestroy() {
        running = false
        stopSound()
        wake?.let { if (it.isHeld) it.release() }
        wake = null
        if (!detached) stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val QUIETEN = "com.wanderwildwood.okiba.RING_QUIETEN"
        private const val EXTRA_NOTICE = "notice"

        @Volatile
        var running = false
            private set

        /** Starts the ring, or says it could not be started from where this was called. */
        fun start(context: Context): Boolean = try {
            context.startForegroundService(Intent(context, RingService::class.java))
            true
        } catch (e: Exception) {
            Log.w("okiba", "ringing service not started: ${e.message}")
            false
        }

        /** Ends the sound and leaves [notice] in the ringing notification's place. */
        fun quieten(context: Context, notice: Notification) {
            try {
                context.startService(Intent(context, RingService::class.java).setAction(QUIETEN).putExtra(EXTRA_NOTICE, notice))
            } catch (e: Exception) {
                stop(context)
                context.getSystemService(NotificationManager::class.java).notify(Ringing.ID, notice)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RingService::class.java))
        }
    }
}
