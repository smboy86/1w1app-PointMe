package com.nadaworks.watchnavigation

// 거리 하한(m), 색, 파동 수, 확산 주기(ms), 밝기를 이 표에서 함께 조절한다.
internal data class WaveStage(
    val minMeters: Float, val name: String, val color: Long,
    val rings: Int, val durationMillis: Int, val intensity: Float,
)

// 큰 거리부터 정렬한다. 경계값은 더 먼 단계에 포함된다(예: 500m는 1단계).
internal val waveStages = listOf(
    WaveStage(500f, "아주 멀 때", 0xFF248DFF, 2, 7000, 0.12f),
    WaveStage(200f, "멀 때", 0xFF248DFF, 3, 5800, 0.22f),
    WaveStage(50f, "조금 가까울 때", 0xFF37BFFF, 4, 4600, 0.42f),
    WaveStage(10f, "가까울 때", 0xFF38E2C4, 5, 3400, 0.60f),
    WaveStage(3f, "매우 가까울 때", 0xFFFFD45B, 6, 2500, 0.76f),
    WaveStage(1f, "목표 근처", 0xFFFF456D, 6, 1900, 0.90f),
    WaveStage(0f, "도착 범위", 0xFFFF204C, 7, 1400, 1f),
)

// 위치가 신선할 때만 효과를 사용한다. 추정 반경을 더해 가까움을 과장하지 않는다.
internal fun waveStageFor(fix: TargetFix?, nowNanos: Long): WaveStage? {
    if (fix == null || !fix.isFresh(nowNanos) || !fix.distanceMeters.isFinite() ||
        !fix.accuracyMeters.isFinite() || fix.distanceMeters < 0f || fix.accuracyMeters < 0f) return null
    val conservativeDistance = fix.distanceMeters + fix.accuracyMeters
    return waveStages.first { conservativeDistance >= it.minMeters }
}
