package com.rproject.chitchat

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.rproject.chitchat.service.MessageListenerService
import com.rproject.chitchat.ui.navigation.ChitchatNavGraph
import com.rproject.chitchat.ui.navigation.Screen
import com.rproject.chitchat.ui.theme.ChitchatTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start local background listener for notifications
        startService(Intent(this, MessageListenerService::class.java))

        setContent {
            ChitchatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    // Start at Splash to check auth state reliably
                    ChitchatNavGraph(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    )
                }
            }
        }
    }
}
