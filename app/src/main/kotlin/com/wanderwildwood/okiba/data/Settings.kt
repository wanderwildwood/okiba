package com.wanderwildwood.okiba.data

import android.content.Context
import com.wanderwildwood.okiba.core.Units

/** The few choices a person makes once, in the app's private preferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var units: Units
        get() = Units.of(prefs.getString("units", null))
        set(v) { prefs.edit().putString("units", v.name).apply() }
}
