package com.wanderwildwood.okiba.meter

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.app.NotificationManagerCompat
import com.wanderwildwood.okiba.R
import com.wanderwildwood.okiba.data.Settings

/** Something standing between the meter's ring and the person it is for, and what to do about it. */
data class Problem(val text: Int, val button: Int, val open: Intent, val done: Int? = null, val onDone: (() -> Unit)? = null)

/**
 * What would stop the meter ringing, or make it ring late, read fresh each time the screen comes
 * back: only what is wrong is shown, and when nothing is, nothing is. Clock's list, cut to what
 * a meter needs.
 */
fun problems(context: Context, settings: Settings, onChange: () -> Unit): List<Problem> {
    val out = mutableListOf<Problem>()
    // A stop by the system since the last look takes back the word that DuraSpeed was dealt with.
    DuraSpeed.newStop(context, settings)
    if (DuraSpeed.atRisk(context, settings)) {
        out += Problem(
            text = R.string.setup_duraspeed,
            button = R.string.setup_duraspeed_open,
            open = DuraSpeed.appInfo(),
            done = R.string.setup_duraspeed_done,
            onDone = {
                settings.duraSpeedAllowed = true
                onChange()
            },
        )
    }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        out += Problem(
            text = R.string.setup_notifications,
            button = R.string.setup_turn_on,
            open = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
    if (!context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
        out += Problem(
            text = R.string.setup_exact,
            button = R.string.setup_allow,
            open = Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)),
        )
    }
    // Android 14 and later let a sideloaded app take the screen only once it is allowed to.
    if (Build.VERSION.SDK_INT >= 34 && !context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
        out += Problem(
            text = R.string.setup_full_screen,
            button = R.string.setup_allow,
            open = Intent(AndroidSettings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + context.packageName)),
        )
    }
    return out
}
