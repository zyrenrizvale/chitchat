package com.rproject.chitchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Chitchat Brand Colors
val ChitchatPurple = Color(0xFF6C63FF)
val ChitchatPurpleLight = Color(0xFF9B94FF)
val ChitchatPurpleDark = Color(0xFF4A42CC)
val ChitchatPurpleContainer = Color(0xFF2D2B5E)
val ChitchatBgDark = Color(0xFF0F0E1A)
val ChitchatSurfaceDark = Color(0xFF1C1B2E)
val ChitchatSurface2Dark = Color(0xFF252440)
val ChitchatOnSurface = Color(0xFFE8E6FF)
val ChitchatOnSurfaceVariant = Color(0xFF9E9CB8)
val ChitchatOutline = Color(0xFF3D3B5C)
val ChitchatGreen = Color(0xFF4CAF7D)
val ChitchatError = Color(0xFFCF6679)

private val DarkColorScheme = darkColorScheme(
    primary = ChitchatPurple,
    onPrimary = Color.White,
    primaryContainer = ChitchatPurpleContainer,
    onPrimaryContainer = ChitchatPurpleLight,
    secondary = ChitchatPurpleLight,
    onSecondary = Color.White,
    background = ChitchatBgDark,
    onBackground = ChitchatOnSurface,
    surface = ChitchatSurfaceDark,
    onSurface = ChitchatOnSurface,
    surfaceVariant = ChitchatSurface2Dark,
    onSurfaceVariant = ChitchatOnSurfaceVariant,
    outline = ChitchatOutline,
    error = ChitchatError,
    onError = Color.White,
)

@Composable
fun ChitchatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
