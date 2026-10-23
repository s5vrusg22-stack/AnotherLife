package com.anotherlife.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Midnight = Color(0xFF101218)
val AtelierSurface = Color(0xFF1C202A)
val Elevated = Color(0xFF353B4B)
val Lavender = Color(0xFFB5A1DC)
val OffWhite = Color(0xFFE9E5F2)

private val colors = darkColorScheme(
    primary = Lavender, onPrimary = Midnight,
    background = Midnight, onBackground = OffWhite,
    surface = AtelierSurface, onSurface = OffWhite,
    surfaceVariant = Elevated, onSurfaceVariant = OffWhite
)

@Composable
fun AnotherLifeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
