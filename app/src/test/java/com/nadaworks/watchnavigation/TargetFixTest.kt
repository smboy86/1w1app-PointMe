package com.nadaworks.watchnavigation

import org.junit.Assert.*
import org.junit.Test

class TargetFixTest {
    @Test fun freshnessUncertaintyAndTrueNorthCorrection() {
        val fix = TargetFix(125f, 8f, 45f, -8f, 1_000_000_000L)
        assertEquals(1L, fix.ageSeconds(2_000_000_000L))
        assertTrue(fix.canGuide(2_000_000_000L))
        assertTrue(fix.isFresh(11_000_000_000L))
        assertFalse(fix.isFresh(11_000_000_001L))
        assertFalse(fix.isFresh(0L))
        assertFalse(fix.copy(distanceMeters = 8f).canGuide(2_000_000_000L))
        assertEquals(0f, fix.arrowRotation(53f), 0.001f)
        assertEquals(-90f, fix.arrowRotation(143f), 0.001f)
    }
}
