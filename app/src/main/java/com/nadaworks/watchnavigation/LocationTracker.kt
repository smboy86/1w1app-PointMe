package com.nadaworks.watchnavigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// Wear OS의 통합 위치 공급자를 통해 워치 GPS 및 사용 가능한 위치 소스를 요청한다.
internal class LocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private var listening = false
    private val noFixTimeout = Runnable {
        if (listening && fix == null) status = R.string.gps_no_signal
    }
    var destination by mutableStateOf<Destination?>(null)
        private set
    private var lastLocation: Location? = null
    var fix by mutableStateOf<TargetFix?>(null)
        private set
    var status by mutableIntStateOf(R.string.gps_waiting)
        private set

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::acceptLocation)
        }

        override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
            if (fix == null) status = if (availability.isLocationAvailable) R.string.gps_waiting else R.string.gps_no_signal
        }
    }

    fun hasPermission() = appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // 목표 변경은 GPS 측정 시각을 갱신하지 않는다.
    fun updateDestination(value: Destination) {
        destination = value
        fix = lastLocation?.let { calculateFix(it, value) }
    }

    fun clearDestination() {
        stop()
        destination = null
        lastLocation = null
        fix = null
    }

    fun start() {
        if (listening) return
        stop()
        fix = null
        lastLocation = null
        if (!hasPermission()) { status = R.string.location_permission; return }
        status = R.string.gps_waiting
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { if (listening) status = R.string.gps_unavailable }
            listening = true
            handler.postDelayed(noFixTimeout, 20_000L)
        } catch (_: SecurityException) {
            status = R.string.location_permission
        }
    }

    fun stop() {
        handler.removeCallbacks(noFixTimeout)
        if (listening) client.removeLocationUpdates(callback)
        listening = false
    }

    private fun acceptLocation(location: Location) {
        val target = destination ?: return
        if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
            !location.longitude.isFinite() || location.longitude !in -180.0..180.0 ||
            !location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy <= 0f ||
            location.elapsedRealtimeNanos <= (fix?.measuredAtNanos ?: 0L)) return
        lastLocation = Location(location)
        fix = calculateFix(location, target)
        handler.removeCallbacks(noFixTimeout)
        status = R.string.gps_active
    }

    private fun calculateFix(location: Location, destination: Destination): TargetFix {
        val target = Location("target").apply {
            latitude = destination.latitude
            longitude = destination.longitude
        }
        val declination = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(),
            (if (location.hasAltitude()) location.altitude else 0.0).toFloat(), location.time).declination
        return TargetFix(location.distanceTo(target), location.accuracy, location.bearingTo(target),
            declination, location.elapsedRealtimeNanos)
    }
}
