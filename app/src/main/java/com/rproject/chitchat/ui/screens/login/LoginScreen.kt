package com.rproject.chitchat.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.rproject.chitchat.ui.components.AuthLayout
import com.rproject.chitchat.ui.theme.*
import java.util.concurrent.TimeUnit

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onNavigateToOtp: (String) -> Unit) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
                "Welcome back !",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Phone input matching reference style
            TextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it.filter { c -> c.isDigit() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                shape = RoundedCornerShape(28.dp),
                placeholder = {
                    Text("Phone Number", color = TextHint)
                },
                leadingIcon = {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+62", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = BorderColor)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Spacer for alignment to match reference layout
            Spacer(modifier = Modifier.weight(1f))

            // Outline Login Button
            OutlinedButton(
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
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, BrandBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandBlue, strokeWidth = 2.dp)
                } else {
                    Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    )
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
