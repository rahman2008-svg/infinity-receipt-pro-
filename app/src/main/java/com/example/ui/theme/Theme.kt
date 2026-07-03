package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF79DD97),
    onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF00522B),
    onPrimaryContainer = Color(0xFF97F9B2),
    secondary = Color(0xFF79DD97),
    onSecondary = Color(0xFF00391B),
    secondaryContainer = Color(0xFF00522B),
    onSecondaryContainer = Color(0xFF97F9B2),
    background = Color(0xFF0F120F),
    onBackground = Color(0xFFE1E3E0),
    surface = Color(0xFF1B1E1B),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF2C322C),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF404943)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoGreenPrimary,
    onPrimary = BentoGreenOnPrimary,
    primaryContainer = BentoGreenContainer,
    onPrimaryContainer = BentoGreenOnContainer,
    secondary = BentoGreenPrimary,
    onSecondary = BentoGreenOnPrimary,
    secondaryContainer = BentoGreenContainer,
    onSecondaryContainer = BentoGreenOnContainer,
    background = BentoBackground,
    onBackground = BentoOnBackground,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    outline = BentoBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to enforce the custom Bento Grid branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
