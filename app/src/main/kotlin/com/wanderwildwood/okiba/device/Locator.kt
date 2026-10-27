package com.wanderwildwood.okiba.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.wanderwildwood.okiba.core.BestFix

/** What a request for one position came to. */
sealed interface FixResult {
    data class Fix(val latitude: Double, val longitude: Double, val accuracyMeters: Float, val timeMillis: Long) : FixResult
    /** Precise location is not allowed: either refused, or only the approximate kind granted. */
    data object NotAllowed : FixResult
    /** The phone's location switch is off, or it has no GPS to ask. */
    data object Off : FixResult
    /** Asked, and nothing came back in time: indoors, under a roof, in a garage. */
    data object NoFix : FixResult
}

/**
 * A fresh position, on request, from the GPS alone.
 *
 * Nothing here listens in the background: the GPS is listened to from a press until a position
 * good to [BestFix.GOOD_ENOUGH_M] comes, or [BestFix.SETTLE_MS] after the first one, when the
 * best seen is kept; with nothing at all after [TIMEOUT_MS] it gives up. Then it stops
 * listening. The GPS provider is asked for by name, so the phone's network location service is
 * never involved and nothing leaves the phone.
 */
class Locator(private val context: Context) {

    private var listener: LocationListener? = null
    private val main = Handler(Looper.getMainLooper())
    private val lm get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Asks; [onResult] is called exactly once on the main thread unless [cancel] comes first. */
    fun request(onResult: (FixResult) -> Unit) {
        cancel()
        if (!hasPermission()) return onResult(FixResult.NotAllowed)
        val lm = lm
        if (!lm.isLocationEnabled || !lm.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            return onResult(FixResult.Off)
        }
        val picker = BestFix<FixResult.Fix>()
        var done = false
        lateinit var l: LocationListener
        val settle = Runnable { picker.best?.let { finish(l, it, onResult) } }
        val timeout = Runnable { finish(l, picker.best ?: FixResult.NoFix, onResult) }
        l = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (listener !== this || done) return
                val acc = if (loc.hasAccuracy()) loc.accuracy else 0f
                val fix = FixResult.Fix(loc.latitude, loc.longitude, acc, System.currentTimeMillis())
                val first = picker.settlesAt() == null
                if (picker.offer(fix, acc, SystemClock.elapsedRealtime())) {
                    done = true
                    finish(this, picker.best!!, onResult)
                } else if (first) {
                    main.postDelayed(settle, BestFix.SETTLE_MS)
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (listener === this && !done) {
                    done = true
                    finish(this, picker.best ?: FixResult.Off, onResult)
                }
            }

            override fun onProviderEnabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        listener = l
        pending = listOf(settle, timeout)
        main.postDelayed(timeout, TIMEOUT_MS)
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, l, Looper.getMainLooper())
        } catch (e: SecurityException) {
            finish(l, FixResult.NotAllowed, onResult)
        } catch (e: IllegalArgumentException) {
            finish(l, FixResult.Off, onResult)
        }
    }

    private var pending: List<Runnable> = emptyList()

    private fun finish(l: LocationListener, r: FixResult, onResult: (FixResult) -> Unit) {
        if (listener !== l) return
        cancel()
        onResult(r)
    }

    /** Stops listening. Anything still on its way is dropped. */
    fun cancel() {
        pending.forEach(main::removeCallbacks)
        pending = emptyList()
        listener?.let { runCatching { lm.removeUpdates(it) } }
        listener = null
    }

    companion object {
        const val TIMEOUT_MS = 45_000L
    }
}
