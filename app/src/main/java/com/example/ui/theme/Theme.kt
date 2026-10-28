package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gp180ColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = CyberBlack,
  primaryContainer = SurfaceCardActive,
  onPrimaryContainer = NeonCyan,
  secondary = NeonBlue,
  onSecondary = Color.White,
  secondaryContainer = NeonBlueDim,
  onSecondaryContainer = NeonBlue,
  tertiary = NeonEmerald,
  onTertiary = CyberBlack,
  tertiaryContainer = NeonEmeraldDim,
  onTertiaryContainer = NeonEmerald,
  background = CyberBlack,
  onBackground = TextPrimary,
  surface = DarkGunmetal,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceCard,
  onSurfaceVariant = TextSecondary,
  outline = SurfaceCardBorder,
  outlineVariant = SliderTrack,
  error = NeonCrimson,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = Gp180ColorScheme,
    typography = Typography,
    content = content
  )
}
