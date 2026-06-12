package com.rproject.chitchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import com.rproject.chitchat.ui.theme.AuthGradientEnd
import com.rproject.chitchat.ui.theme.AuthGradientMid
import com.rproject.chitchat.ui.theme.AuthGradientStart

/**
 * Shared auth screen layout:
 * - Top: vibrant gradient with logo/brand
 * - Smooth wave divider
 * - Bottom: white content card area
 */
@Composable
fun AuthLayout(
    headerContent: @Composable BoxScope.() -> Unit,
    bodyContent: @Composable ColumnScope.() -> Unit
) {
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

        // White wave overlay (covers ~62% from bottom)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWave(this)
        }

        // Header area — sits in gradient section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f),
            contentAlignment = Alignment.Center,
            content = headerContent
        )

        // Body area — sits on the white section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .fillMaxHeight(0.66f)
                .padding(horizontal = 28.dp),
            content = bodyContent
        )
    }
}

private fun drawWave(scope: DrawScope) {
    with(scope) {
        val waveY = size.height * 0.38f
        val path = Path().apply {
            moveTo(0f, waveY + 16f)
            cubicTo(
                size.width * 0.28f, waveY - 48f,
                size.width * 0.72f, waveY + 64f,
                size.width, waveY + 16f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, Color.White)
    }
}
