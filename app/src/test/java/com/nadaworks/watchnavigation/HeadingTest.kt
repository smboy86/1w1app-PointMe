package com.nadaworks.watchnavigation

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingTest {
    @Test fun normalizesCompassAnglesAcrossNorth() {
        for ((input, expected) in listOf(-90f to 270f, -1f to 359f, 0f to 0f,
            90f to 90f, 359f to 359f, 360f to 0f, 721f to 1f)) {
            assertEquals(expected, normalizedDegrees(input), 0.001f)
        }
    }
}
