package com.videocall.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Navy,
    background = Navy,
    onBackground = Teal,
    surface = Slate,
    onSurface = ColorTokens.TextPrimary,
    secondary = Teal,
    error = Danger
)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Navy,
    background = ColorTokens.LightBackground,
    onBackground = Navy,
    surface = ColorTokens.LightSurface,
    onSurface = Navy,
    secondary = Teal,
    error = Danger
)

@Composable
fun VideoCallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = VideoCallTypography,
        content = content
    )
}

object ColorTokens {
    val LightBackground = androidx.compose.ui.graphics.Color(0xFFF4F7FB)
    val LightSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    val TextPrimary = androidx.compose.ui.graphics.Color(0xFFE5E9F0)
}

