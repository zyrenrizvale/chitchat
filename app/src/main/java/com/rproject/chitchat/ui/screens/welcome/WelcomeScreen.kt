package com.rproject.chitchat.ui.screens.welcome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rproject.chitchat.ui.components.AuthLayout
import com.rproject.chitchat.ui.theme.*

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    AuthLayout(
        headerContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CHITCHAT",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
        },
        bodyContent = {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Welcome !",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Connect with anyone,\nanytime, anywhere.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Primary Button (Animated Bounce)
            BounceButton(onClick = onGetStarted, text = "Create Account", isPrimary = true)

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Button (Animated Bounce)
            BounceButton(onClick = onGetStarted, text = "Login", isPrimary = false)

            Spacer(modifier = Modifier.height(48.dp))
        }
    )
}

@Composable
fun BounceButton(onClick: () -> Unit, text: String, isPrimary: Boolean) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "bounce")

    Box(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isPrimary) {
                    Brush.horizontalGradient(listOf(BrandPurple, BrandBlue))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable default ripple for custom bounce
                onClick = onClick
            )
            .then(
                if (!isPrimary) Modifier.border(BorderStroke(1.5.dp, BrandBlue), RoundedCornerShape(28.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) Color.White else BrandBlue
        )
    }
}
