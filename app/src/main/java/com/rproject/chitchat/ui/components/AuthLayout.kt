package com.rproject.chitchat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.rproject.chitchat.ui.theme.AuthGradientEnd
import com.rproject.chitchat.ui.theme.AuthGradientMid
import com.rproject.chitchat.ui.theme.AuthGradientStart
import kotlin.math.sin

/**
 * Shared animated auth screen layout:
 * - Top: vibrant gradient with logo/brand
 * - Smooth dynamic wave divider
 * - Bottom: white content card area with slide up animation
 */
@Composable
fun AuthLayout(
    headerContent: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable ColumnScope.() -> Unit
) {
    // Wave Animation State
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "waveOffset"
    )

    // Fade/Slide In State
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alphaAnim by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutExpo)
    )
    val slideAnim by animateFloatAsState(
        targetValue = if (visible) 0f else 60f,
        animationSpec = tween(800, easing = EaseOutExpo)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient background (full screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(AuthGradientStart, AuthGradientMid, AuthGradientEnd)
                    )
                )
        )

        // Animated Waves Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val waveHeight = 40f
            val baseLine = size.height * 0.38f
            
            // Back wave (semi-transparent)
            drawWave(this, baseLine, waveHeight, waveOffset + 1.5f, Color.White.copy(alpha = 0.3f))
            // Front wave
            drawWave(this, baseLine, waveHeight, waveOffset, Color.White)
        }

        // Header area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f),
            contentAlignment = Alignment.Center,
            content = headerContent
        )

        // Body area with slide up & fade in
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.66f)
                .padding(horizontal = 28.dp)
                .offset(y = slideAnim.dp)
                .background(Color.Transparent)
                .fillMaxSize(),
            content = bodyContent
        )
    }
}

private fun drawWave(scope: DrawScope, baseLine: Float, height: Float, offset: Float, color: Color) {
    with(scope) {
        val path = Path().apply {
            moveTo(0f, baseLine)
            // Draw sinusoidal wave across width
            var x = 0f
            while (x < size.width) {
                val y = baseLine + sin((x / size.width * 2 * Math.PI.toFloat()) + offset) * height
                lineTo(x, y)
                x += 10f
            }
            lineTo(size.width, baseLine + sin((2 * Math.PI.toFloat()) + offset) * height)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
    }
}
