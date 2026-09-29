package com.nadaworks.watchnavigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 선택한 목적지까지의 GPS 직선거리와 진북 기준 방위각을 계산한다.
internal class LocationTracker(private val context: Context) : LocationListener {
    private val manager = context.getSystemService(LocationManager::class.java)
    var destination by mutableStateOf(testDestinations.first())
        private set
    private var lastLocation: Location? = null
    var fix by mutableStateOf<TargetFix?>(null)
        private set
    var status by mutableIntStateOf(R.string.gps_waiting)
        private set

    fun hasPermission() = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // 향후 휴대폰 수신도 이 함수로 연결한다. 목표 변경은 GPS 측정 시각을 갱신하지 않는다.
    fun updateDestination(value: Destination) {
        destination = value
        fix = lastLocation?.let { calculateFix(it) }
    }

    // 캐시 위치는 쓰지 않고 화면을 연 뒤 받은 GPS 위치만 사용한다.
    fun start() {
        stop()
        fix = null
        lastLocation = null
        if (!hasPermission()) { status = R.string.location_permission; return }
        if (!manager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            status = R.string.gps_unavailable
            return
        }
        try {
            status = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) R.string.gps_waiting else R.string.gps_disabled
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
        } catch (_: SecurityException) {
            status = R.string.location_permission
        } catch (_: IllegalArgumentException) {
            status = R.string.gps_unavailable
        }
    }

    fun stop() { manager.removeUpdates(this) }

    override fun onLocationChanged(location: Location) {
        if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
            !location.longitude.isFinite() || location.longitude !in -180.0..180.0 ||
            !location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy <= 0f) return
        if (location.elapsedRealtimeNanos <= (fix?.measuredAtNanos ?: 0L)) return
        lastLocation = Location(location)
        fix = calculateFix(location)
        status = R.string.gps_active
    }

    private fun calculateFix(location: Location): TargetFix {
        val target = Location("target").apply {
            latitude = destination.latitude
            longitude = destination.longitude
        }
        val declination = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(),
            (if (location.hasAltitude()) location.altitude else 0.0).toFloat(), location.time).declination
        return TargetFix(location.distanceTo(target), location.accuracy,
            location.bearingTo(target), declination, location.elapsedRealtimeNanos)
    }

    override fun onProviderDisabled(provider: String) {
        fix = null
        lastLocation = null
        status = R.string.gps_disabled
    }

    override fun onProviderEnabled(provider: String) { status = R.string.gps_waiting }
}
