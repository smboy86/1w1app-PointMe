package com.nadaworks.watchnavigation

// 테스트 버튼과 향후 휴대폰 수신이 함께 사용하는 목적지 값. 잘못된 좌표는 경계에서 거부한다.
internal data class Destination(val name: String, val latitude: Double, val longitude: Double) {
    init {
        require(name.isNotBlank())
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

// 실기기 비교용 하드코딩 좌표: 원래 목표, 북쪽 약 111m, 동쪽 약 106m.
internal val testDestinations = listOf(
    Destination("좌표1", 37.53614, 127.13265),
    Destination("좌표2", 37.53714, 127.13265),
    Destination("좌표3", 37.53614, 127.13385),
)
