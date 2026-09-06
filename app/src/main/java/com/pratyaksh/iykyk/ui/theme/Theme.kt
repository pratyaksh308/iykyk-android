package com.pratyaksh.iykyk.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class IykykColors(
    val appBackground: Color,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val ink: Color,
    val mutedInk: Color,
    val primaryContainer: Color,
    val primaryFixed: Color,
    val secondaryFixed: Color,
    val tertiaryFixed: Color,
    val secondaryContainer: Color,
    val amberSparkle: Color,
    val coral: Color,
    val purple: Color,
    val yellow: Color,
    val teal: Color
)

val LightIykykColors = IykykColors(
    appBackground = AppBackgroundLight,
    surface = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    ink = InkLight,
    mutedInk = MutedInkLight,
    primaryContainer = PrimaryContainerLight,
    primaryFixed = PrimaryFixedLight,
    secondaryFixed = SecondaryFixedLight,
    tertiaryFixed = TertiaryFixedLight,
    secondaryContainer = SecondaryContainerLight,
    amberSparkle = AmberSparkleLight,
    coral = CoralLight,
    purple = PurpleLight,
    yellow = YellowLight,
    teal = TealLight
)

val DarkIykykColors = IykykColors(
    appBackground = AppBackgroundDark,
    surface = SurfaceDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    ink = InkDark,
    mutedInk = MutedInkDark,
    primaryContainer = PrimaryContainerDark,
    primaryFixed = PrimaryFixedDark,
    secondaryFixed = SecondaryFixedDark,
    tertiaryFixed = TertiaryFixedDark,
    secondaryContainer = SecondaryContainerDark,
    amberSparkle = AmberSparkleDark,
    coral = CoralDark,
    purple = PurpleDark,
    yellow = YellowDark,
    teal = TealDark
)

val LocalIykykColors = staticCompositionLocalOf { LightIykykColors }

object IykykTheme {
    val colors: IykykColors
        @Composable
        get() = LocalIykykColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = CoralDark,
    secondary = PurpleDark,
    tertiary = YellowDark,
    background = AppBackgroundDark,
    surface = SurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = CoralLight,
    secondary = PurpleLight,
    tertiary = YellowLight,
    background = AppBackgroundLight,
    surface = SurfaceLight
)

@Composable
fun IykykTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val iykykColors = if (darkTheme) DarkIykykColors else LightIykykColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = iykykColors.appBackground.toArgb()
            window.navigationBarColor = iykykColors.appBackground.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIykykColors provides iykykColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
