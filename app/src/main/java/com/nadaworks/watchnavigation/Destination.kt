package com.nadaworks.watchnavigation

// 휴대폰에서 수신한 목적지 좌표. 잘못된 좌표는 수신 경계에서 거부한다.
internal data class Destination(val name: String, val latitude: Double, val longitude: Double) {
    init {
        require(name.isNotBlank())
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}
