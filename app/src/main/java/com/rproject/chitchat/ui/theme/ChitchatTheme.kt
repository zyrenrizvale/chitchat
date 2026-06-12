package com.rproject.chitchat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Auth Gradient ──────────────────────────────────────────────────────────────
val AuthGradientStart = Color(0xFF4B78F5)
val AuthGradientMid   = Color(0xFF7B5CF6)
val AuthGradientEnd   = Color(0xFF9B4FF0)

// ── Brand ─────────────────────────────────────────────────────────────────────
val BrandPurple       = Color(0xFF6C63FF)
val BrandPurpleLight  = Color(0xFFEEECFF)
val BrandPurpleDark   = Color(0xFF4A42CC)
val BrandBlue         = Color(0xFF4B78F5)

// ── App Surfaces ──────────────────────────────────────────────────────────────
val AppBackground     = Color(0xFFF5F6FA)
val SurfaceWhite      = Color(0xFFFFFFFF)
val SurfaceGray       = Color(0xFFF3F4F8)
val InputBg           = Color(0xFFF3F4F8)
val BorderColor       = Color(0xFFE8E9F0)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary       = Color(0xFF2D3142)
val TextSecondary     = Color(0xFF9094A0)
val TextHint          = Color(0xFFBBBDC8)

// ── Misc ──────────────────────────────────────────────────────────────────────
val ErrorColor        = Color(0xFFE53E3E)
val SuccessGreen      = Color(0xFF48BB78)
val DividerColor      = Color(0xFFF0F1F5)
val SentBubble        = BrandPurple
val ReceivedBubble    = SurfaceWhite

private val LightColorScheme = lightColorScheme(
    primary            = BrandPurple,
    onPrimary          = Color.White,
    primaryContainer   = BrandPurpleLight,
    onPrimaryContainer = BrandPurpleDark,
    secondary          = BrandBlue,
    onSecondary        = Color.White,
    background         = AppBackground,
    onBackground       = TextPrimary,
    surface            = SurfaceWhite,
    onSurface          = TextPrimary,
    surfaceVariant     = SurfaceGray,
    onSurfaceVariant   = TextSecondary,
    outline            = BorderColor,
    error              = ErrorColor,
    onError            = Color.White,
)

@Composable
fun ChitchatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
