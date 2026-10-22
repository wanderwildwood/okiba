package com.wanderwildwood.okiba.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotTest {
    private fun spot(t: Long, note: String = "") = Spot(12.5, -45.25, 6f, t, note)

    @Test fun geoUriCarriesCoordinatesAndAnEncodedLabel() {
        assertEquals("geo:12.500000,-45.250000?q=12.500000,-45.250000(Parked)", spot(1).geoUri("Parked"))
        assertEquals("geo:12.500000,-45.250000?q=12.500000,-45.250000(My%20car%28old%29)", spot(1).geoUri("My car(old)"))
    }

    @Test fun shareTextHasGeoAndWebLinks() {
        val t = spot(1, "level 3, row C").shareText("Parked", "Thu 22 Oct 16:41")
        assertTrue(t.startsWith("Parked Thu 22 Oct 16:41\nlevel 3, row C\n"))
        assertTrue(t.contains("geo:12.500000,-45.250000?q="))
        assertTrue(t.endsWith("https://www.openstreetmap.org/?mlat=12.500000&mlon=-45.250000#map=19/12.500000/-45.250000"))
    }

    @Test fun noteIsLeftOutOfShareWhenBlank() {
        assertTrue(spot(1).shareText("Parked", "x").lines()[1].startsWith("geo:"))
    }

    @Test fun historyKeepsFiveNewestFirst() {
        var h = History()
        for (t in 1L..7L) h = h.marked(spot(t))
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L), h.spots.map { it.timeMillis })
        assertEquals(7L, h.shown?.timeMillis)
    }

    @Test fun markingTheSameInstantTwiceDoesNotDuplicate() {
        val h = History().marked(spot(1)).marked(spot(1))
        assertEquals(1, h.spots.size)
    }

    @Test fun showingAnOlderSpot() {
        val h = History().marked(spot(1)).marked(spot(2)).show(spot(1))
        assertEquals(1L, h.shown?.timeMillis)
        assertEquals(2, h.spots.size)
    }

    @Test fun showingAnUnknownSpotChangesNothing() {
        val h = History().marked(spot(1))
        assertEquals(h, h.show(spot(99)))
    }

    @Test fun forgettingTheShownSpotFallsBackToTheNewest() {
        val h = History().marked(spot(1)).marked(spot(2)).marked(spot(3)).show(spot(1)).forgotten(spot(1))
        assertEquals(listOf(3L, 2L), h.spots.map { it.timeMillis })
        assertEquals(3L, h.shown?.timeMillis)
    }

    @Test fun forgettingAnotherSpotKeepsTheShownOne() {
        val h = History().marked(spot(1)).marked(spot(2)).forgotten(spot(1))
        assertEquals(2L, h.shown?.timeMillis)
    }

    @Test fun forgettingTheLastLeavesNothing() {
        val h = History().marked(spot(1)).forgotten(spot(1))
        assertNull(h.shown)
        assertTrue(h.spots.isEmpty())
    }

    @Test fun noteIsCleanedAndCapped() {
        val h = History().marked(spot(1)).withNote(spot(1), "  level\t3\nrow C  ")
        assertEquals("level 3 row C", h.shown?.note)
        val long = History().marked(spot(1)).withNote(spot(1), "x".repeat(200))
        assertEquals(NOTE_MAX, long.shown?.note?.length)
    }

    @Test fun storageRoundTrips() {
        val h = History().marked(spot(1, "level 3, row C")).marked(spot(2)).show(spot(1))
        assertEquals(h, History.decode(History.encode(h)))
    }

    @Test fun decodingGarbageGivesAnEmptyHistory() {
        assertTrue(History.decode(null).spots.isEmpty())
        assertTrue(History.decode("").spots.isEmpty())
        assertTrue(History.decode("not\ta\tspot\nat all").spots.isEmpty())
    }

    @Test fun ageUnits() {
        val min = 60_000L
        assertEquals(Age.JustNow, Age.of(30_000))
        assertEquals(Age.JustNow, Age.of(-5 * min))
        assertEquals(Age.Minutes(1), Age.of(min))
        assertEquals(Age.Minutes(59), Age.of(59 * min + 59_000))
        assertEquals(Age.Hours(1, 0), Age.of(60 * min))
        assertEquals(Age.Hours(2, 10), Age.of(130 * min))
        assertEquals(Age.Hours(23, 59), Age.of(24 * 60 * min - min))
        assertEquals(Age.Days(1, 0), Age.of(24 * 60 * min))
        assertEquals(Age.Days(3, 5), Age.of((3 * 24 + 5) * 60 * min))
    }
}
