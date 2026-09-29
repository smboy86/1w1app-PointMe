package com.nadaworks.watchnavigation

import org.junit.Assert.*
import org.junit.Test

class DestinationTest {
    @Test fun rejectsInvalidIncomingCoordinates() {
        for ((lat, lon) in listOf(Double.NaN to 0.0, 0.0 to Double.POSITIVE_INFINITY,
            91.0 to 0.0, 0.0 to -181.0)) {
            assertThrows(IllegalArgumentException::class.java) { Destination("수신 목표", lat, lon) }
        }
        assertEquals(90.0, Destination("경계", 90.0, 180.0).latitude, 0.0)
        assertEquals(3, testDestinations.distinct().size)
    }
}
