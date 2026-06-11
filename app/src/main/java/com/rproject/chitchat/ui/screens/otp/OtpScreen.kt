package com.rproject.chitchat.ui.screens.otp

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase

@Composable
fun OtpScreen(verificationId: String, onVerificationSuccess: (Boolean) -> Unit) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Masukkan Kode OTP", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) otpCode = it },
            label = { Text("6 Digit OTP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
                                    dbRef.get().addOnSuccessListener { snapshot ->
                                        isLoading = false
                                        val hasProfile = snapshot.child("name").exists()
                                        onVerificationSuccess(!hasProfile)
                                    }.addOnFailureListener {
                                        isLoading = false
                                        onVerificationSuccess(true) // asumsikan user baru jika gagal ambil profil
                                    }
                                } else {
                                    isLoading = false
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "OTP Salah atau Kadaluarsa", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = otpCode.length == 6 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Verifikasi")
            }
        }
    }
}
