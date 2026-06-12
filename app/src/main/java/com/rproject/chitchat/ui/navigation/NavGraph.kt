package com.rproject.chitchat.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.rproject.chitchat.ui.screens.chat.ChatScreen
import com.rproject.chitchat.ui.screens.home.HomeScreen
import com.rproject.chitchat.ui.screens.login.LoginScreen
import com.rproject.chitchat.ui.screens.otp.OtpScreen
import com.rproject.chitchat.ui.screens.profile.ProfileSetupScreen
import com.rproject.chitchat.ui.screens.splash.SplashScreen
import com.rproject.chitchat.ui.screens.welcome.WelcomeScreen
import com.rproject.chitchat.ui.screens.group.CreateGroupScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Otp : Screen("otp/{verificationId}") {
        fun createRoute(verificationId: String) = "otp/$verificationId"
    }
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object Chat : Screen("chat/{userId}") {
        fun createRoute(userId: String) = "chat/$userId"
    }
    object CreateGroup : Screen("create_group")
}

@Composable
fun ChitchatNavGraph(navController: NavHostController, startDestination: String = Screen.Splash.route) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) },
        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(Screen.ProfileSetup.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                }
            )
        }
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
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
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
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
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
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
        composable(route = Screen.CreateGroup.route) {
            CreateGroupScreen(
                onBack = { navController.popBackStack() },
                onGroupCreated = { groupId ->
                    navController.navigate(Screen.Chat.createRoute(groupId)) {
                        popUpTo(Screen.CreateGroup.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
