package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = SagePrimary,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = OnSageContainer,
    secondary = AmberWarning,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = UndersizedSlate,
    onTertiary = Color.White,
    tertiaryContainer = UndersizedContainer,
    background = StoneBackground,
    onBackground = TextPrimary,
    surface = StoneSurface,
    onSurface = TextPrimary,
    surfaceVariant = StoneSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StoneOutline,
    outlineVariant = StoneOutlineSubtle,
    error = RottenError,
    onError = Color.White,
    errorContainer = RottenContainer,
    onErrorContainer = OnRottenContainer,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = SagePrimaryLight,
    onPrimary = Color(0xFF1B240E),
    primaryContainer = SagePrimaryDark,
    onPrimaryContainer = Color(0xFFE2ECCF),
    secondary = AmberWarning,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = AmberContainer,
    tertiary = Color(0xFF94A3B8),
    onTertiary = Color(0xFF0F172A),
    background = Color(0xFF141312),
    onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF1C1B1A),
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF292524),
    onSurfaceVariant = Color(0xFFA8A29E),
    outline = Color(0xFF44403C),
    outlineVariant = Color(0xFF292524),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
  )

@Composable
fun AkshiTheme(
  darkTheme: Boolean = false, // Default to clean Apple-style light theme
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Alias for template compatibility
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  AkshiTheme(darkTheme = darkTheme, content = content)
}

