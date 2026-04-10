package com.example.securechat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand colors ──────────────────────────────────────────────────────────────
val Blue600    = Color(0xFF0084FF)
val Blue700    = Color(0xFF006AE0)
val Blue100    = Color(0xFFD6EAFF)

val GrayBg     = Color(0xFFF0F2F5)
val GrayBorder = Color(0xFFE4E6EB)
val GrayMuted  = Color(0xFF65676B)
val GrayDark   = Color(0xFF050505)

val GreenOnline = Color(0xFF31A24C)
val RedError    = Color(0xFFFF3B30)

// ── Light color scheme ────────────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary          = Blue600,
    onPrimary        = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue700,
    secondary        = GrayMuted,
    onSecondary      = Color.White,
    background       = Color.White,
    onBackground     = GrayDark,
    surface          = Color.White,
    onSurface        = GrayDark,
    surfaceVariant   = GrayBg,
    onSurfaceVariant = GrayMuted,
    outline          = GrayBorder,
    error            = RedError,
    onError          = Color.White
)

// ── Dark color scheme ─────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary          = Blue600,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF004B8D),
    onPrimaryContainer = Blue100,
    secondary        = Color(0xFF8A8D91),
    background       = Color(0xFF18191A),
    onBackground     = Color(0xFFE4E6EB),
    surface          = Color(0xFF242526),
    onSurface        = Color(0xFFE4E6EB),
    surfaceVariant   = Color(0xFF3A3B3C),
    onSurfaceVariant = Color(0xFFB0B3B8),
    outline          = Color(0xFF3A3B3C),
    error            = RedError,
    onError          = Color.White
)

// ── App Theme ─────────────────────────────────────────────────────────────────
@Composable
fun ConnectNowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}

// ── Typography ────────────────────────────────────────────────────────────────
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight  = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight  = 18.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp
    )
)

// ── Shapes ────────────────────────────────────────────────────────────────────
val AppShapes = Shapes(
    small       = RoundedCornerShape(8.dp),
    medium      = RoundedCornerShape(12.dp),
    large       = RoundedCornerShape(16.dp),
    extraLarge  = RoundedCornerShape(24.dp)
)