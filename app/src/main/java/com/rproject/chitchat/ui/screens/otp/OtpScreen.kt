package com.rproject.chitchat.ui.screens.otp

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.rproject.chitchat.ui.components.AuthLayout
import com.rproject.chitchat.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(verificationId: String, onVerificationSuccess: (Boolean) -> Unit) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(60) }
    var timerRunning by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timer > 0) {
                delay(1000L)
                timer--
            }
            timerRunning = false
        }
    }

    AuthLayout(
        headerContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Forum, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("CHITCHAT", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        },
        bodyContent = {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Verify Code",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Enter 6-digit verification code",
                color = TextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 6-box OTP input
            BasicTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it.filter { c -> c.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(6) { idx ->
                            val char = otpCode.getOrNull(idx)
                            val isCurrent = idx == otpCode.length
                            val isFilled = char != null
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.8f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(InputBg)
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = when {
                                            isCurrent -> BrandBlue
                                            isFilled -> BrandBlue.copy(alpha = 0.5f)
                                            else -> BorderColor
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char?.toString() ?: "",
                                    style = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer / Resend
            if (timerRunning) {
                Text(
                    "Resend in ${timer}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                TextButton(
                    onClick = {
                        otpCode = ""
                        timer = 60
                        timerRunning = true
                        Toast.makeText(context, "New code sent", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Resend Code", color = BrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val buttonInteractionSource = remember { MutableInteractionSource() }
            val isPressed by buttonInteractionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "bounce")

            Box(
                modifier = Modifier
                    .scale(scale)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.5.dp, BrandBlue, RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = buttonInteractionSource,
                        indication = null,
                        enabled = otpCode.length == 6 && !isLoading,
                        onClick = {
                            if (otpCode.length == 6) {
                                isLoading = true
                                val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                                FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = task.result?.user
                                            if (user != null) {
                                                FirebaseDatabase.getInstance()
                                                    .getReference("users").child(user.uid)
                                                    .get()
                                                    .addOnSuccessListener { snapshot ->
                                                        isLoading = false
                                                        val hasProfile = snapshot.child("name").exists()
                                                        onVerificationSuccess(!hasProfile)
                                                    }
                                                    .addOnFailureListener {
                                                        isLoading = false
                                                        onVerificationSuccess(true)
                                                    }
                                            } else { isLoading = false }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "Invalid code", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlue, strokeWidth = 2.dp)
                } else {
                    Text(
                        "Verify",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (otpCode.length == 6) BrandBlue else BrandBlue.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    )
}
