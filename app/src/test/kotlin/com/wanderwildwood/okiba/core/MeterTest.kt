package com.wanderwildwood.okiba.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

// Times are made up; zones are real so summer time is real.
class MeterTest {
    private val ny = ZoneId.of("America/New_York")
    private val utc = ZoneId.of("UTC")
    private val min = 60_000L

    private fun at(zone: ZoneId, y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi), zone).toInstant().toEpochMilli()

    private fun local(zone: ZoneId, millis: Long): LocalDateTime =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    @Test fun aLengthIsCountedInRealMinutes() {
        val now = at(utc, 2026, 10, 27, 10, 0)
        assertEquals(now + 30 * min, MeterMath.endFor(MeterChoice.Minutes(30), now, utc))
        assertEquals(now + 120 * min, MeterMath.endFor(MeterChoice.Minutes(120), now, utc))
    }

    @Test fun aLengthIsKeptWithinADay() {
        val now = 0L
        assertEquals(min, MeterMath.endFor(MeterChoice.Minutes(0), now, utc))
        assertEquals(24 * 60 * min, MeterMath.endFor(MeterChoice.Minutes(5000), now, utc))
    }

    @Test fun aTicketTimeLaterTodayIsToday() {
        val now = at(ny, 2026, 10, 27, 14, 10)
        assertEquals(at(ny, 2026, 10, 27, 16, 30), MeterMath.endFor(MeterChoice.EndsAt(16, 30), now, ny))
    }

    @Test fun aTicketTimeAlreadyPassedIsTomorrowAcrossMidnight() {
        val now = at(ny, 2026, 10, 27, 23, 50)
        val end = MeterMath.endFor(MeterChoice.EndsAt(0, 20), now, ny)
        assertEquals(LocalDateTime.of(2026, 10, 28, 0, 20), local(ny, end))
        assertEquals(30 * min, end - now)
        // The very minute it is now is not "now": it is tomorrow.
        val same = MeterMath.endFor(MeterChoice.EndsAt(23, 50), now, ny)
        assertEquals(24 * 60 * min, same - now)
    }

    @Test fun anHourAcrossTheSpringChangeIsSixtyMinutes() {
        // 2026-03-08: New York's clocks go from 02:00 to 03:00.
        val now = at(ny, 2026, 3, 8, 1, 30)
        val end = MeterMath.endFor(MeterChoice.Minutes(60), now, ny)
        assertEquals(60 * min, end - now)
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), local(ny, end))
    }

    @Test fun anHourAcrossTheAutumnChangeIsSixtyMinutes() {
        // 2026-11-01: New York's clocks go from 02:00 back to 01:00.
        val now = at(ny, 2026, 11, 1, 0, 45)
        val end = MeterMath.endFor(MeterChoice.Minutes(60), now, ny)
        assertEquals(60 * min, end - now)
        assertEquals(LocalDateTime.of(2026, 11, 1, 1, 45), local(ny, end))
    }

    @Test fun aTicketTimeTheSpringChangeSkipsMovesOnByTheGap() {
        val now = at(ny, 2026, 3, 8, 1, 0)
        val end = MeterMath.endFor(MeterChoice.EndsAt(2, 30), now, ny)
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), local(ny, end))
        assertEquals(90 * min, end - now)
    }

    @Test fun aTicketTimeTheAutumnChangeRepeatsIsTheFirst() {
        val now = at(ny, 2026, 11, 1, 0, 30)
        val end = MeterMath.endFor(MeterChoice.EndsAt(1, 30), now, ny)
        // The first 01:30 (summer time) is one hour on, not two.
        assertEquals(60 * min, end - now)
    }

    @Test fun theWarningIsMinutesBeforeTheEnd() {
        val now = 1_000_000L
        val end = now + 120 * min
        assertEquals(end - 5 * min, MeterMath.warnAt(end, 5, now))
        assertNull(MeterMath.warnAt(end, 0, now))
        // A warning longer than the meter has no time to ring before it: none.
        assertNull(MeterMath.warnAt(now + 10 * min, 15, now))
        assertNull(MeterMath.warnAt(now + 5 * min, 5, now))
    }

    @Test fun aTwoMinuteMeterWarnsAfterOne() {
        val now = 5_000_000L
        val m = MeterMath.start(MeterChoice.Minutes(2), 1, now, utc)
        assertEquals(now + 2 * min, m.endAt)
        assertEquals(now + 1 * min, m.warnAt)
        assertEquals(Due(MeterRing.Warn, now + min), MeterMath.next(m, now))
    }

    @Test fun ringsComeInOrderAndThenStop() {
        val now = 0L
        val m0 = MeterMath.start(MeterChoice.Minutes(30), 5, now, utc)
        // Too early: nothing rings, and the warning stays next.
        assertNull(MeterMath.ring(m0, 10 * min))
        val (m1, r1) = MeterMath.ring(m0, 25 * min)!!
        assertEquals(MeterRing.Warn, r1)
        assertTrue(m1.warned)
        assertEquals(Due(MeterRing.End, 30 * min), MeterMath.next(m1, 25 * min))
        val (m2, r2) = MeterMath.ring(m1, 30 * min)!!
        assertEquals(MeterRing.End, r2)
        assertTrue(m2.ended)
        assertNull(MeterMath.next(m2, 31 * min))
        assertNull(MeterMath.ring(m2, 31 * min))
    }

    @Test fun afterARestartAPendingMeterIsSetAgain() {
        val m = MeterMath.start(MeterChoice.Minutes(60), 5, 0L, utc)
        // Back on with both rings still ahead: the warning, at its own time.
        assertEquals(Due(MeterRing.Warn, 55 * min), MeterMath.next(m, 20 * min))
        assertNull(MeterMath.ring(m, 20 * min))
    }

    @Test fun afterARestartAMissedWarningRingsAtOnce() {
        val m = MeterMath.start(MeterChoice.Minutes(60), 5, 0L, utc)
        val now = 57 * min
        assertEquals(Due(MeterRing.Warn, now), MeterMath.next(m, now))
        assertEquals(MeterRing.Warn, MeterMath.ring(m, now)!!.second)
    }

    @Test fun afterARestartPastTheEndOnlyTheEndRings() {
        val m = MeterMath.start(MeterChoice.Minutes(60), 5, 0L, utc)
        val now = 90 * min
        assertEquals(Due(MeterRing.End, now), MeterMath.next(m, now))
        val (after, ring) = MeterMath.ring(m, now)!!
        assertEquals(MeterRing.End, ring)
        assertTrue(after.warned && after.ended)
    }

    @Test fun minutesLeftCountUp() {
        val m = Meter(0L, 10 * min, null)
        assertEquals(10L, m.minutesLeft(0L))
        assertEquals(1L, m.minutesLeft(10 * min - 30_000L))
        assertEquals(0L, m.minutesLeft(11 * min))
        assertFalse(m.isOver(10 * min - 1))
        assertTrue(m.isOver(10 * min))
    }

    @Test fun aMeterSurvivesStorage() {
        val m = Meter(1L, 2L, 3L, warned = true, ended = false)
        assertEquals(m, Meter.decode(m.encode()))
        val n = Meter(1L, 2L, null)
        assertEquals(n, Meter.decode(n.encode()))
        assertNull(Meter.decode(null))
        assertNull(Meter.decode("garbage"))
    }
}
