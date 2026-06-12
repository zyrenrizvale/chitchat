package com.rproject.chitchat.ui.screens.otp

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.rproject.chitchat.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(verificationId: String, onVerificationSuccess: (Boolean) -> Unit) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(60) }
    var timerRunning by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Countdown timer
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timer > 0) {
                delay(1000L)
                timer--
            }
            timerRunning = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ChitchatBgDark, ChitchatSurfaceDark, Color(0xFF1A1830))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(ChitchatPurple, ChitchatPurpleDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 38.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Verifikasi Nomor",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ChitchatOnSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Masukkan 6 digit kode yang dikirim\nke nomor telepon kamu",
                style = MaterialTheme.typography.bodyMedium,
                color = ChitchatOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ChitchatSurfaceDark.copy(alpha = 0.85f))
                    .border(1.dp, ChitchatOutline, RoundedCornerShape(24.dp))
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // 6-box OTP Input
                    BasicTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        decorationBox = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(6) { idx ->
                                    val char = otpCode.getOrNull(idx)
                                    val isCurrent = idx == otpCode.length
                                    val isFilled = char != null

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isFilled) ChitchatPurpleContainer
                                                else ChitchatSurface2Dark
                                            )
                                            .border(
                                                width = if (isCurrent || isFilled) 2.dp else 1.dp,
                                                color = when {
                                                    isFilled -> ChitchatPurple
                                                    isCurrent -> ChitchatPurpleLight
                                                    else -> ChitchatOutline
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char?.toString() ?: "",
                                            style = TextStyle(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFilled) ChitchatPurpleLight else ChitchatOnSurface,
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
                            "Kirim ulang dalam ${timer}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChitchatOnSurfaceVariant
                        )
                    } else {
                        TextButton(onClick = {
                            otpCode = ""
                            timer = 60
                            timerRunning = true
                            Toast.makeText(context, "Kode baru telah dikirim", Toast.LENGTH_SHORT).show()
                        }) {
                            Text(
                                "Kirim Ulang Kode",
                                color = ChitchatPurpleLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Verify button
                    Button(
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
                                            } else {
                                                isLoading = false
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "Kode OTP salah atau kadaluarsa", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChitchatPurple),
                        shape = RoundedCornerShape(14.dp),
                        enabled = otpCode.length == 6 && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "Verifikasi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
