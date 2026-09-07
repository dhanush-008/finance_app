package com.borrowercopilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Colour Palette ───────────────────────────────────────────────────────────
// Deep navy + warm gold — trustworthy fintech feel, not generic CRUD

val Navy900   = Color(0xFF0B1437)
val Navy800   = Color(0xFF142055)
val Navy700   = Color(0xFF1E2E70)
val Navy600   = Color(0xFF2A3F8F)
val Teal500   = Color(0xFF1A8C8C)
val Teal400   = Color(0xFF22AAAA)
val Gold500   = Color(0xFFF4A621)
val Gold400   = Color(0xFFF7BC52)
val Slate100  = Color(0xFFF0F2F8)
val Slate200  = Color(0xFFDDE1EE)
val Slate500  = Color(0xFF8B92B3)
val Slate700  = Color(0xFF4A5278)
val White     = Color(0xFFFFFFFF)
val Black     = Color(0xFF0A0A0A)

// Semantic
val Success   = Color(0xFF1DA462)
val Warning   = Color(0xFFF4A621)
val Danger    = Color(0xFFD93B3B)
val DangerLight = Color(0xFFFFF0F0)
val WarningLight = Color(0xFFFFF8EC)
val SuccessLight = Color(0xFFEBFAF3)
val InfoLight  = Color(0xFFEBF1FF)

private val LightColorScheme = lightColorScheme(
    primary         = Navy800,
    onPrimary       = White,
    primaryContainer= InfoLight,
    onPrimaryContainer = Navy800,
    secondary       = Teal500,
    onSecondary     = White,
    tertiary        = Gold500,
    onTertiary      = Black,
    background      = Slate100,
    onBackground    = Navy900,
    surface         = White,
    onSurface       = Navy900,
    surfaceVariant  = Slate200,
    onSurfaceVariant= Slate700,
    outline         = Slate200,
    error           = Danger,
    onError         = White
)

@Composable
fun BorrowerCopilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Using light theme only for clarity — production would add dark mode
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = AppTypography,
        content     = content
    )
}

// ─── Typography ───────────────────────────────────────────────────────────────

val AppTypography = Typography(
    // Big financial numbers
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
        color      = Navy900
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 36.sp,
        color      = Navy900
    ),
    // Section headings
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 30.sp,
        color      = Navy900
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 26.sp,
        color      = Navy900
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 15.sp,
        lineHeight = 22.sp,
        color      = Navy900
    ),
    // Body
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        color      = Navy900
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 22.sp,
        color      = Slate700
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 18.sp,
        color      = Slate500
    ),
    // Labels
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        color      = Navy800
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        color      = Slate700
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        color      = Slate500
    )
)
