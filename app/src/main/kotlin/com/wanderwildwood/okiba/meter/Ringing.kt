package com.wanderwildwood.okiba.meter

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wanderwildwood.okiba.MainActivity
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.core.Meter
import com.wanderwildwood.okiba.core.MeterRing
import com.wanderwildwood.okiba.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How the meter rings: [RingService] plays the phone's alarm sound in a loop on the alarm volume
 * (so a phone set to silent still rings) and buzzes, and holds up a notification whose full-screen
 * intent brings the ringing screen over the lock screen, which is what lights a Kompakt's panel.
 * Both the warning and the end ring until Stop is pressed, or for [RING_MS], after which the
 * notification stays, quietly. Clock's ring, cut down to one meter.
 *
 * Only Stop ends a ring. The ringing notification cannot be swiped or tapped away, and the
 * ringing screen being closed, or drawn again by the system, leaves the sound going.
 */
object Ringing {
    /** Kept for a ring without the service: the notification makes the sound itself. */
    private const val RING = "meter_ring"
    /** The service's notification: the service makes the sound, so the channel has none. */
    const val RINGING = "meter_ringing"
    private const val QUIET = "meter_quiet"
    const val ID = 1
    const val RING_MS = 2 * 60_000L

    /** Whether the app's own screen is in front, where only a direct start brings the ringing screen up. */
    @Volatile
    var appInFront = false

    private val _changed = MutableStateFlow(0)

    /** Moves on each ring and each Stop, so a ringing screen that is up says what rings now. */
    val changed: StateFlow<Int> = _changed

    fun channels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(
            NotificationChannel(RINGING, context.getString(R.string.channel_ring), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_ring_about)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(RING, context.getString(R.string.channel_ring_fallback), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_ring_fallback_about)
                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(QUIET, context.getString(R.string.channel_quiet), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    /** What a meter is ringing for: its end once that has come, its warning before. */
    fun what(m: Meter): MeterRing = if (m.isOver(System.currentTimeMillis())) MeterRing.End else MeterRing.Warn

    /**
     * Rings [ring] for meter [m]. One that was due more than a couple of minutes ago (the phone
     * was off at the time) is only shown, quietly: a meter that ended an hour ago is news, not
     * an alarm.
     */
    fun ring(context: Context, m: Meter, ring: MeterRing) {
        channels(context)
        val due = if (ring == MeterRing.End) m.endAt else (m.warnAt ?: m.endAt)
        if (System.currentTimeMillis() - due > RING_MS) {
            RingService.stop(context)
            post(context, quiet(context, m, ring))
            _changed.value++
            return
        }
        if (!RingService.start(context)) {
            // Android would not let the service start from here: the notification rings instead,
            // over and over until it is answered.
            post(context, insistent(context, m, ring))
        }
        _changed.value++
        // A phone asleep or locked is woken by the full-screen intent, which brings the ringing
        // screen up; starting it here as well would be a start from the background, which
        // Android blocks. A phone in use is shown only a notification at the top, so there the
        // screen is brought up by hand: always allowed with this app in front, and tried with
        // another app in front, where Android may refuse it and the notification rings on.
        if (appInFront || inUse(context)) RingActivity.show(context)
        // The ring goes on; this quietens it if nobody answers.
        // Exact: an ordinary alarm was seen to come over a minute late.
        val am = context.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + RING_MS
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, action(context, SILENCE))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, action(context, SILENCE))
    }

    /** The screen on and unlocked: someone is using the phone. */
    private fun inUse(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isInteractive &&
            !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    /** Stop pressed: the sound ends, the notification goes, and so does the ringing screen. */
    fun stop(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(action(context, SILENCE))
        RingService.stop(context)
        context.getSystemService(NotificationManager::class.java).cancel(ID)
        RingActivity.closeAll()
        _changed.value++
    }

    /** Nobody answered: the sound ends, and the notification stays, quietly. */
    fun silence(context: Context) {
        val m = Store(context).meter() ?: return stop(context)
        val n = quiet(context, m, what(m))
        if (RingService.running) {
            RingService.quieten(context, n)
        } else if (context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == ID }) {
            post(context, n)
        }
    }

    /**
     * The ringing notification was taken away by something other than Stop: swiped (Android 14
     * and later allow it even for this one) or cleared by another app. It goes back up, since
     * only Stop ends a ring.
     */
    fun repost(context: Context) {
        val m = Store(context).meter() ?: return
        post(context, if (RingService.running) loud(context, m, what(m)) else insistent(context, m, what(m)))
    }

    /** The ring without the service: the notification sounds itself, over and over until answered. */
    private fun insistent(context: Context, m: Meter, ring: MeterRing): Notification =
        loud(context, m, ring, RING).apply { flags = flags or Notification.FLAG_INSISTENT }

    /** A new meter or a cancelled one: nothing of the old one rings on. */
    fun cancel(context: Context) = stop(context)

    fun title(context: Context, m: Meter, ring: MeterRing): String = when (ring) {
        MeterRing.Warn -> context.getString(R.string.ring_warn_title, m.minutesLeft(System.currentTimeMillis()))
        MeterRing.End -> context.getString(R.string.ring_end_title)
    }

    fun text(context: Context, m: Meter, ring: MeterRing): String =
        context.getString(if (ring == MeterRing.End) R.string.ring_end_text else R.string.ring_warn_text, clock(context, m.endAt))

    fun clock(context: Context, millis: Long): String =
        DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", Locale.getDefault())
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    /**
     * The ringing notification: it stays until Stop (not swiped away, not taken away by a tap),
     * and a tap on it, like its full-screen intent, brings up the ringing screen.
     */
    fun loud(context: Context, m: Meter, ring: MeterRing, channel: String = RINGING): Notification =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title(context, m, ring))
            .setContentText(text(context, m, ring))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setContentIntent(screen(context))
            .setFullScreenIntent(screen(context), true)
            .setDeleteIntent(action(context, MeterReceiver.REPOST))
            .addAction(0, context.getString(R.string.ring_stop), action(context, MeterReceiver.STOP))
            .build()

    /** What stays after a ring nobody answered, or one that was due while the phone was off. */
    private fun quiet(context: Context, m: Meter, ring: MeterRing): Notification =
        NotificationCompat.Builder(context, QUIET)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title(context, m, ring))
            .setContentText(text(context, m, ring))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setContentIntent(open(context))
            .setDeleteIntent(action(context, MeterReceiver.STOP))
            .addAction(0, context.getString(R.string.ring_stop), action(context, MeterReceiver.STOP))
            .build()

    private fun post(context: Context, n: Notification) {
        try {
            context.getSystemService(NotificationManager::class.java).notify(ID, n)
        } catch (e: SecurityException) {
            Log.w("okiba", "meter notification not posted: ${e.message}")
        }
    }

    const val SILENCE = "com.wanderwildwood.okiba.METER_SILENCE"

    private fun action(context: Context, what: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        what.hashCode(),
        Intent(context, MeterReceiver::class.java).setAction(what),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun open(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun screen(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        1,
        RingActivity.intent(context),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
