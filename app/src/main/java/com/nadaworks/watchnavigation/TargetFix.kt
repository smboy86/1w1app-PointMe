package com.nadaworks.watchnavigation

// accuracyMeters는 68% 신뢰 수준의 수평 위치 추정 반경이며 실제 최대 오차가 아니다.
internal data class TargetFix(
    val distanceMeters: Float,
    val accuracyMeters: Float,
    val trueBearing: Float,
    val declination: Float,
    val measuredAtNanos: Long,
) {
    fun ageSeconds(nowNanos: Long): Long = ((nowNanos - measuredAtNanos).coerceAtLeast(0L) / 1_000_000_000L)
    fun isFresh(nowNanos: Long): Boolean = nowNanos >= measuredAtNanos && nowNanos - measuredAtNanos <= 10_000_000_000L
    fun canGuide(nowNanos: Long): Boolean = isFresh(nowNanos) && distanceMeters > accuracyMeters
    // GPS 방위각은 진북, 회전 센서는 자북이므로 자기 편각을 더해 같은 기준으로 맞춘다.
    fun arrowRotation(magneticHeading: Float): Float = trueBearing - (magneticHeading + declination)
}
