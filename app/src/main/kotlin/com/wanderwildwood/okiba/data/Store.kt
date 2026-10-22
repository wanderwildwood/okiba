package com.wanderwildwood.okiba.data

import android.content.Context
import com.wanderwildwood.okiba.core.History

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

    private companion object {
        const val KEY = "history"
    }
}
