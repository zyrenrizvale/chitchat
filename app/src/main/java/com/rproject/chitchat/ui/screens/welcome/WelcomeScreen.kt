package com.rproject.chitchat.ui.screens.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rproject.chitchat.ui.theme.*

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "bubble")
    val bubble1Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -18f, label = "b1",
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val bubble2Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 14f, label = "b2",
        animationSpec = infiniteRepeatable(tween(2900, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val bubble3Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f, label = "b3",
        animationSpec = infiniteRepeatable(tween(2100, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    val btnScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f, label = "btnScale",
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ChitchatBgDark, ChitchatSurfaceDark, Color(0xFF1A1830))
                )
            )
    ) {
        // Decorative background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ChitchatPurple.copy(alpha = 0.08f),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.85f, size.height * 0.1f)
            )
            drawCircle(
                color = ChitchatPurple.copy(alpha = 0.06f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.1f, size.height * 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Illustration — animated chat bubbles
            Box(
                modifier = Modifier
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(ChitchatPurple.copy(alpha = 0.12f))
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ChitchatPurple.copy(alpha = 0.4f), ChitchatPurpleDark.copy(alpha = 0.2f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💬", fontSize = 56.sp)
                }

                // Floating bubble 1 (sent, top right)
                Box(
                    modifier = Modifier
                        .offset(x = 68.dp, y = (-44 + bubble1Y).dp)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                        .background(ChitchatPurple)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Hei! 👋", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Floating bubble 2 (received, bottom left)
                Box(
                    modifier = Modifier
                        .offset(x = (-64).dp, y = (52 + bubble2Y).dp)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                        .background(ChitchatSurface2Dark)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Halo! 😊", color = ChitchatOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // Floating bubble 3 (sent, bottom right)
                Box(
                    modifier = Modifier
                        .offset(x = 56.dp, y = (72 + bubble3Y).dp)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                        .background(ChitchatPurpleDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Apa kabar?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Text content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Chitchat",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        brush = Brush.horizontalGradient(
                            colors = listOf(ChitchatPurpleLight, ChitchatPurple)
                        )
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chat bebas, aman, dan cepat\nbersama siapa saja.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChitchatOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            // CTA Button
            Column(
                modifier = Modifier.padding(bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .scale(btnScale),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChitchatPurple
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        "Mulai Sekarang",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Gratis. Tanpa iklan. Selamanya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChitchatOnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
