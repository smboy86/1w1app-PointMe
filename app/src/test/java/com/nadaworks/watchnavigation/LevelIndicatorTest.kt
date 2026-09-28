package com.nadaworks.watchnavigation

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.cos

class LevelIndicatorTest {
    @Test fun levelToleranceDirectionsClampAndSmoothing() {
        fun sample(x: Float, y: Float, z: Float = 1f) = LevelIndicator().update(x, y, z, 0)
        assertEquals(LevelReading(0f, 0f, true), sample(0f, 0f))
        assertTrue(sample(sin(Math.toRadians(3.0)).toFloat(), 0f).isLevel)
        assertFalse(sample(sin(Math.toRadians(5.0)).toFloat(), 0f).isLevel)
        assertTrue(sample(0.2f, 0f).x > 0f)
        assertTrue(sample(-0.2f, 0f).x < 0f)
        assertTrue(sample(0f, 0.2f).y < 0f)
        assertTrue(sample(0f, -0.2f).y > 0f)
        val diagonal = sample(0.6f, 0.6f)
        assertEquals(1f, hypot(diagonal.x, diagonal.y), 0.001f)
        assertFalse(sample(0f, 0f, -1f).isLevel)
        assertEquals(1f, sample(0f, 0f, -1f).y, 0.001f)
        // 수평 상태에서 흔들림은 서서히 반영하며 재시작 시 이전 기울기를 버린다.
        val filter = LevelIndicator()
        filter.update(0f, 0f, 1f, 0)
        val smoothed = filter.update(0.4f, 0f, 0.9f, 66_000_000)
        assertTrue(smoothed.x in 0f..sample(0.4f, 0f).x)
        filter.reset()
        assertEquals(sample(0.4f, 0f), filter.update(0.4f, 0f, 1f, 100_000_000))
        // 실제 회전 행렬에 해당하는 15° 기울기에서도 수평으로 판정하지 않는다.
        val tilt = Math.toRadians(15.0)
        assertFalse(sample(sin(tilt).toFloat(), 0f, cos(tilt).toFloat()).isLevel)
    }
}
