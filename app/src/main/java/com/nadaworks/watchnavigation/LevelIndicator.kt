package com.nadaworks.watchnavigation

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

// 화면 기준 -1~1 위치와 수평 여부. 화면이 아래를 향하면 수평으로 인정하지 않는다.
internal data class LevelReading(val x: Float, val y: Float, val isLevel: Boolean)

// 감도 조절값: 4° 이내는 중심에 붙이고 25°에서 최대 이동, 약 0.2초로 흔들림 완화.
internal class LevelIndicator {
    private val tolerance = sin(Math.toRadians(4.0)).toFloat()
    private val fullScale = sin(Math.toRadians(25.0)).toFloat()
    private var lastTime: Long? = null
    private var x = 0f
    private var y = 0f

    fun reset() { lastTime = null }

    // 회전 행렬의 세계 수직축을 화면 좌표로 변환하고 시간 기반 저역 통과 필터를 적용한다.
    fun update(upX: Float, upY: Float, upZ: Float, timestamp: Long): LevelReading {
        val alpha = lastTime?.let {
            (1.0 - exp(-((timestamp - it).coerceAtLeast(0) / 1e9) / 0.2)).toFloat()
        } ?: 1f
        lastTime = timestamp
        x += alpha * (upX - x)
        y += alpha * (-upY - y)
        val magnitude = hypot(x, y)
        val isLevel = upZ > 0f && magnitude <= tolerance
        if (isLevel) return LevelReading(0f, 0f, true)
        // 뒤집거나 세웠을 때도 중심 겹침이 수평처럼 보이지 않도록 가장자리로 보낸다.
        if (upZ <= 0f && magnitude <= tolerance) return LevelReading(0f, 1f, false)
        val distance = ((magnitude - tolerance) / (fullScale - tolerance)).coerceIn(0f, 1f)
        return LevelReading(x / magnitude * distance, y / magnitude * distance, false)
    }
}
