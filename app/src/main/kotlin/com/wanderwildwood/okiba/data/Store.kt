package com.wanderwildwood.okiba.data

import android.content.Context
import com.wanderwildwood.okiba.core.History
import com.wanderwildwood.okiba.core.Meter

/**
 * The few spots, as one string in the app's private preferences. Excluded from backup and
 * device transfer (see the manifest), so it lives and dies with the install.
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("okiba", Context.MODE_PRIVATE)

    fun load(): History = History.decode(prefs.getString(KEY, null))

    fun save(h: History) {
        prefs.edit().putString(KEY, History.encode(h)).apply()
    }

    fun meter(): Meter? = Meter.decode(prefs.getString(METER, null))

    /** Written at once (commit, not apply): an alarm receiver may read it the next moment, in another process start. */
    fun saveMeter(m: Meter?) {
        prefs.edit().apply { if (m == null) remove(METER) else putString(METER, m.encode()) }.commit()
    }

    private companion object {
        const val KEY = "history"
        const val METER = "meter"
    }
}
