package app.zcode.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Role-based palette matching ZCode desktop ("zai" theme). Light is the default there,
 * so it is the default here; dark mirrors its `.dark` tokens.
 */
@Immutable
data class ZColors(
    val bg: Color, // window ground (desktop sidebar tone)
    val surface: Color, // main content panel
    val surfaceLow: Color, // inset areas: composer footer, code blocks, chips
    val line: Color, // hairlines and borders
    val lineStrong: Color,
    val fg: Color,
    val fgSecondary: Color,
    val fgTertiary: Color,
    val accent: Color, // primary action fill (black on light, white on dark)
    val onAccent: Color,
    val attention: Color, // ZCode's orange for permissions / needs-you
    val success: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightZColors = ZColors(
    bg = Color(0xFFF0F0F0),
    surface = Color(0xFFFFFFFF),
    surfaceLow = Color(0xFFF5F5F5),
    line = Color(0x14000000),
    lineStrong = Color(0x26000000),
    fg = Color(0xFF1A1A1A),
    fgSecondary = Color(0xFF6B6B6B),
    fgTertiary = Color(0xFF9E9E9E),
    accent = Color(0xFF111111),
    onAccent = Color(0xFFFFFFFF),
    attention = Color(0xFFE8590C),
    success = Color(0xFF16A34A),
    danger = Color(0xFFDC2626),
    isDark = false,
)

val DarkZColors = ZColors(
    bg = Color(0xFF0F0F0F),
    surface = Color(0xFF161616),
    surfaceLow = Color(0xFF1F1F1F),
    line = Color(0x1AFFFFFF),
    lineStrong = Color(0x30FFFFFF),
    fg = Color(0xFFF2F2F2),
    fgSecondary = Color(0xFFA0A0A0),
    fgTertiary = Color(0xFF6E6E6E),
    accent = Color(0xFFF5F5F5),
    onAccent = Color(0xFF111111),
    attention = Color(0xFFF97316),
    success = Color(0xFF22C55E),
    danger = Color(0xFFEF4444),
    isDark = true,
)

val LocalZColors = staticCompositionLocalOf { LightZColors }
