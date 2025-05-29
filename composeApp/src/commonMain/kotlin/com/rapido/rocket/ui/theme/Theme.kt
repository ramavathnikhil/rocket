package com.rapido.rocket.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Yellow and White Color Palette
private val Yellow = Color(0xFFFFD700) // Gold yellow
private val YellowVariant = Color(0xFFFFC107) // Amber yellow
private val LightYellow = Color(0xFFFFF9C4) // Very light yellow
private val DarkYellow = Color(0xFFFF8F00) // Dark amber
private val White = Color(0xFFFFFFFF)
private val OffWhite = Color(0xFFFFFBF0) // Slightly warm white
private val LightGray = Color(0xFFF5F5F5)
private val DarkGray = Color(0xFF424242)
private val Black = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = Yellow,
    onPrimary = Black,
    primaryContainer = LightYellow,
    onPrimaryContainer = DarkYellow,
    
    secondary = YellowVariant,
    onSecondary = Black,
    secondaryContainer = LightYellow,
    onSecondaryContainer = DarkYellow,
    
    tertiary = DarkYellow,
    onTertiary = White,
    tertiaryContainer = LightYellow,
    onTertiaryContainer = DarkYellow,
    
    error = Color(0xFFD32F2F),
    onError = White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFC62828),
    
    background = White,
    onBackground = Black,
    
    surface = White,
    onSurface = Black,
    surfaceVariant = OffWhite,
    onSurfaceVariant = DarkGray,
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = LightGray,
    
    scrim = Color(0x80000000),
    
    inverseSurface = DarkGray,
    inverseOnSurface = White,
    inversePrimary = LightYellow,
    
    surfaceDim = LightGray,
    surfaceBright = White,
    surfaceContainerLowest = White,
    surfaceContainerLow = OffWhite,
    surfaceContainer = LightGray,
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE0E0E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Yellow,
    onPrimary = Black,
    primaryContainer = DarkYellow,
    onPrimaryContainer = LightYellow,
    
    secondary = YellowVariant,
    onSecondary = Black,
    secondaryContainer = DarkYellow,
    onSecondaryContainer = LightYellow,
    
    tertiary = LightYellow,
    onTertiary = Black,
    tertiaryContainer = DarkYellow,
    onTertiaryContainer = LightYellow,
    
    error = Color(0xFFEF5350),
    onError = Black,
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFEBEE),
    
    background = Color(0xFF121212),
    onBackground = White,
    
    surface = Color(0xFF121212),
    onSurface = White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE0E0E0),
    
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF424242),
    
    scrim = Color(0x80000000),
    
    inverseSurface = White,
    inverseOnSurface = Black,
    inversePrimary = DarkYellow,
    
    surfaceDim = Color(0xFF0F0F0F),
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF363636)
)

@Composable
fun RapidoRocketTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
} 