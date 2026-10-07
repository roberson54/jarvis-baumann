package com.baumann.jarvis.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.baumann.jarvis.domain.model.JarvisState

fun stateColor(state: JarvisState): Color = when (state) {
    JarvisState.IDLE -> Color(0xFF29B6F6)
    JarvisState.LISTENING -> Color(0xFF00E5FF)
    JarvisState.PROCESSING -> Color(0xFFFFB300)
    JarvisState.SPEAKING -> Color(0xFF69F0AE)
    JarvisState.ERROR -> Color(0xFFFF5252)
}

fun stateLabel(state: JarvisState): String = when (state) {
    JarvisState.IDLE -> "AGUARDANDO"
    JarvisState.LISTENING -> "OUVINDO"
    JarvisState.PROCESSING -> "PROCESSANDO"
    JarvisState.SPEAKING -> "FALANDO"
    JarvisState.ERROR -> "ERRO"
}

@Composable
fun JarvisCore(state: JarvisState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "core")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = if (state == JarvisState.PROCESSING) 1800 else 7000,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    val pulseMs = when (state) {
        JarvisState.LISTENING -> 600
        JarvisState.PROCESSING -> 900
        JarvisState.SPEAKING -> 450
        else -> 1800
    }
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = pulseMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val color = stateColor(state)

    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension / 2f
        val stroke = 3.dp.toPx()

        drawCircle(color.copy(alpha = 0.12f), radius = r * 0.62f * pulse, center = c)
        drawCircle(color, radius = r * 0.34f * pulse, center = c, style = Stroke(stroke))
        drawCircle(color.copy(alpha = 0.85f), radius = r * 0.12f, center = c)

        val ring1 = r * 0.78f
        for (i in 0..1) {
            drawArc(
                color = color,
                startAngle = rotation + i * 180f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(c.x - ring1, c.y - ring1),
                size = Size(ring1 * 2, ring1 * 2),
                style = Stroke(stroke * 1.5f, cap = StrokeCap.Round)
            )
        }

        val ring2 = r * 0.95f
        drawCircle(color.copy(alpha = 0.25f), radius = ring2, center = c, style = Stroke(1.dp.toPx()))
        drawArc(
            color = color.copy(alpha = 0.6f),
            startAngle = -rotation * 1.5f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(c.x - ring2, c.y - ring2),
            size = Size(ring2 * 2, ring2 * 2),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}
