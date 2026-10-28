package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF002029),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = JarvisCyanGlow,
    secondary = JarvisBlue,
    onSecondary = Color(0xFF002B4D),
    secondaryContainer = Color(0xFF00437A),
    onSecondaryContainer = Color(0xFFBFE3FF),
    tertiary = JarvisAccentGreen,
    onTertiary = Color(0xFF003919),
    background = JarvisDarkNavy,
    onBackground = TextPrimary,
    surface = JarvisSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = JarvisCardBg,
    onSurfaceVariant = TextSecondary,
    outline = JarvisCardBorder,
    error = JarvisAccentRed,
    onError = Color.White
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
