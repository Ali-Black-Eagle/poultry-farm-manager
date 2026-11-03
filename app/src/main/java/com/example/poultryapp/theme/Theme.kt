package com.example.poultryapp.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FarmGreenPrimaryDark,
    onPrimary = FarmGreenOnPrimaryDark,
    primaryContainer = FarmGreenPrimaryContainerDark,
    onPrimaryContainer = FarmGreenOnPrimaryContainerDark,
    secondary = FarmGreenSecondaryDark,
    onSecondary = FarmGreenOnSecondaryDark,
    secondaryContainer = FarmGreenSecondaryContainerDark,
    onSecondaryContainer = FarmGreenOnSecondaryContainerDark,
    tertiary = FarmAmberTertiaryDark,
    onTertiary = FarmAmberOnTertiaryDark,
    tertiaryContainer = FarmAmberTertiaryContainerDark,
    onTertiaryContainer = FarmAmberOnTertiaryContainerDark,
    background = FarmBackgroundDark,
    onBackground = FarmOnBackgroundDark,
    surface = FarmSurfaceDark,
    onSurface = FarmOnSurfaceDark,
    surfaceVariant = FarmSurfaceVariantDark,
    onSurfaceVariant = FarmOnSurfaceVariantDark,
    error = FarmErrorDark,
    onError = FarmOnErrorDark,
    errorContainer = FarmErrorContainerDark,
    onErrorContainer = FarmOnErrorContainerDark,
    outline = FarmTextTertiaryDark,
    outlineVariant = FarmBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = FarmGreenPrimary,
    onPrimary = FarmGreenOnPrimary,
    primaryContainer = FarmGreenPrimaryContainer,
    onPrimaryContainer = FarmGreenOnPrimaryContainer,
    secondary = FarmGreenSecondary,
    onSecondary = FarmGreenOnSecondary,
    secondaryContainer = FarmGreenSecondaryContainer,
    onSecondaryContainer = FarmGreenOnSecondaryContainer,
    tertiary = FarmAmberTertiary,
    onTertiary = FarmAmberOnTertiary,
    tertiaryContainer = FarmAmberTertiaryContainer,
    onTertiaryContainer = FarmAmberOnTertiaryContainer,
    background = FarmBackgroundLight,
    onBackground = FarmOnBackgroundLight,
    surface = FarmSurfaceLight,
    onSurface = FarmOnSurfaceLight,
    surfaceVariant = FarmSurfaceVariantLight,
    onSurfaceVariant = FarmOnSurfaceVariantLight,
    error = FarmError,
    onError = FarmOnError,
    errorContainer = FarmErrorContainer,
    onErrorContainer = FarmOnErrorContainer,
    outline = FarmTextTertiaryLight,
    outlineVariant = FarmBorderLight
)

@Composable
fun PoultryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to our custom farm aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
