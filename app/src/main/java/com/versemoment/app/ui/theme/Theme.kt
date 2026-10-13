package com.versemoment.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD8B46A),
    secondary = Color(0xFF9EAD9A),
    tertiary = Color(0xFF172235),
    background = Color(0xFF101827),
    surface = Color(0xFF172235),
    onPrimary = Color(0xFF101827),
    onBackground = Color(0xFFF7F3EA),
    onSurface = Color(0xFFF7F3EA),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD8B46A),
    secondary = Color(0xFF9EAD9A),
    tertiary = Color(0xFF172235),
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF101827),
    onBackground = Color(0xFF101827),
    onSurface = Color(0xFF252B32),
)

@Composable
fun VerseMomentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
