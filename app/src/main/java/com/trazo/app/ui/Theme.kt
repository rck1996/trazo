package com.trazo.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.trazo.app.data.AppSettings
import com.trazo.app.data.ThemePreference

data class TrazoPalette(
    val paper: Color, val raised: Color, val ink: Color, val muted: Color,
    val coral: Color, val mustard: Color, val leaf: Color, val sky: Color, val lavender: Color
)

private val LightPalette = TrazoPalette(
    Color(0xFFFFF9EB), Color(0xFFFFFDF7), Color(0xFF272522), Color(0xFF6F6961),
    Color(0xFFE9654B), Color(0xFFF2B84B), Color(0xFF43866A), Color(0xFF81AFC2), Color(0xFFA58BB8)
)
private val DarkPalette = TrazoPalette(
    Color(0xFF141311), Color(0xFF24211E), Color(0xFFFFF7E8), Color(0xFFD2C8BA),
    Color(0xFFFF836A), Color(0xFFF4C668), Color(0xFF71B996), Color(0xFF7EB5CC), Color(0xFFB99BCB)
)
private val LocalTrazoPalette = staticCompositionLocalOf { LightPalette }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalTrazoHaptics = staticCompositionLocalOf { true }
val LocalMinimalMode = staticCompositionLocalOf { false }

val Paper: Color @Composable get() = LocalTrazoPalette.current.paper
val PaperRaised: Color @Composable get() = LocalTrazoPalette.current.raised
val Ink: Color @Composable get() = LocalTrazoPalette.current.ink
val MutedInk: Color @Composable get() = LocalTrazoPalette.current.muted
val Coral: Color @Composable get() = LocalTrazoPalette.current.coral
val Mustard: Color @Composable get() = LocalTrazoPalette.current.mustard
val Leaf: Color @Composable get() = LocalTrazoPalette.current.leaf
val Sky: Color @Composable get() = LocalTrazoPalette.current.sky
val Lavender: Color @Composable get() = LocalTrazoPalette.current.lavender

@Composable
fun TrazoTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    val colors = if (dark) darkColorScheme(
        primary = palette.coral, secondary = palette.leaf, tertiary = palette.mustard,
        background = palette.paper, onBackground = palette.ink,
        surface = palette.raised, onSurface = palette.ink, outline = palette.muted
    ) else lightColorScheme(
        primary = palette.coral, onPrimary = Color.White,
        secondary = palette.leaf, onSecondary = Color.White, tertiary = palette.mustard,
        background = palette.paper, onBackground = palette.ink,
        surface = palette.raised, onSurface = palette.ink, outline = palette.ink
    )
    val density = LocalDensity.current
    val scaledDensity = Density(density.density, if (settings.largeText) density.fontScale * 1.15f else density.fontScale)
    CompositionLocalProvider(
        LocalTrazoPalette provides palette,
        LocalReducedMotion provides (settings.reducedMotion || settings.minimalMode),
        LocalTrazoHaptics provides settings.haptics,
        LocalMinimalMode provides settings.minimalMode,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = MaterialTheme.typography.copy(
                displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 37.sp, letterSpacing = (-1).sp, color = palette.ink),
                headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = (-0.5).sp, color = palette.ink),
                titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 23.sp, color = palette.ink),
                titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = palette.ink),
                bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = palette.ink),
                labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.ink)
            ), content = content
        )
    }
}
