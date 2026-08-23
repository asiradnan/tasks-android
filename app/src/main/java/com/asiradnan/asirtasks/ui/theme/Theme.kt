package com.asiradnan.asirtasks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Medium greys for container surfaces (cards, dialogs, text fields, etc.)
private val DarkContainer = Color(0xFF1C1C1C)
private val DarkContainerHigh = Color(0xFF2A2A2A)
private val DarkContainerLow = Color(0xFF141414)

private val LightContainer = Color(0xFFE3E3E3)
private val LightContainerHigh = Color(0xFFD5D5D5)
private val LightContainerLow = Color(0xFFEFEFEF)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    outline = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White,

    // Container colors — medium grey instead of pure black
    primaryContainer = DarkContainerHigh,
    onPrimaryContainer = Color.White,
    secondaryContainer = DarkContainer,
    onSecondaryContainer = Color.White,
    tertiaryContainer = DarkContainer,
    onTertiaryContainer = Color.White,
    surfaceContainer = DarkContainer,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHigh,
    surfaceContainerLowest = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color.Black,
    onSecondary = Color.White,
    tertiary = Color.Black,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    outline = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,

    // Container colors — medium grey instead of pure white
    primaryContainer = LightContainerHigh,
    onPrimaryContainer = Color.Black,
    secondaryContainer = LightContainer,
    onSecondaryContainer = Color.Black,
    tertiaryContainer = LightContainer,
    onTertiaryContainer = Color.Black,
    surfaceContainer = LightContainer,
    surfaceContainerLow = LightContainerLow,
    surfaceContainerHigh = LightContainerHigh,
    surfaceContainerHighest = LightContainerHigh,
    surfaceContainerLowest = Color.White,
)

private val SyncedGreenLight = Color(0xFF2E7D32)
private val ErrorRedLight = Color(0xFFC62828)

private val SyncedGreenDark = Color(0xFF81C784)
private val ErrorRedDark = Color(0xFFE57373)

private val MutedGreyLight = Color(0xFF9E9E9E)  // grey 500
private val MutedGreyDark = Color(0xFF8A8A8A)   // slightly warmer/lighter for dark bg legibility

data class SyncStatusColors(
    val synced: Color,
    val error: Color,
    val muted: Color
)

private val LightSyncColors = SyncStatusColors(
    synced = SyncedGreenLight,
    error = ErrorRedLight,
    muted = MutedGreyLight
)

private val DarkSyncColors = SyncStatusColors(
    synced = SyncedGreenDark,
    error = ErrorRedDark,
    muted = MutedGreyDark
)

val LocalSyncStatusColors = staticCompositionLocalOf { LightSyncColors }

@Composable
fun AsirTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val syncColors = if (darkTheme) DarkSyncColors else LightSyncColors

    CompositionLocalProvider(LocalSyncStatusColors provides syncColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object AppTheme {
    val syncColors: SyncStatusColors
        @Composable get() = LocalSyncStatusColors.current
}