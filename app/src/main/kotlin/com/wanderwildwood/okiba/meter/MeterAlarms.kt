package com.wanderwildwood.okiba.meter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wanderwildwood.okiba.core.Meter
import com.wanderwildwood.okiba.core.MeterChoice
import com.wanderwildwood.okiba.core.MeterMath
import com.wanderwildwood.okiba.core.MeterRing
import com.wanderwildwood.okiba.data.Settings
import com.wanderwildwood.okiba.data.Store
import java.time.ZoneId

/**
 * The meter's alarms: one exact alarm at a time, for whatever rings next, set again after each
 * ring and after the phone restarts. Everything is read from [Store], so a receiver started cold
 * by the system knows as much as the screen does. Clock's timer engine, cut down to one timer.
 */
object MeterAlarms {
    private const val TAG = "okiba"

    fun start(context: Context, choice: MeterChoice): Meter {
        val m = MeterMath.start(choice, Settings(context).warnMinutes, System.currentTimeMillis(), ZoneId.systemDefault())
        Store(context).saveMeter(m)
        Ringing.cancel(context)
        schedule(context)
        return m
    }

    /** Stops the meter: no more rings, and whatever is ringing goes quiet. */
    fun cancel(context: Context) {
        Store(context).saveMeter(null)
        Ringing.cancel(context)
        schedule(context)
    }

    /** An alarm went off, or the phone has just started: ring what is due, then set the next. */
    fun fire(context: Context) {
        val store = Store(context)
        val now = System.currentTimeMillis()
        val rung = MeterMath.ring(store.meter(), now)
        if (rung != null) {
            val (m, ring) = rung
            store.saveMeter(m)
            Log.i(TAG, "meter ${ring.name.lowercase()} at $now (ends ${m.endAt})")
            Ringing.ring(context, m, ring)
        }
        schedule(context)
    }

    /** Sets the one alarm for what rings next, or clears it when nothing does. */
    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context)
        val now = System.currentTimeMillis()
        val due = MeterMath.next(Store(context).meter(), now)
        if (due == null) {
            am.cancel(pi)
            Log.i(TAG, "no meter alarm to set")
            return
        }
        val at = maxOf(due.at, now + 500L)
        // Exact, and let through Doze: a meter that rings ten minutes late has not rung.
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            Log.w(TAG, "no exact alarms allowed; inexact alarm set")
        }
        Log.i(TAG, "meter ${due.ring.name.lowercase()} alarm set for $at")
    }

    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, MeterReceiver::class.java).setAction(MeterReceiver.FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** The meter's alarm going off, and Stop pressed on its notification. */
class MeterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FIRE -> MeterAlarms.fire(context)
            STOP -> Ringing.stop(context)
            Ringing.SILENCE -> Ringing.silence(context)
        }
    }

    companion object {
        const val FIRE = "com.wanderwildwood.okiba.METER"
        const val STOP = "com.wanderwildwood.okiba.METER_STOP"
    }
}

/**
 * Everything that clears or moves an alarm: the phone starting up, this app being updated, the
 * clock or its time zone being changed, the permission for exact alarms being given back. Each
 * sets the meter's alarm again from what is stored, and a ring whose time passed meanwhile rings
 * now.
 */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("okiba", "system: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> MeterAlarms.fire(context)
            Intent.ACTION_LOCALE_CHANGED -> Ringing.channels(context)
        }
    }
}
