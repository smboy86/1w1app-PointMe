package com.nadaworks.watchnavigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 자기 북쪽 기준 방향을 추적하며, 화면이 활성화된 동안만 센서를 사용한다.
class HeadingTracker(context: Context, private val displayRotation: () -> Int) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val matrix = FloatArray(9)
    private val adjusted = FloatArray(9)
    private val orientation = FloatArray(3)

    var heading by mutableStateOf<Float?>(null)
        private set
    var status by mutableIntStateOf(R.string.waiting)
        private set

    // 이전 방향을 지우고 새 센서 이벤트를 기다린다.
    fun start() {
        heading = null
        status = when {
            sensor == null -> R.string.missing_sensor
            !manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) -> R.string.sensor_failed
            else -> R.string.waiting
        }
    }

    fun stop() { manager.unregisterListener(this) }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        val axes = when (displayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(matrix, axes.first, axes.second, adjusted)
        SensorManager.getOrientation(adjusted, orientation)
        heading = normalizedDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat())
        updateAccuracy(event.accuracy)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { updateAccuracy(accuracy) }

    private fun updateAccuracy(accuracy: Int) {
        status = if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            R.string.accuracy_low
        } else {
            R.string.sensor_active
        }
    }
}

// 음수와 한 바퀴 이상의 각도도 0 이상 360 미만으로 정규화한다.
internal fun normalizedDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f
