package com.toan.codraw.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium color palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Accent gradients (Brighter and more vibrant)
val GradientStart = Color(0xFF6A82FB)
val GradientEnd = Color(0xFFFC5C7D)
val GradientMint = Color(0xFF00E5FF)
val CardGradientStart = Color(0xFF4CB8C4)
val CardGradientEnd = Color(0xFF3CD3AD)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A82FB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E7FF),
    onPrimaryContainer = Color(0xFF1E2F7C),
    secondary = Color(0xFFFC5C7D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1E8),
    onSecondaryContainer = Color(0xFF6C1B30),
    tertiary = Color(0xFF00E5FF),
    onTertiary = Color(0xFF00444D),
    tertiaryContainer = Color(0xFFB5FAFF),
    onTertiaryContainer = Color(0xFF002226),
    background = Color(0xFFF4F7FF), // Bright bluish-white
    onBackground = Color(0xFF2B3A67), // Dark navy instead of black
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B3A67), // Dark navy
    surfaceVariant = Color(0xFFE8EEFC),
    onSurfaceVariant = Color(0xFF4A5777), // Lighter navy
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme( // Deep colorful navy instead of black for dark mode
    primary = Color(0xFF8DA3FF),
    onPrimary = Color(0xFF1E2F7C),
    primaryContainer = Color(0xFF354897),
    onPrimaryContainer = Color(0xFFE2E7FF),
    secondary = Color(0xFFFF8FA3),
    onSecondary = Color(0xFF6C1B30),
    secondaryContainer = Color(0xFF8F2944),
    onSecondaryContainer = Color(0xFFFFE1E8),
    tertiary = Color(0xFF5DF2FF),
    onTertiary = Color(0xFF00444D),
    tertiaryContainer = Color(0xFF006673),
    onTertiaryContainer = Color(0xFFB5FAFF),
    background = Color(0xFF1A1F38), // Deep navy
    onBackground = Color(0xFFE2E7FF),
    surface = Color(0xFF222845), // Slightly lighter navy
    onSurface = Color(0xFFE2E7FF),
    surfaceVariant = Color(0xFF2E385D),
    onSurfaceVariant = Color(0xFFC7D0EA),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun CodrawTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
