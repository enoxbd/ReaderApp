package com.enox.enoxpay.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    background = BgColor,
    surface = CardBg,
    onBackground = TextColor,
    onSurface = TextColor,
    outlineVariant = BorderColor,
    onSurfaceVariant = IconColor
  )

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    primaryContainer = DarkPrimaryContainerBlue,
    onPrimaryContainer = DarkOnPrimaryContainerBlue,
    background = DarkBgColor,
    surface = DarkCardBg,
    onBackground = DarkTextColor,
    onSurface = DarkTextColor,
    outlineVariant = DarkBorderColor,
    onSurfaceVariant = DarkIconColor
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
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
