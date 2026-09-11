package app.zcode.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object ZTheme {
    val colors: ZColors
        @Composable @ReadOnlyComposable get() = LocalZColors.current
}

@Composable
fun ZCodeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val z = if (darkTheme) DarkZColors else LightZColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = z.accent, onPrimary = z.onAccent,
            background = z.bg, onBackground = z.fg,
            surface = z.surface, onSurface = z.fg,
            surfaceVariant = z.surfaceLow, onSurfaceVariant = z.fgSecondary,
            surfaceContainer = z.surface, surfaceContainerHigh = z.surfaceLow,
            outline = z.line, outlineVariant = z.line, error = z.danger,
        )
    } else {
        lightColorScheme(
            primary = z.accent, onPrimary = z.onAccent,
            background = z.bg, onBackground = z.fg,
            surface = z.surface, onSurface = z.fg,
            surfaceVariant = z.surfaceLow, onSurfaceVariant = z.fgSecondary,
            surfaceContainer = z.surface, surfaceContainerHigh = z.surfaceLow,
            outline = z.line, outlineVariant = z.line, error = z.danger,
        )
    }
    CompositionLocalProvider(LocalZColors provides z) {
        MaterialTheme(colorScheme = scheme, typography = ZCodeTypography, content = content)
    }
}
