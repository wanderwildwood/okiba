package com.wanderwildwood.okiba.core

/** How long ago, in the units a person says it in. */
sealed interface Age {
    data object JustNow : Age
    data class Minutes(val m: Int) : Age
    data class Hours(val h: Int, val m: Int) : Age
    data class Days(val d: Int, val h: Int) : Age

    companion object {
        /** A clock set back past the mark is treated as just now rather than as a negative. */
        fun of(elapsedMillis: Long): Age {
            val minutes = (elapsedMillis / 60_000L).coerceAtLeast(0)
            return when {
                minutes < 1 -> JustNow
                minutes < 60 -> Minutes(minutes.toInt())
                minutes < 24 * 60 -> Hours((minutes / 60).toInt(), (minutes % 60).toInt())
                else -> Days((minutes / (24 * 60)).toInt(), ((minutes / 60) % 24).toInt())
            }
        }

        /**
         * How long until the next of [anchors] crosses a whole minute from [now], so that a line
         * counted from it ("4 min ago", "12 min left") changes then and the screen is redrawn
         * once a minute, not more. With no anchors, the next minute of the clock.
         */
        fun nextTickMillis(now: Long, anchors: List<Long>): Long {
            val all = anchors.ifEmpty { listOf(0L) }
            return all.minOf { 60_000L - Math.floorMod(now - it, 60_000L) }
        }
    }
}
