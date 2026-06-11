package com.rproject.chitchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rproject.chitchat.ui.screens.home.HomeScreen
import com.rproject.chitchat.ui.screens.login.LoginScreen
import com.rproject.chitchat.ui.screens.otp.OtpScreen
import com.rproject.chitchat.ui.screens.profile.ProfileSetupScreen
import com.rproject.chitchat.ui.screens.chat.ChatScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp/{verificationId}") {
        fun createRoute(verificationId: String) = "otp/$verificationId"
    }
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object Chat : Screen("chat/{userId}") {
        fun createRoute(userId: String) = "chat/$userId"
    }
}

@Composable
fun ChitchatNavGraph(navController: NavHostController, startDestination: String = Screen.Login.route) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToOtp = { verificationId ->
                    navController.navigate(Screen.Otp.createRoute(verificationId))
                }
            )
        }
        composable(route = Screen.Otp.route) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            OtpScreen(
                verificationId = verificationId,
                onVerificationSuccess = { isNewUser ->
                    if (isNewUser) {
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(route = Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToChat = { userId ->
                    navController.navigate(Screen.Chat.createRoute(userId))
                }
            )
        }
        composable(route = Screen.Chat.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ChatScreen(
                otherUserId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
