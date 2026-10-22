package com.wanderwildwood.okiba.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Every place here is made up: points on the equator and the prime meridian, or a grid.
class GeoTest {
    @Test fun oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111_195.0, Geo.distanceMeters(0.0, 0.0, 1.0, 0.0), 50.0)
    }

    @Test fun distanceToSelfIsZeroAndSymmetric() {
        assertEquals(0.0, Geo.distanceMeters(10.0, 20.0, 10.0, 20.0), 1e-6)
        val a = Geo.distanceMeters(10.0, 20.0, 10.001, 20.002)
        val b = Geo.distanceMeters(10.001, 20.002, 10.0, 20.0)
        assertEquals(a, b, 1e-6)
    }

    @Test fun streetScaleDistance() {
        // 0.0018 degrees of latitude is about 200 m.
        assertEquals(200.0, Geo.distanceMeters(10.0, 20.0, 10.0018, 20.0), 1.0)
    }

    @Test fun bearingsAtTheCardinalPoints() {
        assertEquals(0.0, Geo.bearingDegrees(0.0, 0.0, 1.0, 0.0), 0.01)
        assertEquals(90.0, Geo.bearingDegrees(0.0, 0.0, 0.0, 1.0), 0.01)
        assertEquals(180.0, Geo.bearingDegrees(0.0, 0.0, -1.0, 0.0), 0.01)
        assertEquals(270.0, Geo.bearingDegrees(0.0, 0.0, 0.0, -1.0), 0.01)
    }

    @Test fun compassPointsAndTheirEdges() {
        assertEquals(Compass.N, Geo.compass(0.0))
        assertEquals(Compass.N, Geo.compass(22.4))
        assertEquals(Compass.NE, Geo.compass(22.6))
        assertEquals(Compass.E, Geo.compass(90.0))
        assertEquals(Compass.SW, Geo.compass(225.0))
        assertEquals(Compass.NW, Geo.compass(315.0))
        assertEquals(Compass.N, Geo.compass(359.9))
        assertEquals(Compass.N, Geo.compass(-10.0))
    }

    @Test fun distanceWords() {
        assertEquals("5 m", Geo.formatDistance(2.0, false))
        assertEquals("180 m", Geo.formatDistance(183.0, false))
        assertEquals("1.4 km", Geo.formatDistance(1_420.0, false))
        assertEquals("30 ft", Geo.formatDistance(9.0, true))
        assertEquals("0.5 mi", Geo.formatDistance(800.0, true))
    }

    @Test fun marginOfError() {
        assertTrue(Geo.isWithinMargin(8.0, 3.0, 3.0))   // under the 10 m floor
        assertTrue(Geo.isWithinMargin(25.0, 15.0, 15.0))
        assertFalse(Geo.isWithinMargin(60.0, 15.0, 15.0))
    }

    @Test fun coordinatesAreFixedPointWithADotWhateverTheLocale() {
        assertEquals("12.34568, -98.76543", Geo.formatCoordinates(12.345678, -98.765432))
    }

    @Test fun imperialCountries() {
        assertTrue(Geo.usesImperial("US"))
        assertFalse(Geo.usesImperial("DE"))
    }
}
