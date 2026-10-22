package com.wanderwildwood.okiba.core

/** One place the car was left: where, how sure, when, and an optional line to find it by. */
data class Spot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timeMillis: Long,
    val note: String = "",
) {
    /** A `geo:` address any maps app can take; the label rides in the q parameter. */
    fun geoUri(label: String): String {
        val c = coordinates()
        return "geo:$c?q=$c(${label.encodeLabel()})"
    }

    /** What the share sheet carries: a line a person can read, a geo link, and a web map link. */
    fun shareText(label: String, whenText: String): String {
        val c = coordinates()
        val sb = StringBuilder()
        sb.append(label).append(' ').append(whenText).append('\n')
        if (note.isNotBlank()) sb.append(note).append('\n')
        sb.append(geoUri(label)).append('\n')
        sb.append("https://www.openstreetmap.org/?mlat=").append(latString())
            .append("&mlon=").append(lonString())
            .append("#map=19/").append(latString()).append('/').append(lonString())
        return sb.toString()
    }

    private fun latString() = String.format(java.util.Locale.US, "%.6f", latitude)
    private fun lonString() = String.format(java.util.Locale.US, "%.6f", longitude)
    private fun coordinates() = "${latString()},${lonString()}"

    private fun String.encodeLabel(): String =
        replace("%", "%25").replace("(", "%28").replace(")", "%29").replace(" ", "%20")
            .replace("&", "%26").replace("#", "%23")
}

/** The last few spots, newest first, and which one is on show. Pure, so it can be tested. */
data class History(val spots: List<Spot> = emptyList(), val shownTime: Long? = null) {

    val shown: Spot?
        get() = spots.firstOrNull { it.timeMillis == shownTime } ?: spots.firstOrNull()

    /** A new spot goes on top and is shown; the oldest beyond [MAX] fall off. */
    fun marked(spot: Spot): History =
        History((listOf(spot) + spots.filter { it.timeMillis != spot.timeMillis }).take(MAX), spot.timeMillis)

    fun show(spot: Spot): History = if (spots.any { it.timeMillis == spot.timeMillis }) copy(shownTime = spot.timeMillis) else this

    /** Forgets one spot. If it was the one on show, the newest remaining takes over. */
    fun forgotten(spot: Spot): History {
        val left = spots.filter { it.timeMillis != spot.timeMillis }
        val keep = if (shownTime == spot.timeMillis) left.firstOrNull()?.timeMillis else shownTime
        return History(left, keep)
    }

    fun withNote(spot: Spot, note: String): History =
        copy(spots = spots.map { if (it.timeMillis == spot.timeMillis) it.copy(note = note.cleanNote()) else it })

    companion object {
        const val MAX = 5

        // Plain text, one spot a line, tab between fields. A note has its tabs and line
        // breaks turned to spaces on the way in, so no escaping is needed.
        fun encode(h: History): String = buildString {
            append(h.shownTime ?: "").append('\n')
            for (s in h.spots) {
                append(s.latitude).append('\t').append(s.longitude).append('\t')
                    .append(s.accuracyMeters).append('\t').append(s.timeMillis).append('\t')
                    .append(s.note.cleanNote()).append('\n')
            }
        }

        fun decode(text: String?): History {
            if (text.isNullOrBlank()) return History()
            val lines = text.split('\n')
            val shown = lines.firstOrNull()?.toLongOrNull()
            val spots = lines.drop(1).mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 4) return@mapNotNull null
                val lat = f[0].toDoubleOrNull() ?: return@mapNotNull null
                val lon = f[1].toDoubleOrNull() ?: return@mapNotNull null
                val acc = f[2].toFloatOrNull() ?: return@mapNotNull null
                val t = f[3].toLongOrNull() ?: return@mapNotNull null
                Spot(lat, lon, acc, t, f.getOrElse(4) { "" })
            }.take(MAX)
            return History(spots, shown)
        }
    }
}

const val NOTE_MAX = 60

fun String.cleanNote(): String =
    replace(Regex("[\\t\\r\\n]+"), " ").trim().take(NOTE_MAX)
