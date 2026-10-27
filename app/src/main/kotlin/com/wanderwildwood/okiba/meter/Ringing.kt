package com.wanderwildwood.okiba.meter

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.wanderwildwood.okiba.MainActivity
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.core.Meter
import com.wanderwildwood.okiba.core.MeterRing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How the meter rings: a notification with the phone's alarm sound and a buzz, on the alarm
 * volume so a phone set to silent still rings, and the ringing screen brought up over the lock
 * screen, which is what lights a Kompakt's panel. The warning sounds once; the end goes on
 * sounding until Stop is pressed, or for [RING_MS], after which it stays as a quiet notification.
 * Clock's ring, without its service: one sound from the notification is enough for a meter.
 */
object Ringing {
    private const val RING = "meter_ring"
    private const val QUIET = "meter_quiet"
    private const val ID = 1
    const val RING_MS = 2 * 60_000L

    fun channels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(
            NotificationChannel(RING, context.getString(R.string.channel_ring), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_ring_about)
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

    /**
     * Rings [ring] for meter [m]. One that was due more than a couple of minutes ago (the phone
     * was off at the time) is only shown, quietly: a meter that ended an hour ago is news, not
     * an alarm.
     */
    fun ring(context: Context, m: Meter, ring: MeterRing) {
        channels(context)
        val due = if (ring == MeterRing.End) m.endAt else (m.warnAt ?: m.endAt)
        val late = System.currentTimeMillis() - due > RING_MS
        post(context, m, ring, loud = !late)
        if (late) return
        RingActivity.show(context)
        if (ring == MeterRing.End) {
            // The end goes on sounding; this quietens it if nobody answers.
            // Exact: an ordinary alarm was seen to come over a minute late.
            val am = context.getSystemService(AlarmManager::class.java)
            val at = System.currentTimeMillis() + RING_MS
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, action(context, SILENCE))
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, action(context, SILENCE))
        }
    }

    /** Stop pressed: the sound ends and the notification goes. */
    fun stop(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(action(context, SILENCE))
        context.getSystemService(NotificationManager::class.java).cancel(ID)
        RingActivity.closeAll()
    }

    /** Nobody answered: the sound ends, and the notification stays, quietly. */
    fun silence(context: Context) {
        val m = com.wanderwildwood.okiba.data.Store(context).meter() ?: return stop(context)
        if (context.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id == ID }) return
        post(context, m, MeterRing.End, loud = false)
    }

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

    private fun post(context: Context, m: Meter, ring: MeterRing, loud: Boolean) {
        val b = NotificationCompat.Builder(context, if (loud) RING else QUIET)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title(context, m, ring))
            .setContentText(text(context, m, ring))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (loud) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setShowWhen(false)
            .setContentIntent(open(context))
            .setDeleteIntent(action(context, MeterReceiver.STOP))
            .addAction(0, context.getString(R.string.ring_stop), action(context, MeterReceiver.STOP))
        if (loud) b.setFullScreenIntent(screen(context), true)
        val n = b.build()
        if (loud && ring == MeterRing.End) n.flags = n.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(ID, n)
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
