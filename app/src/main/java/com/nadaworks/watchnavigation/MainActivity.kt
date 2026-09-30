package com.nadaworks.watchnavigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.roundToInt

// 목표 좌표를 수신한 동안만 위치와 방향 센서를 사용한다.
class MainActivity : ComponentActivity() {
    private var screenActive by mutableStateOf(false)
    private var destinationNotice by mutableStateOf<Destination?>(null)
    private lateinit var tracker: HeadingTracker
    private lateinit var location: LocationTracker
    private var permissionPending = false

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionPending = false
        if (location.destination != null) startTracking()
    }

    private val destinationListener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
        events.forEach { event ->
            when (event.type) {
                DataEvent.TYPE_CHANGED -> receiveDestination(event.dataItem)
                DataEvent.TYPE_DELETED -> if (event.dataItem.uri.path == DESTINATION_PATH) clearDestination()
            }
        }
    }

    private fun receiveDestination(item: DataItem) {
        if (item.uri.path != DESTINATION_PATH) return
        val target = runCatching {
            val data = DataMapItem.fromDataItem(item).dataMap
            require(data.containsKey("latitude") && data.containsKey("longitude"))
            Destination(data.getString("name")?.takeIf(String::isNotBlank) ?: "목적지",
                data.getDouble("latitude"), data.getDouble("longitude"))
        }.getOrNull() ?: return
        runOnUiThread {
            val previous = location.destination
            location.updateDestination(target)
            if (previous != target) destinationNotice = target
            if (previous == null) {
                if (location.hasPermission()) startTracking() else location.start()
            }
        }
    }

    private fun clearDestination() = runOnUiThread {
        destinationNotice = null
        location.clearDestination()
        tracker.stop()
    }

    private fun acknowledgeDestination() {
        destinationNotice = null
        if (location.hasPermission()) startTracking()
        else if (!permissionPending) {
            permissionPending = true
            permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun startTracking() {
        if (!screenActive || location.destination == null) return
        if (!location.hasPermission()) { location.start(); return }
        tracker.start()
        location.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tracker = HeadingTracker(this) { display?.rotation ?: Surface.ROTATION_0 }
        location = LocationTracker(this)
        setContent {
            MaterialTheme {
                CompassScreen(tracker, location, screenActive, destinationNotice,
                    onAcknowledgeDestination = ::acknowledgeDestination) {
                    val intent = if (!location.hasPermission()) {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    } else Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        screenActive = true
        startTracking()
        val client = Wearable.getDataClient(this)
        client.addListener(destinationListener)
        client.dataItems.addOnSuccessListener { items ->
            items.forEach(::receiveDestination)
            items.release()
        }
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(destinationListener)
        screenActive = false
        tracker.stop()
        location.stop()
        super.onPause()
    }

    private companion object { const val DESTINATION_PATH = "/destination" }
}

@Composable
private fun CompassScreen(
    tracker: HeadingTracker,
    location: LocationTracker,
    active: Boolean,
    notice: Destination?,
    onAcknowledgeDestination: () -> Unit,
    openLocationSettings: () -> Unit,
) {
    val destination = location.destination
    if (destination == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.no_destination), fontSize = 16.sp, color = Color.White)
                Text(stringResource(R.string.waiting_for_destination), fontSize = 10.sp,
                    color = Color(0xFFACB8C1), textAlign = TextAlign.Center)
            }
        }
        return
    }

    var clockTick by remember { mutableLongStateOf(SystemClock.elapsedRealtimeNanos()) }
    LaunchedEffect(active) {
        while (active) { clockTick = SystemClock.elapsedRealtimeNanos(); delay(1000L) }
    }
    val fix = location.fix
    val now = maxOf(clockTick, SystemClock.elapsedRealtimeNanos())
    val fresh = fix?.isFresh(now) == true
    val canGuide = fix?.canGuide(now) == true
    val stage = waveStageFor(fix, now)
    val needsSettings = location.status == R.string.location_permission || location.status == R.string.gps_disabled ||
        location.status == R.string.gps_no_signal || location.status == R.string.gps_unavailable
    val heading = tracker.heading
    val level = tracker.level
    val levelText = stringResource(when {
        level == null -> R.string.waiting
        level.isLevel -> R.string.level_ready
        else -> R.string.level_hint
    })
    val description = stringResource(R.string.arrow_description, destination.name) + ", " + levelText
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        WaveCompass(stage, if (canGuide && heading != null) fix!!.arrowRotation(heading) else null,
            level, active, Modifier.fillMaxSize().semantics { contentDescription = description })
        Column(Modifier.align(Alignment.TopCenter).padding(top = 5.dp)
            .heightIn(min = 48.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(destination.name, fontSize = 10.sp, color = Color(0xFF6DE1D2))
            Text(fix?.let { stringResource(R.string.distance_meters, it.distanceMeters.roundToInt()) } ?: "— m",
                fontSize = 20.sp, color = if (fresh) Color.White else Color(0xFFACB8C1))
            if (needsSettings) {
                Text(stringResource(R.string.location_settings), fontSize = 12.sp,
                    color = Color(0xFF6DE1D2), modifier = Modifier.clickable(onClick = openLocationSettings).padding(12.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp)
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            if (stage != null) Text("${waveStages.indexOf(stage) + 1}/7 · ${stage.name} · 추정",
                fontSize = 9.sp, color = Color(stage.color))
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
    if (notice != null) DestinationReceivedDialog(notice, onAcknowledgeDestination)
}

@Composable
private fun DestinationReceivedDialog(target: Destination, onAcknowledge: () -> Unit) {
    Dialog(onDismissRequest = onAcknowledge) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF182327), RoundedCornerShape(24.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.destination_received), fontSize = 15.sp, color = Color.White)
            Text(target.name, fontSize = 12.sp, color = Color(0xFF6DE1D2))
            Text(stringResource(R.string.received_coordinates, target.latitude.toString(), target.longitude.toString()),
                fontSize = 11.sp, color = Color(0xFFDAE2E5), textAlign = TextAlign.Center)
            Text(stringResource(R.string.start_guidance), fontSize = 12.sp, color = Color(0xFF6DE1D2),
                modifier = Modifier.clickable(role = Role.Button, onClick = onAcknowledge).padding(12.dp))
        }
    }
}
