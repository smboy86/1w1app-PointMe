package com.nadaworks.watchnavigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.sin
import kotlin.math.PI

// 원점은 항상 화면 정중앙. 화살표 회전과 수평계 이동은 서로 독립적이다.
@Composable
internal fun WaveCompass(stage: WaveStage?, rotation: Float?, level: LevelReading?, active: Boolean, modifier: Modifier) {
    val phase: State<Float> = if (active && stage != null) {
        rememberInfiniteTransition(label = "distance waves").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(stage.durationMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "outward phase",
        )
    } else rememberUpdatedState(0f)
    Canvas(modifier) {
        val unit = size.minDimension
        val color = stage?.let { Color(it.color) } ?: Color(0xFF647483)
        val strength = stage?.intensity ?: 0f
        val maxRadius = unit * 0.46f
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = strength * 0.28f), Color.Transparent),
            center = center, radius = maxRadius), maxRadius, center)
        if (stage != null) {
            repeat(stage.rings) { index ->
                val progress = (phase.value + index.toFloat() / stage.rings) % 1f
                val radius = unit * (0.075f + progress * 0.385f)
                val alpha = (sin(progress * PI).toFloat() * strength).coerceIn(0f, 1f)
                // 넓고 옅은 선을 겹쳐 발광을 표현한다. 블러·이미지·별도 그래픽 라이브러리는 쓰지 않는다.
                drawCircle(color.copy(alpha = alpha * 0.10f), radius, center, style = Stroke(unit * 0.026f))
                drawCircle(color.copy(alpha = alpha * 0.25f), radius, center, style = Stroke(unit * 0.012f))
                drawCircle(color.copy(alpha = alpha), radius, center, style = Stroke(unit * 0.003f))
            }
        }
        val arrowColor = Color(0xFF6DE1D2)
        val arrowAlpha = if (rotation == null) 0.2f else 1f
        val arrowSize = unit * (2f / 3f)
        val targetRotation = rotation ?: 0f
        // 수평계는 같은 화살표의 흐린 그림자다. 수평이면 중심 화살표와 겹친다.
        level?.let {
            val shadowOffset = Offset(it.x, it.y) * (unit * 0.18f)
            drawNavigationArrow(center + shadowOffset, targetRotation, arrowSize, arrowColor.copy(alpha = 0.16f))
        }
        drawNavigationArrow(center, targetRotation, arrowSize, arrowColor.copy(alpha = arrowAlpha))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNavigationArrow(
    anchor: Offset, rotation: Float, size: Float, color: Color,
) {
    // 폭을 넓히고 꼭짓점과 안쪽 골을 둥글려 통통한 삼각 화살촉으로 만든다.
    val tip = Offset(0f, -size * 0.24f)
    val valley = Offset(0f, size * 0.035f)
    val left = Offset(-size * 0.195f, size * 0.13f)
    val right = Offset(size * 0.195f, size * 0.13f)
    val silhouette = Path().apply {
        moveTo(-size * 0.012f, -size * 0.232f)
        quadraticTo(tip.x, -size * 0.252f, size * 0.012f, -size * 0.232f)
        cubicTo(size * 0.075f, -size * 0.08f, size * 0.185f, size * 0.09f, right.x, right.y)
        quadraticTo(size * 0.202f, size * 0.145f, size * 0.18f, size * 0.132f)
        cubicTo(size * 0.11f, size * 0.105f, size * 0.045f, size * 0.052f, valley.x, valley.y)
        cubicTo(-size * 0.045f, size * 0.052f, -size * 0.11f, size * 0.105f, -size * 0.18f, size * 0.132f)
        quadraticTo(-size * 0.202f, size * 0.145f, left.x, left.y)
        cubicTo(-size * 0.185f, size * 0.09f, -size * 0.075f, -size * 0.08f, -size * 0.012f, -size * 0.232f)
        close()
    }
    val leftFace = Path().apply {
        moveTo(-size * 0.014f, -size * 0.22f)
        cubicTo(-size * 0.08f, -size * 0.06f, -size * 0.18f, size * 0.10f, left.x, left.y)
        quadraticTo(-size * 0.10f, size * 0.095f, valley.x, valley.y)
        close()
    }
    val rightFace = Path().apply {
        moveTo(size * 0.014f, -size * 0.22f)
        cubicTo(size * 0.08f, -size * 0.06f, size * 0.18f, size * 0.10f, right.x, right.y)
        quadraticTo(size * 0.10f, size * 0.095f, valley.x, valley.y)
        close()
    }
    val centerY = size * 0.07f
    rotate(rotation, pivot = anchor + Offset(0f, centerY)) {
        translate(anchor.x, anchor.y + centerY) {
            // 닫힌 바탕 면이 항상 밑에 있어 곡면 분할선의 안티앨리어싱 틈도 검게 비치지 않는다.
            translate(top = size * 0.014f) {
                drawPath(silhouette, Color(0xFF063F3D).copy(alpha = color.alpha))
            }
            drawPath(silhouette, brush = Brush.linearGradient(
                listOf(Color(0xFF52DDB9), Color(0xFF117F70)),
                start = Offset(-size * 0.12f, -size * 0.2f), end = Offset(size * 0.14f, size * 0.13f),
            ), alpha = color.alpha)
            drawPath(silhouette, color.copy(alpha = color.alpha * 0.25f),
                style = Stroke(width = size * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(leftFace, brush = Brush.linearGradient(
                listOf(Color(0xFFB0FFE9), Color(0xFF48D8B5), Color(0xFF15927E)),
                start = Offset(-size * 0.13f, -size * 0.2f), end = Offset(size * 0.06f, size * 0.12f),
            ), alpha = color.alpha)
            drawPath(rightFace, brush = Brush.linearGradient(
                listOf(Color(0xFF54DAB4), Color(0xFF08715F), Color(0xFF034C45)),
                start = Offset(-size * 0.02f, -size * 0.2f), end = Offset(size * 0.16f, size * 0.12f),
            ), alpha = color.alpha)
            drawPath(silhouette, color.copy(alpha = color.alpha * 0.9f),
                style = Stroke(width = size * 0.007f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(Color(0xFFB8FFE9).copy(alpha = color.alpha * 0.9f), tip, valley, size * 0.004f)
        }
    }
}
