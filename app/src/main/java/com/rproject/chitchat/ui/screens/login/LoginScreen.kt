package com.rproject.chitchat.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
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

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login ke Chitchat", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Nomor Telepon (ex: +628...)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (phoneNumber.isNotBlank()) {
                    isLoading = true
                    sendVerificationCode(
                        context = context,
                        phoneNumber = phoneNumber,
                        onCodeSent = { verificationId ->
                            isLoading = false
                            onNavigateToOtp(verificationId)
                        },
                        onFailed = { exception ->
                            isLoading = false
                            Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Kirim OTP")
            }
        }
    }
}

private fun sendVerificationCode(
    context: Context,
    phoneNumber: String,
    onCodeSent: (String) -> Unit,
    onFailed: (Exception) -> Unit
) {
    val activity = context.findActivity() ?: return
    val auth = FirebaseAuth.getInstance()
    
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // Can handle auto-verify here if needed
        }

        override fun onVerificationFailed(e: FirebaseException) {
            onFailed(e)
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
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
