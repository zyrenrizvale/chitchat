package com.rproject.chitchat.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rproject.chitchat.ui.theme.*
import java.util.concurrent.TimeUnit

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LoginScreen(onNavigateToOtp: (String) -> Unit) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChitchatBgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // App icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(ChitchatPurple, ChitchatPurpleDark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Forum,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Masuk ke Chitchat",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ChitchatOnSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Masukkan nomor telepon kamu\nuntuk menerima kode verifikasi",
                style = MaterialTheme.typography.bodyMedium,
                color = ChitchatOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(44.dp))

            // Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ChitchatSurfaceDark)
                    .border(1.dp, ChitchatOutline, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Text(
                    "Nomor Telepon",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChitchatOnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Input row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChitchatSurface2Dark)
                        .border(
                            width = 1.5.dp,
                            color = if (phoneNumber.isNotEmpty()) ChitchatPurple else ChitchatOutline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prefix
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = ChitchatPurpleLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "+62",
                            color = ChitchatOnSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Divider(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp),
                            color = ChitchatOutline
                        )
                    }

                    TextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = ChitchatOnSurface,
                            unfocusedTextColor = ChitchatOnSurface,
                            cursorColor = ChitchatPurple
                        ),
                        placeholder = {
                            Text("8xx-xxxx-xxxx", color = ChitchatOnSurfaceVariant.copy(alpha = 0.45f), fontSize = 15.sp)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val full = "+62$phoneNumber"
                        if (phoneNumber.length >= 8) {
                            isLoading = true
                            sendVerificationCode(
                                context = context,
                                phoneNumber = full,
                                onCodeSent = { verificationId ->
                                    isLoading = false
                                    onNavigateToOtp(verificationId)
                                },
                                onFailed = { exception ->
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Nomor tidak valid", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChitchatPurple),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Kirim Kode OTP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Dengan melanjutkan, kamu menyetujui\nSyarat & Ketentuan Chitchat.",
                style = MaterialTheme.typography.bodySmall,
                color = ChitchatOnSurfaceVariant.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

fun sendVerificationCode(
    context: Context,
    phoneNumber: String,
    onCodeSent: (String) -> Unit,
    onFailed: (Exception) -> Unit
) {
    val activity = context.findActivity() ?: return
    val auth = FirebaseAuth.getInstance()
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {}
        override fun onVerificationFailed(e: FirebaseException) { onFailed(e) }
        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            onCodeSent(verificationId)
        }
    }
    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)
        .build()
    PhoneAuthProvider.verifyPhoneNumber(options)
}
