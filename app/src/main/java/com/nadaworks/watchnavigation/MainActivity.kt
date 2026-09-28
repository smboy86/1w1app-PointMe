package com.nadaworks.watchnavigation

import android.os.Bundle
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
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

// 테스트 목표는 자기 북쪽에서 시계방향 45°인 북동쪽이며, 숫자는 현재 워치 방위각이다.
@Composable
private fun CompassScreen(tracker: HeadingTracker) {
    val heading = tracker.heading
    val arrowDescription = stringResource(R.string.arrow_description)
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.target_label), fontSize = 12.sp, color = Color(0xFFACB8C1))
        Canvas(
            Modifier.size(64.dp)
                .graphicsLayer { rotationZ = 45f - (heading ?: 0f); alpha = if (heading == null) 0.2f else 1f }
                .semantics { contentDescription = arrowDescription },
        ) {
            drawPath(Path().apply {
                moveTo(size.width * 0.5f, 0f)
                lineTo(size.width * 0.86f, size.height * 0.9f)
                lineTo(size.width * 0.5f, size.height * 0.7f)
                lineTo(size.width * 0.14f, size.height * 0.9f)
                close()
            }, Color(0xFF6DE1D2))
        }
        Text(
            heading?.let { stringResource(R.string.heading_degrees, it.toInt()) } ?: "—",
            fontSize = 26.sp,
            color = Color.White,
        )
        Text(stringResource(tracker.status), fontSize = 10.sp, textAlign = TextAlign.Center,
            color = Color(0xFFACB8C1))
    }
}
