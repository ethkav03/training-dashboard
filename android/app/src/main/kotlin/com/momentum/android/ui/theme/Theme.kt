package com.momentum.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Series/status colors don't map to any Material3 ColorScheme role, so they
 * live in their own CompositionLocal alongside MaterialTheme -- the same
 * "extended colors" pattern Compose's own theming samples use. Read via
 * `MomentumTheme.colors.series1` etc.
 */
data class MomentumExtendedColors(
    val series1: Color,
    val series2: Color,
    val series3: Color,
    val series4: Color,
    val series5: Color,
    val series6: Color,
    val series7: Color,
    val series8: Color,
    val statusGood: Color,
    val statusWarning: Color,
    val statusSerious: Color,
    val statusCritical: Color,
    val page: Color,
    val hairline: Color,
    val textSecondary: Color,
    val textMuted: Color,
)

private val LightExtendedColors = MomentumExtendedColors(
    series1 = Series1Light,
    series2 = Series2Light,
    series3 = Series3Light,
    series4 = Series4Light,
    series5 = Series5Light,
    series6 = Series6Light,
    series7 = Series7Light,
    series8 = Series8Light,
    statusGood = StatusGoodLight,
    statusWarning = StatusWarningLight,
    statusSerious = StatusSeriousLight,
    statusCritical = StatusCriticalLight,
    page = PageLight,
    hairline = HairlineLight,
    textSecondary = TextSecondaryLight,
    textMuted = TextMutedLight,
)

private val DarkExtendedColors = MomentumExtendedColors(
    series1 = Series1Dark,
    series2 = Series2Dark,
    series3 = Series3Dark,
    series4 = Series4Dark,
    series5 = Series5Dark,
    series6 = Series6Dark,
    series7 = Series7Dark,
    series8 = Series8Dark,
    statusGood = StatusGoodDark,
    statusWarning = StatusWarningDark,
    statusSerious = StatusSeriousDark,
    statusCritical = StatusCriticalDark,
    page = PageDark,
    hairline = HairlineDark,
    textSecondary = TextSecondaryDark,
    textMuted = TextMutedDark,
)

private val LocalMomentumExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColors = lightColorScheme(
    primary = Series1Light,
    background = PageLight,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = HairlineLight,
    error = StatusCriticalLight,
)

private val DarkColors = darkColorScheme(
    primary = Series1Dark,
    background = PageDark,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = HairlineDark,
    error = StatusCriticalDark,
)

object MomentumTheme {
    val colors: MomentumExtendedColors
        @Composable get() = LocalMomentumExtendedColors.current
}

// Dynamic (wallpaper-derived Material You) color used to be the default
// here, but the web app has no equivalent -- its palette is fixed brand
// colors, not user-wallpaper-derived. Now that visual parity with web
// matters, dynamic color is dropped entirely so a chart series/status color
// means the same thing on both clients rather than varying per device.
@Composable
fun MomentumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalMomentumExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
