package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ObsidianGoldColorScheme = darkColorScheme(
    primary = PrimaryGold,
    onPrimary = OnPrimaryGold,
    primaryContainer = PrimaryGoldenContainer,
    onPrimaryContainer = OnPrimaryGoldenContainer,
    secondary = SecondaryGold,
    onSecondary = OnSecondaryGold,
    secondaryContainer = SecondaryGoldContainer,
    onSecondaryContainer = OnSecondaryGoldContainer,
    background = DeepDarkBackground,
    onBackground = OnSurfaceGold,
    surface = Level2DarkSurface,
    onSurface = OnSurfaceGold,
    surfaceVariant = Level2DarkSurface,
    onSurfaceVariant = OnSurfaceGoldVariant,
    outline = OutlineGold,
    outlineVariant = OutlineGoldVariant,
    error = DarkError,
    onError = OnDarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = OnDarkErrorContainer
)

private val ChampagneGoldLightColorScheme = lightColorScheme(
    primary = Color(0xFFC59B27),       // Elegant dark champagne gold for light theme
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFF1D0),
    onPrimaryContainer = Color(0xFF554300),
    secondary = Color(0xFF6B6A65),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7F6EE),
    onSecondaryContainer = Color(0xFF1E1E1C),
    background = Color(0xFFFAF9F5),    // Clean light warm linen/champagne sheet
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F4EC),
    onSurfaceVariant = Color(0xFF7D7765),
    outline = Color(0xFF8A8471),
    outlineVariant = Color(0xFFDCD8CB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        ObsidianGoldColorScheme
    } else {
        ChampagneGoldLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
