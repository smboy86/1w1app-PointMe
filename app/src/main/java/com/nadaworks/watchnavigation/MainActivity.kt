package com.nadaworks.watchnavigation

import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tracker = HeadingTracker(this) { display?.rotation ?: Surface.ROTATION_0 }
        setContent { MaterialTheme { CompassScreen(tracker) } }
    }

    override fun onResume() { super.onResume(); tracker.start() }
    override fun onPause() { tracker.stop(); super.onPause() }
}

// 꼬리 원은 화면 중심에 고정하고 화살표만 회전하며 수평계는 화면 좌표로 움직인다.
@Composable
private fun CompassScreen(tracker: HeadingTracker) {
    val heading = tracker.heading
    val level = tracker.level
    val levelText = stringResource(when {
        level == null -> R.string.waiting
        level.isLevel -> R.string.level_ready
        else -> R.string.level_hint
    })
    val description = stringResource(R.string.arrow_description) + ", " + levelText
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
            val unit = size.minDimension
            val radius = unit * 0.08f
            val shaft = unit * 0.015f
            val neck = -sqrt(radius * radius - shaft * shaft)
            val arcAngle = Math.toDegrees(acos((shaft / radius).toDouble())).toFloat()
            val arrowColor = Color(0xFF6DE1D2).copy(alpha = if (heading == null) 0.25f else 1f)
            rotate(45f - (heading ?: 0f), pivot = center) {
                translate(center.x, center.y) {
                    val path = Path().apply {
                        moveTo(0f, -unit * 0.32f)
                        lineTo(unit * 0.105f, -unit * 0.15f)
                        lineTo(shaft, -unit * 0.19f)
                        lineTo(shaft, neck)
                        arcTo(Rect(-radius, -radius, radius, radius), -arcAngle,
                            180f + 2f * arcAngle, false)
                        lineTo(-shaft, -unit * 0.19f)
                        lineTo(-unit * 0.105f, -unit * 0.15f)
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
        Text(stringResource(R.string.target_label), fontSize = 11.sp, color = Color(0xFFACB8C1),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
        Text(levelText, fontSize = 12.sp, textAlign = TextAlign.Center,
            color = if (level?.isLevel == true) Color(0xFFB3A0FF) else Color(0xFFACB8C1),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp))
        if (tracker.status != R.string.sensor_active) {
            Text(stringResource(tracker.status), fontSize = 10.sp, textAlign = TextAlign.Center,
                color = Color(0xFFFFBE73),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 19.dp))
        }
    }
}
