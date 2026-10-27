package com.wanderwildwood.okiba.core

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * A parking meter running: when it was set, when it ends, when to warn before it does, and what
 * has rung so far. Times are wall-clock milliseconds, so the alarm rings at the same instant
 * whatever happens to the clock's time zone or summer time in between.
 */
data class Meter(
    val setAt: Long,
    val endAt: Long,
    /** When the warning rings, or null for none. */
    val warnAt: Long?,
    val warned: Boolean = false,
    val ended: Boolean = false,
) {
    /** Whether the end has come, by the flag a ring set or by the clock. */
    fun isOver(now: Long): Boolean = ended || now >= endAt

    /** Whole minutes left, counted up: 30 s left is "1 min left", never "0". */
    fun minutesLeft(now: Long): Long = ((endAt - now).coerceAtLeast(0L) + 59_999L) / 60_000L

    fun encode(): String = listOf(setAt, endAt, warnAt ?: "", if (warned) 1 else 0, if (ended) 1 else 0).joinToString("\t")

    companion object {
        fun decode(text: String?): Meter? {
            val f = text?.split('\t') ?: return null
            if (f.size < 5) return null
            val setAt = f[0].toLongOrNull() ?: return null
            val endAt = f[1].toLongOrNull() ?: return null
            return Meter(setAt, endAt, f[2].toLongOrNull(), f[3] == "1", f[4] == "1")
        }
    }
}

/** How long the meter was paid for: a length of time, or the time printed on the ticket. */
sealed interface MeterChoice {
    data class Minutes(val minutes: Int) : MeterChoice
    data class EndsAt(val hour: Int, val minute: Int) : MeterChoice
}

/** What rings next, and when. */
enum class MeterRing { Warn, End }

data class Due(val ring: MeterRing, val at: Long)

object MeterMath {
    /** The longest meter the app will set: a day. */
    const val MAX_MINUTES = 24 * 60

    /**
     * When a meter set at [now] ends. A length is counted in real time, so an hour across the
     * change to or from summer time is still sixty minutes. A time on the ticket is the next time
     * the clock reads it: later today, or tomorrow if it has passed (a ticket bought at 23:50
     * that ends at 00:20). A time the clock skips in spring is moved on by the length of the gap
     * (02:30 becomes 03:30), and a time it reads twice in autumn is the first of the two.
     */
    fun endFor(choice: MeterChoice, now: Long, zone: ZoneId): Long = when (choice) {
        is MeterChoice.Minutes -> now + choice.minutes.coerceIn(1, MAX_MINUTES) * 60_000L
        is MeterChoice.EndsAt -> {
            val here = Instant.ofEpochMilli(now).atZone(zone)
            val time = LocalTime.of(choice.hour, choice.minute)
            var at = ZonedDateTime.of(here.toLocalDate(), time, zone)
            if (!at.toInstant().isAfter(here.toInstant())) at = ZonedDateTime.of(here.toLocalDate().plusDays(1), time, zone)
            at.toInstant().toEpochMilli()
        }
    }

    /**
     * When to warn, [warnMinutes] before [end]; null with the warning off, or when that moment is
     * already past at [now] (a ten-minute meter with a fifteen-minute warning rings only at its end).
     */
    fun warnAt(end: Long, warnMinutes: Int, now: Long): Long? {
        if (warnMinutes <= 0) return null
        val at = end - warnMinutes * 60_000L
        return if (at > now) at else null
    }

    fun start(choice: MeterChoice, warnMinutes: Int, now: Long, zone: ZoneId): Meter {
        val end = endFor(choice, now, zone)
        return Meter(setAt = now, endAt = end, warnAt = warnAt(end, warnMinutes, now))
    }

    /**
     * What is still to ring, and when to set the alarm for it: the warning, then the end. Used
     * after every ring and after the phone restarts. A ring whose time passed while the phone was
     * off is due at once; a warning whose end has also passed is skipped, since the end says more.
     */
    fun next(m: Meter?, now: Long): Due? {
        if (m == null || m.ended) return null
        val w = m.warnAt
        if (w != null && !m.warned && now < m.endAt) return Due(MeterRing.Warn, maxOf(w, now))
        return Due(MeterRing.End, maxOf(m.endAt, now))
    }

    /**
     * The meter after an alarm went off at [now], and which ring to sound, or null when nothing
     * was due (an alarm that came early, or one left over from a meter since cancelled).
     */
    fun ring(m: Meter?, now: Long): Pair<Meter, MeterRing>? {
        if (m == null || m.ended) return null
        if (now >= m.endAt) return m.copy(warned = true, ended = true) to MeterRing.End
        val w = m.warnAt ?: return null
        if (!m.warned && now >= w) return m.copy(warned = true) to MeterRing.Warn
        return null
    }
}
