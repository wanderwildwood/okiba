package com.wanderwildwood.okiba.device

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * The country the phone is in, for choosing feet or metres: the SIM's country first, then the
 * mobile network's, then the language setting's. A phone set to US English in Poland is common,
 * and its SIM knows better than its language. None of these needs a permission.
 */
object Region {
    fun country(context: Context): String {
        val tm = runCatching { context.getSystemService(TelephonyManager::class.java) }.getOrNull()
        return listOfNotNull(
            runCatching { tm?.simCountryIso }.getOrNull(),
            runCatching { tm?.networkCountryIso }.getOrNull(),
            Locale.getDefault().country,
        ).map { it.trim().uppercase(Locale.ROOT) }.firstOrNull { it.length == 2 } ?: ""
    }
}
