package com.nadaworks.watchnavigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.acos
import kotlin.math.sqrt
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

// 앱을 보고 있는 동안만 추적하여 백그라운드 배터리 사용을 막는다.
class MainActivity : ComponentActivity() {
    private lateinit var tracker: HeadingTracker
    private lateinit var location: LocationTracker
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        location.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tracker = HeadingTracker(this) { display?.rotation ?: Surface.ROTATION_0 }
        location = LocationTracker(this)
        setContent { MaterialTheme { CompassScreen(tracker, location) {
            val intent = if (!location.hasPermission()) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            } else Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } } }
        if (savedInstanceState == null && !location.hasPermission()) {
            permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    override fun onResume() { super.onResume(); tracker.start(); location.start() }
    override fun onPause() { tracker.stop(); location.stop(); super.onPause() }
}

// 꼬리 원은 화면 중심에 고정하고 화살표만 회전하며 수평계는 화면 좌표로 움직인다.
@Composable
private fun CompassScreen(tracker: HeadingTracker, location: LocationTracker, openLocationSettings: () -> Unit) {
    var choosingTarget by rememberSaveable { mutableStateOf(true) }
    BackHandler(choosingTarget) { choosingTarget = false }
    if (choosingTarget) {
        TargetSelection(location) { choosingTarget = false }
        return
    }
    var clockTick by remember { mutableLongStateOf(SystemClock.elapsedRealtimeNanos()) }
    LaunchedEffect(Unit) {
        while (true) { clockTick = SystemClock.elapsedRealtimeNanos(); delay(1000L) }
    }
    val fix = location.fix
    val now = maxOf(clockTick, SystemClock.elapsedRealtimeNanos())
    val fresh = fix?.isFresh(now) == true
    val canGuide = fix?.canGuide(now) == true
    val needsSettings = location.status == R.string.location_permission || location.status == R.string.gps_disabled
    val heading = tracker.heading
    val level = tracker.level
    val levelText = stringResource(when {
        level == null -> R.string.waiting
        level.isLevel -> R.string.level_ready
        else -> R.string.level_hint
    })
    val description = stringResource(R.string.arrow_description, location.destination.name) + ", " + levelText
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
            val unit = size.minDimension
            val radius = unit * 0.08f
            val shaft = unit * 0.015f
            val neck = -sqrt(radius * radius - shaft * shaft)
            val arcAngle = Math.toDegrees(acos((shaft / radius).toDouble())).toFloat()
            val arrowColor = Color(0xFF6DE1D2).copy(alpha = if (heading == null || !canGuide) 0.2f else 1f)
            rotate(if (canGuide && heading != null) fix!!.arrowRotation(heading) else 0f, pivot = center) {
                translate(center.x, center.y) {
                    val path = Path().apply {
                        moveTo(0f, -unit * 0.24f)
                        lineTo(unit * 0.105f, -unit * 0.13f)
                        lineTo(shaft, -unit * 0.16f)
                        lineTo(shaft, neck)
                        arcTo(Rect(-radius, -radius, radius, radius), -arcAngle,
                            180f + 2f * arcAngle, false)
                        lineTo(-shaft, -unit * 0.16f)
                        lineTo(-unit * 0.105f, -unit * 0.13f)
                        close()
                    }
                    drawPath(path, arrowColor, style = Stroke(width = unit * 0.012f, join = StrokeJoin.Round))
                }
            }
            // 수평계는 방위각과 독립적으로 화면의 높은 쪽을 향해 움직인다.
            level?.let {
                val bubbleCenter = center + Offset(it.x, it.y) * (unit * 0.18f)
                val bubbleColor = if (it.isLevel) Color(0xFFB3A0FF) else Color(0xFFFFBE73)
                drawCircle(bubbleColor.copy(alpha = 0.15f), radius * 0.8f, bubbleCenter)
                drawCircle(bubbleColor, radius * 0.8f, bubbleCenter, style = Stroke(unit * 0.008f))
            }
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 5.dp)
            .clickable(role = Role.Button) { choosingTarget = true }.heightIn(min = 48.dp).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.choose_target, location.destination.name),
                fontSize = 10.sp, color = Color(0xFF6DE1D2))
            Text(fix?.let { stringResource(R.string.distance_meters, it.distanceMeters.roundToInt()) } ?: "— m",
                fontSize = 20.sp, color = if (fresh) Color.White else Color(0xFFACB8C1))
            if (needsSettings) {
                Text(stringResource(R.string.location_settings), fontSize = 12.sp,
                    color = Color(0xFF6DE1D2), modifier = Modifier.clickable(onClick = openLocationSettings).padding(12.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(fix?.let { stringResource(R.string.gps_accuracy, ceil(it.accuracyMeters).toInt()) }
                ?: stringResource(location.status), fontSize = 10.sp, color = Color(0xFFACB8C1), textAlign = TextAlign.Center)
            if (fix != null) {
                Text(stringResource(if (fresh) R.string.gps_age else R.string.gps_stale, fix.ageSeconds(now)),
                    fontSize = 9.sp, color = if (fresh) Color(0xFFACB8C1) else Color(0xFFFFBE73))
            }
            val hint = when {
                tracker.status != R.string.sensor_active -> stringResource(tracker.status)
                fresh && !canGuide -> stringResource(R.string.within_accuracy)
                else -> levelText
            }
            Text(hint, fontSize = 10.sp, textAlign = TextAlign.Center,
                color = if (level?.isLevel == true) Color(0xFFB3A0FF) else Color(0xFFACB8C1))
        }
    }
}

// 3개 테스트 버튼만 제공하며 선택 즉시 기존 GPS 위치로 나침반 화면을 다시 계산한다.
@Composable
private fun TargetSelection(location: LocationTracker, close: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())
        .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.test_targets), fontSize = 12.sp, color = Color.White)
        testDestinations.forEach { target ->
            val chosen = target == location.destination
            Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .background(if (chosen) Color(0xFF20463F) else Color(0xFF1C2329), RoundedCornerShape(20.dp))
                .semantics { selected = chosen }
                .clickable(role = Role.Button) { location.updateDestination(target); close() }
                .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(target.name + if (chosen) " · " + stringResource(R.string.target_selected) else "",
                    fontSize = 13.sp, color = Color(0xFF6DE1D2))
                Text("${target.latitude}, ${target.longitude}", fontSize = 9.sp, color = Color(0xFFACB8C1))
            }
        }
        Text(stringResource(R.string.back_to_compass), fontSize = 12.sp, color = Color.White,
            modifier = Modifier.clickable(role = Role.Button, onClick = close).padding(16.dp))
    }
}
