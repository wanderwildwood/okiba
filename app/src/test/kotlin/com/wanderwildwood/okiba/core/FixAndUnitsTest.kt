package com.wanderwildwood.okiba.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FixAndUnitsTest {
    @Test fun aRoughFirstFixIsNotTheEnd() {
        val p = BestFix<String>(goodEnoughM = 15f, settleMs = 20_000L)
        assertNull(p.settlesAt())
        assertFalse(p.offer("rough", 60f, 1_000L))
        assertEquals("rough", p.best)
        assertEquals(21_000L, p.settlesAt())
    }

    @Test fun aGoodFixEndsTheRun() {
        val p = BestFix<String>(goodEnoughM = 15f)
        assertFalse(p.offer("rough", 60f, 0L))
        assertFalse(p.offer("better", 25f, 1_000L))
        assertTrue(p.offer("good", 8f, 2_000L))
        assertEquals("good", p.best)
        assertEquals(8f, p.bestAccuracy)
    }

    @Test fun aWorseFixNeverReplacesABetterOne() {
        val p = BestFix<String>()
        p.offer("a", 30f, 0L)
        p.offer("b", 50f, 1_000L)
        assertEquals("a", p.best)
        // The settling time is counted from the first fix, not the latest.
        assertEquals(BestFix.SETTLE_MS, p.settlesAt())
    }

    @Test fun aFixWithNoAccuracyCountsAsTheWorst() {
        val p = BestFix<String>()
        p.offer("unknown", 0f, 0L)
        p.offer("known", 40f, 1_000L)
        assertEquals("known", p.best)
        val q = BestFix<String>()
        assertFalse(q.offer("only", 0f, 0L))
        assertEquals("only", q.best)
    }

    @Test fun unitsFollowTheCountryOnlyWhenAutomatic() {
        assertTrue(Units.Auto.imperial("US"))
        assertTrue(Units.Auto.imperial("us"))
        assertFalse(Units.Auto.imperial("PL"))
        assertFalse(Units.Auto.imperial(""))
        assertFalse(Units.Metric.imperial("US"))
        assertTrue(Units.Imperial.imperial("PL"))
        assertEquals(Units.Auto, Units.of(null))
        assertEquals(Units.Auto, Units.of("nonsense"))
        assertEquals(Units.Metric, Units.of("Metric"))
    }

    @Test fun theClockTicksWhenTheAgeChanges() {
        // Marked at 10 s past some minute; now is 50 s after that: the age turns in 10 s.
        assertEquals(10_000L, Age.nextTickMillis(70_000L, listOf(20_000L)))
        // Exactly on the minute: the next change is a whole minute away.
        assertEquals(60_000L, Age.nextTickMillis(80_000L, listOf(20_000L)))
        // Two anchors: whichever turns first.
        assertEquals(5_000L, Age.nextTickMillis(70_000L, listOf(20_000L, 15_000L)))
        // An anchor in the future, such as a meter's end, counts the same way.
        assertEquals(30_000L, Age.nextTickMillis(0L, listOf(90_000L)))
        // None: the clock's own minute.
        assertEquals(20_000L, Age.nextTickMillis(100_000L, emptyList()))
    }
}
