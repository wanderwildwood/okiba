package com.wanderwildwood.okiba.core

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Eight points of the compass, clockwise from north. */
enum class Compass { N, NE, E, SE, S, SW, W, NW }

object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance in metres (haversine). Good to well under a metre at street scale. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Initial bearing from the first point to the second, degrees clockwise from north, 0 until 360. */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun compass(bearing: Double): Compass {
        val b = ((bearing % 360.0) + 360.0) % 360.0
        return Compass.entries[((b + 22.5) / 45.0).toInt() % 8]
    }

    /** Whether the person is, for all the phone can tell, standing on the spot. */
    fun isWithinMargin(distanceM: Double, accuracyHereM: Double, accuracySpotM: Double): Boolean =
        distanceM <= max(accuracyHereM + accuracySpotM, MIN_MARGIN_M)

    /** Below this, no phone's fix says anything about which side of a car you are on. */
    const val MIN_MARGIN_M = 10.0

    /**
     * A distance a person can read at a glance: whole metres under a kilometre, one decimal
     * above; or feet and miles for a locale that uses them. Rounded to what the fix can
     * claim, never to a false precision.
     */
    fun formatDistance(meters: Double, imperial: Boolean): String {
        if (imperial) {
            val feet = meters * 3.28084
            return if (feet < 1000) "${roundTo(feet, if (feet < 100) 5 else 10)} ft"
            else String.format(Locale.US, "%.1f mi", meters / 1609.344)
        }
        return if (meters < 1000) "${roundTo(meters, if (meters < 100) 5 else 10)} m"
        else String.format(Locale.US, "%.1f km", meters / 1000.0)
    }

    private fun roundTo(value: Double, step: Int): Int =
        max(step, (value / step).roundToInt() * step)

    /** Plain decimal degrees, five places (about a metre), pasteable into any map. */
    fun formatCoordinates(lat: Double, lon: Double): String =
        String.format(Locale.US, "%.5f, %.5f", lat, lon)

    fun formatAccuracy(meters: Float, imperial: Boolean): String =
        formatDistance(meters.toDouble(), imperial)

    /** Countries that still measure a street in feet. */
    fun usesImperial(country: String): Boolean = country in setOf("US", "LR", "MM")
}
