package com.nadaworks.watchnavigation

import org.junit.Assert.*
import org.junit.Test

class WaveStageTest {
    @Test fun sevenBoundariesUncertaintyAndStaleFix() {
        fun stage(distance: Float, accuracy: Float = 0f) = waveStageFor(TargetFix(distance, accuracy, 0f, 0f, 1L), 1L)
        assertEquals(7, waveStages.size)
        assertTrue(waveStages.zipWithNext().all { (a, b) -> a.minMeters > b.minMeters && a.durationMillis > b.durationMillis })
        for ((index, threshold) in listOf(500f, 200f, 50f, 10f, 3f, 1f, 0f).withIndex()) {
            assertEquals(waveStages[index], stage(threshold))
            if (index < 6) assertEquals(waveStages[index + 1], stage(threshold - 0.01f))
        }
        assertEquals(waveStages[3], stage(0.5f, 12f))
        assertNull(stage(Float.NaN))
        assertNull(stage(-1f))
        assertNull(waveStageFor(null, 1L))
        assertNull(waveStageFor(TargetFix(1f, 1f, 0f, 0f, 0L), 11_000_000_000L))
    }
}
