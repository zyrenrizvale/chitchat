package com.rproject.chitchat.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rproject.chitchat.ui.theme.AuthGradientEnd
import com.rproject.chitchat.ui.theme.AuthGradientStart
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit, onNavigateToWelcome: () -> Unit, onNavigateToProfileSetup: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) // minimum delay for logo display
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Check if profile exists
            FirebaseDatabase.getInstance().getReference("users").child(user.uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.child("name").exists()) {
                        onNavigateToHome()
                    } else {
                        onNavigateToProfileSetup()
                    }
                }
                .addOnFailureListener {
                    onNavigateToWelcome()
                }
        } else {
            onNavigateToWelcome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AuthGradientStart, AuthGradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Forum, null, tint = Color.White, modifier = Modifier.fillMaxSize())
        }
    }
}
