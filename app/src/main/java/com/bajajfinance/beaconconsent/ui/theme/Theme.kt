package com.bajajfinance.beaconconsent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// BFL brand palette
val BflNavy = Color(0xFF00133A)
val BflNavyBand = Color(0xFF0B1F3A)
val BflCyan = Color(0xFF00B0F0)
val BflGold = Color(0xFFFFC000)
val BflBlue = Color(0xFF2E75B6)
val SurfaceLight = Color(0xFFF4F6F9)

private val Light = lightColorScheme(
    primary = BflNavy,
    onPrimary = Color.White,
    secondary = BflBlue,
    tertiary = BflCyan,
    background = SurfaceLight,
    surface = Color.White,
    onSurface = Color(0xFF16294A),
)

private val Dark = darkColorScheme(
    primary = BflCyan,
    onPrimary = BflNavy,
    secondary = BflBlue,
    tertiary = BflGold,
    background = BflNavy,
    surface = BflNavyBand,
    onSurface = Color(0xFFE7ECF5),
)

@Composable
fun BeaconConsentTheme(content: @Composable () -> Unit) {
    // Locked to the Light theme regardless of the device's dark-mode setting.
    MaterialTheme(
        colorScheme = Light,
        content = content
    )
}
