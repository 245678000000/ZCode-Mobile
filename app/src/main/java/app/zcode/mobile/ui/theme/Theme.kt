package app.zcode.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Sand,
    onPrimary = Ink,
    primaryContainer = InkOverlay,
    onPrimaryContainer = Paper,
    secondary = Sage,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = InkRaised,
    onSurfaceVariant = Mute,
    outline = Line,
    outlineVariant = Line,
    error = Clay,
    onError = Paper,
    tertiary = Amber,
    scrim = Color(0xCC0F1114),
)

@Composable
fun ZCodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = ZCodeTypography,
        content = content,
    )
}
