package com.example.simpledocumentscanner.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ScannerThemeColor(
    val displayName: String,
    val swatch: Color,
    internal val lightPrimary: Color,
    internal val lightContainer: Color,
    internal val darkPrimary: Color,
    internal val darkContainer: Color,
) {
    OCEAN(
        displayName = "元氣藍",
        swatch = Color(0xFF4967F2),
        lightPrimary = Color(0xFF3F5FC4),
        lightContainer = Color(0xFFDDE2FF),
        darkPrimary = Color(0xFFB9C4FF),
        darkContainer = Color(0xFF284691),
    ),
    FOREST(
        displayName = "森林綠",
        swatch = Color(0xFF10A37F),
        lightPrimary = Color(0xFF087A62),
        lightContainer = Color(0xFFB8F2E2),
        darkPrimary = Color(0xFF62DBC0),
        darkContainer = Color(0xFF005142),
    ),
    SUNSHINE(
        displayName = "陽光黃",
        swatch = Color(0xFFF5B700),
        lightPrimary = Color(0xFF806000),
        lightContainer = Color(0xFFFFE08A),
        darkPrimary = Color(0xFFFFD75A),
        darkContainer = Color(0xFF5C4500),
    ),
    CORAL(
        displayName = "珊瑚橘",
        swatch = Color(0xFFF2674E),
        lightPrimary = Color(0xFFA84332),
        lightContainer = Color(0xFFFFDAD3),
        darkPrimary = Color(0xFFFFB4A7),
        darkContainer = Color(0xFF842B1F),
    ),
    BERRY(
        displayName = "莓果粉",
        swatch = Color(0xFFE34F87),
        lightPrimary = Color(0xFF9A3B63),
        lightContainer = Color(0xFFFFD9E4),
        darkPrimary = Color(0xFFFFB0CA),
        darkContainer = Color(0xFF7B234B),
    ),
    VIOLET(
        displayName = "葡萄紫",
        swatch = Color(0xFF8061C9),
        lightPrimary = Color(0xFF684BA5),
        lightContainer = Color(0xFFEADDFF),
        darkPrimary = Color(0xFFD1BCFF),
        darkContainer = Color(0xFF503487),
    ),
}

private fun scannerLightColors(themeColor: ScannerThemeColor) = lightColorScheme(
    primary = themeColor.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = themeColor.lightContainer,
    onPrimaryContainer = Color(0xFF171B24),
    secondary = Color(0xFF705A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE179),
    onSecondaryContainer = Color(0xFF241A00),
    background = Color(0xFFF8F7FC),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE5E2E9),
    onSurfaceVariant = Color(0xFF47464D),
)

private fun scannerDarkColors(themeColor: ScannerThemeColor) = darkColorScheme(
    primary = themeColor.darkPrimary,
    onPrimary = Color(0xFF182044),
    primaryContainer = themeColor.darkContainer,
    onPrimaryContainer = Color(0xFFE0E5FF),
    secondary = Color(0xFFE8C440),
    onSecondary = Color(0xFF3A3000),
    secondaryContainer = Color(0xFF554600),
    onSecondaryContainer = Color(0xFFFFE179),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E8),
    surface = Color(0xFF191C22),
    onSurface = Color(0xFFE3E2E8),
    surfaceVariant = Color(0xFF44464F),
    onSurfaceVariant = Color(0xFFC7C5CF),
)

private fun customLightColors(seed: Color): androidx.compose.material3.ColorScheme {
    val hsv = seed.toHsv()
    val primary = Color.hsv(hsv[0], 0.72f, 0.68f)
    val container = Color.hsv(hsv[0], 0.24f, 0.97f)
    val secondary = Color.hsv((hsv[0] + 40f) % 360f, 0.58f, 0.58f)
    val secondaryContainer = Color.hsv((hsv[0] + 40f) % 360f, 0.20f, 0.94f)
    val background = Color.hsv(hsv[0], 0.04f, 0.98f)
    val surface = Color.hsv(hsv[0], 0.025f, 1f)
    val surfaceVariant = Color.hsv(hsv[0], 0.09f, 0.91f)
    return lightColorScheme(
        primary = primary,
        onPrimary = primary.contrastingContentColor(),
        primaryContainer = container,
        onPrimaryContainer = container.contrastingContentColor(),
        secondary = secondary,
        onSecondary = secondary.contrastingContentColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastingContentColor(),
        background = background,
        onBackground = background.contrastingContentColor(),
        surface = surface,
        onSurface = surface.contrastingContentColor(),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = surfaceVariant.contrastingContentColor(),
    )
}

private fun customDarkColors(seed: Color): androidx.compose.material3.ColorScheme {
    val hsv = seed.toHsv()
    val primary = Color.hsv(hsv[0], 0.48f, 0.96f)
    val container = Color.hsv(hsv[0], 0.58f, 0.42f)
    val secondary = Color.hsv((hsv[0] + 40f) % 360f, 0.42f, 0.88f)
    val secondaryContainer = Color.hsv((hsv[0] + 40f) % 360f, 0.46f, 0.34f)
    val background = Color.hsv(hsv[0], 0.16f, 0.10f)
    val surface = Color.hsv(hsv[0], 0.14f, 0.14f)
    val surfaceVariant = Color.hsv(hsv[0], 0.18f, 0.29f)
    return darkColorScheme(
        primary = primary,
        onPrimary = primary.contrastingContentColor(),
        primaryContainer = container,
        onPrimaryContainer = container.contrastingContentColor(),
        secondary = secondary,
        onSecondary = secondary.contrastingContentColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastingContentColor(),
        background = background,
        onBackground = background.contrastingContentColor(),
        surface = surface,
        onSurface = surface.contrastingContentColor(),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = surfaceVariant.contrastingContentColor(),
    )
}

private fun Color.toHsv(): FloatArray = FloatArray(3).also {
    android.graphics.Color.colorToHSV(toArgb(), it)
}

private fun Color.contrastingContentColor(): Color =
    if (luminance() > 0.42f) Color(0xFF171717) else Color.White

@Composable
fun SimpleScannerTheme(
    themeColor: ScannerThemeColor,
    customThemeColor: Int?,
    displayMode: DisplayMode,
    content: @Composable () -> Unit,
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (displayMode) {
        DisplayMode.SYSTEM -> systemDarkTheme
        DisplayMode.LIGHT -> false
        DisplayMode.DARK -> true
    }
    val customColor = customThemeColor?.let(::Color)
    val colorScheme = if (customColor != null) {
        if (darkTheme) customDarkColors(customColor) else customLightColors(customColor)
    } else if (darkTheme) {
        scannerDarkColors(themeColor)
    } else {
        scannerLightColors(themeColor)
    }
    val activity = LocalActivity.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            activity?.window?.let { window ->
                window.navigationBarColor = colorScheme.background.toArgb()
                window.isNavigationBarContrastEnforced = false
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
