package com.wanderwildwood.okiba.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

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
 * One fresh position, on request, from the GPS alone.
 *
 * Nothing here listens in the background: a single `getCurrentLocation` call per press, which
 * gives up after [TIMEOUT_MS]. The GPS provider is asked for by name, so the phone's network
 * location service is never involved and nothing leaves the phone.
 */
class Locator(private val context: Context) {

    private var signal: CancellationSignal? = null
    private var timeout: Runnable? = null
    private val main = Handler(Looper.getMainLooper())

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Asks once; [onResult] is called exactly once on the main thread unless [cancel] comes first. */
    fun request(onResult: (FixResult) -> Unit) {
        cancel()
        if (!hasPermission()) return onResult(FixResult.NotAllowed)
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isLocationEnabled || !lm.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            return onResult(FixResult.Off)
        }
        val s = CancellationSignal()
        signal = s
        var done = false
        fun finish(r: FixResult) {
            if (done) return
            done = true
            timeout?.let(main::removeCallbacks)
            timeout = null
            signal = null
            onResult(r)
        }
        val t = Runnable { s.cancel() }
        timeout = t
        main.postDelayed(t, TIMEOUT_MS)
        try {
            lm.getCurrentLocation(LocationManager.GPS_PROVIDER, s, ContextCompat.getMainExecutor(context)) { loc: Location? ->
                if (signal !== s) return@getCurrentLocation  // stopped by the person, or superseded
                finish(
                    if (loc == null) FixResult.NoFix
                    else FixResult.Fix(loc.latitude, loc.longitude, if (loc.hasAccuracy()) loc.accuracy else 0f, System.currentTimeMillis()),
                )
            }
        } catch (e: SecurityException) {
            finish(FixResult.NotAllowed)
        } catch (e: IllegalArgumentException) {
            finish(FixResult.Off)
        }
    }

    /** Stops waiting. The pending callback, if it still arrives, is dropped. */
    fun cancel() {
        timeout?.let(main::removeCallbacks)
        timeout = null
        signal?.cancel()
        signal = null
    }

    companion object {
        const val TIMEOUT_MS = 45_000L
    }
}
