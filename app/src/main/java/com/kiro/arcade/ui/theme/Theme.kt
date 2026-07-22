package com.kiro.arcade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KiroColorScheme = darkColorScheme(
    primary = KiroAccentViolet,
    secondary = KiroAccentPink,
    tertiary = KiroAccentCyan,
    background = KiroBackgroundBottom,
    surface = KiroBackgroundBottom,
    onPrimary = KiroTextPrimary,
    onBackground = KiroTextPrimary,
    onSurface = KiroTextPrimary
)

@Composable
fun KiroArcadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KiroColorScheme,
        content = content
    )
}
