package com.wanderwildwood.okiba.data

import android.content.Context
import com.wanderwildwood.okiba.core.Units

/** The few choices a person makes once, in the app's private preferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var units: Units
        get() = Units.of(prefs.getString("units", null))
        set(v) { prefs.edit().putString("units", v.name).apply() }

    /** Minutes before a meter ends to warn; 0 for no warning. */
    var warnMinutes: Int
        get() = prefs.getInt("warn", DEFAULT_WARN)
        set(v) { prefs.edit().putInt("warn", v).apply() }

    /** The person's word that Parked is switched on in DuraSpeed's list. */
    var duraSpeedAllowed: Boolean
        get() = prefs.getBoolean("duraspeed", false)
        set(v) { prefs.edit().putBoolean("duraspeed", v).apply() }

    /** The newest stop by the system already looked at, so each is counted once. */
    var stopSeen: Long
        get() = prefs.getLong("stop_seen", 0L)
        set(v) { prefs.edit().putLong("stop_seen", v).apply() }

    companion object {
        const val DEFAULT_WARN = 5
        val WARN_CHOICES = listOf(0, 1, 2, 5, 10, 15)
    }
}
