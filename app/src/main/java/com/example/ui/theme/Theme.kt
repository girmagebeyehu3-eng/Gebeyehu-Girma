package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BoomDarkColorScheme = darkColorScheme(
    primary = BoomCyan,
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF004D48),
    onPrimaryContainer = Color(0xFF70F5EA),
    secondary = BoomCoral,
    onSecondary = Color(0xFF531700),
    secondaryContainer = Color(0xFF792500),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = BoomGold,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BoomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
