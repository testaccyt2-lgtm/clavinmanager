package com.sirclavin.clavinmanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The four brand colours from design.md. Nothing else is used. */
object Brand {
    val Blue = Color(0xFF2563EB)
    val Ink = Color(0xFF0F172A)
    val White = Color(0xFFFFFFFF)
    val Aqua = Color(0xFF2DD4BF)
}

@Immutable
class ClavinColors(
    val dark: Boolean,
    val bg: Color,
    val ink: Color,
    val muted: Color,
    val card: Color,
    val border: Color,
    val dialog: Color,
    val blueText: Color
)

private val LightPalette = ClavinColors(
    dark = false,
    bg = Brand.White,
    ink = Brand.Ink,
    muted = Brand.Ink.copy(alpha = 0.62f),
    card = Brand.Blue.copy(alpha = 0.06f),
    border = Brand.Ink.copy(alpha = 0.08f),
    dialog = Brand.White,
    blueText = Brand.Blue
)

private val DarkPalette = ClavinColors(
    dark = true,
    bg = Brand.Ink,
    ink = Brand.White,
    muted = Brand.White.copy(alpha = 0.68f),
    card = Brand.White.copy(alpha = 0.07f),
    border = Brand.White.copy(alpha = 0.12f),
    dialog = lerp(Brand.Ink, Brand.White, 0.09f),
    // Blue text on the dark background is lightened so it stays readable.
    blueText = lerp(Brand.Blue, Brand.White, 0.45f)
)

val LocalClavin = staticCompositionLocalOf { LightPalette }

object Clavin {
    val colors: ClavinColors
        @Composable
        @ReadOnlyComposable
        get() = LocalClavin.current
}

@Composable
fun ClavinTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = Brand.Blue, onPrimary = Brand.White,
            secondary = Brand.Aqua, onSecondary = Brand.Ink,
            background = Brand.Ink, onBackground = Brand.White,
            surface = Brand.Ink, onSurface = Brand.White
        )
    } else {
        lightColorScheme(
            primary = Brand.Blue, onPrimary = Brand.White,
            secondary = Brand.Aqua, onSecondary = Brand.Ink,
            background = Brand.White, onBackground = Brand.Ink,
            surface = Brand.White, onSurface = Brand.Ink
        )
    }
    CompositionLocalProvider(LocalClavin provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
