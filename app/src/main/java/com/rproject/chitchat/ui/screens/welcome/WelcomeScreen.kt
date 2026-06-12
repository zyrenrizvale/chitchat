package com.rproject.chitchat.ui.screens.welcome

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rproject.chitchat.ui.theme.*

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "anim")

    val bubble1Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -14f, label = "b1",
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val bubble2Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 12f, label = "b2",
        animationSpec = infiniteRepeatable(tween(3100, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val bubble3Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f, label = "b3",
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )
    val btnScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.025f, label = "btnScale",
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChitchatBgDark)
    ) {
        // Background decorative arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ChitchatPurple.copy(alpha = 0.10f),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.9f, size.height * 0.08f)
            )
            drawCircle(
                color = ChitchatPurple.copy(alpha = 0.07f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.05f, size.height * 0.88f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // === ILLUSTRATION: Chat Bubbles with Icons ===
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(ChitchatPurple.copy(alpha = 0.10f))
                )
                // Center logo circle
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            Brush.linearGradient(listOf(ChitchatPurple, ChitchatPurpleDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                // Bubble 1 — top right (sent)
                Box(
                    modifier = Modifier
                        .offset(x = 70.dp, y = (-50 + bubble1Y).dp)
                        .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                        .background(ChitchatPurple)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Hei, apa kabar?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Bubble 2 — bottom left (received)
                Box(
                    modifier = Modifier
                        .offset(x = (-68).dp, y = (56 + bubble2Y).dp)
                        .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                        .background(ChitchatSurface2Dark)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Baik, kamu?", color = ChitchatOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Bubble 3 — bottom right (sent)
                Box(
                    modifier = Modifier
                        .offset(x = 60.dp, y = (74 + bubble3Y).dp)
                        .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                        .background(ChitchatPurpleDark)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text("Alhamdulillah!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // === TEXT ===
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Chitchat",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = Brush.horizontalGradient(listOf(ChitchatPurpleLight, ChitchatPurple))
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Terhubung dengan siapa saja,\nkapan saja dan di mana saja.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ChitchatOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(28.dp))

                // Feature pills
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FeaturePill(icon = Icons.Filled.Lock, label = "Aman")
                    FeaturePill(icon = Icons.Filled.Speed, label = "Cepat")
                    FeaturePill(icon = Icons.Filled.Forum, label = "Gratis")
                }
            }

            // === BUTTON ===
            Column(
                modifier = Modifier.padding(bottom = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(btnScale),
                    colors = ButtonDefaults.buttonColors(containerColor = ChitchatPurple),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        "Mulai Sekarang",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Gratis. Tanpa iklan. Selamanya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChitchatOnSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun FeaturePill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ChitchatSurface2Dark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ChitchatPurpleLight,
            modifier = Modifier.size(14.dp)
        )
        Text(label, color = ChitchatOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
