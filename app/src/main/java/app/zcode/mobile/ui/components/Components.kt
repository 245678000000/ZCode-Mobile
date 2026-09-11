package app.zcode.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.ui.theme.ZTheme

val PageInset = 20.dp
val ButtonShape = RoundedCornerShape(10.dp)
val PanelShape = RoundedCornerShape(14.dp)

// ---------------------------------------------------------------------------
// Brand
// ---------------------------------------------------------------------------

private fun zPath(w: Float): Path {
    // Same geometry as the launcher icon, normalised to a 44.2 x 37.6 box.
    val s = w / 44.2f
    fun p(x: Float, y: Float) = Offset((x - 31.9f) * s, (y - 35.2f) * s)
    return Path().apply {
        moveTo(p(33f, 35.2f).x, p(33f, 35.2f).y); lineTo(p(55f, 35.2f).x, p(55f, 35.2f).y)
        lineTo(p(51.2f, 40.6f).x, p(51.2f, 40.6f).y); lineTo(p(33f, 40.6f).x, p(33f, 40.6f).y); close()
        moveTo(p(58.5f, 35.2f).x, p(58.5f, 35.2f).y); lineTo(p(76.1f, 35.2f).x, p(76.1f, 35.2f).y)
        lineTo(p(49.5f, 72.8f).x, p(49.5f, 72.8f).y); lineTo(p(31.9f, 72.8f).x, p(31.9f, 72.8f).y); close()
        moveTo(p(75f, 67.4f).x, p(75f, 67.4f).y); lineTo(p(75f, 72.8f).x, p(75f, 72.8f).y)
        lineTo(p(53f, 72.8f).x, p(53f, 72.8f).y); lineTo(p(56.8f, 67.4f).x, p(56.8f, 67.4f).y); close()
    }
}

/** Filled Z glyph. [size] is the width. */
@Composable
fun ZGlyph(size: Dp, color: Color = ZTheme.colors.fg, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = size, height = size * (37.6f / 44.2f))) {
        drawPath(zPath(this.size.width), color)
    }
}

/** Thin outlined Z — the empty-state mark ZCode desktop shows on its home. */
@Composable
fun ZOutline(size: Dp, modifier: Modifier = Modifier, color: Color = ZTheme.colors.lineStrong) {
    Canvas(modifier = modifier.size(width = size, height = size * (37.6f / 44.2f))) {
        drawPath(zPath(this.size.width), color, style = Stroke(width = 1.dp.toPx()))
    }
}

/** App-icon style tile. */
@Composable
fun ZMark(size: Dp = 36.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.24f)).background(Color(0xFF111111)),
        contentAlignment = Alignment.Center,
    ) {
        ZGlyph(size = size * 0.5f, color = Color.White)
    }
}

// ---------------------------------------------------------------------------
// Structure
// ---------------------------------------------------------------------------

@Composable
fun Hairline(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(modifier = modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(ZTheme.colors.line))
}

/** Small grey group label, like the sidebar's "项目". */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = ZTheme.colors.fgTertiary, fontSize = 12.sp)
        if (trailing != null) Text(trailing, color = ZTheme.colors.fgTertiary, fontSize = 12.sp)
    }
}

/**
 * Plain list row: title, optional caption, optional right-aligned meta (an age, a status).
 * No card, no icon by default — rows separate with [Hairline]s like the desktop sidebar.
 */
@Composable
fun ListRow(
    title: String,
    caption: String? = null,
    meta: String? = null,
    metaColor: Color = ZTheme.colors.fgTertiary,
    leading: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    titleColor: Color = ZTheme.colors.fg,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) leading()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = titleColor, fontSize = 15.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) Text(caption, color = ZTheme.colors.fgSecondary, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (meta != null) Text(meta, color = metaColor, fontSize = 12.sp)
        if (trailing != null) trailing()
        if (chevron) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = ZTheme.colors.fgTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, caption: String? = null, onChange: (Boolean) -> Unit) {
    val c = ZTheme.colors
    ListRow(
        title = label,
        caption = caption,
        onClick = { onChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = c.accent,
                    checkedThumbColor = c.onAccent,
                    checkedBorderColor = Color.Transparent,
                    uncheckedTrackColor = c.surfaceLow,
                    uncheckedThumbColor = c.fgTertiary,
                    uncheckedBorderColor = c.lineStrong,
                ),
            )
        },
    )
}

/** White panel with a soft edge — the composer / dialog surface on desktop. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = ZTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (c.isDark) 0.dp else 8.dp, PanelShape, spotColor = Color(0x14000000), ambientColor = Color(0x0A000000))
            .clip(PanelShape)
            .background(c.surface)
            .border(1.dp, c.line, PanelShape)
            .padding(padding),
        content = content,
    )
}

/** Inset grey block: code, quoted text. */
@Composable
fun CodeBlock(text: String, modifier: Modifier = Modifier, color: Color = ZTheme.colors.fg) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ZTheme.colors.surfaceLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = text, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = ZTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(ButtonShape)
            .background(if (enabled) c.accent else c.accent.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = ZTheme.colors.fg) {
    val c = ZTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(ButtonShape)
            .background(c.surface)
            .border(1.dp, c.lineStrong, ButtonShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Small grey chip, like the desktop home's suggestion chips (周报总结 / 报错修复 …). */
@Composable
fun Chip(text: String, onClick: () -> Unit, icon: ImageVector? = null) {
    val c = ZTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.fgSecondary, modifier = Modifier.size(14.dp))
        Text(text, color = c.fg, fontSize = 12.sp, maxLines = 1, softWrap = false)
    }
}

@Composable
fun IconButtonCircle(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, tint: Color = ZTheme.colors.fg, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Solid round action button — the desktop composer's send button. */
@Composable
fun RoundAction(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, enabled: Boolean = true, size: Dp = 32.dp) {
    val c = ZTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) c.accent else c.surfaceLow)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (enabled) c.onAccent else c.fgTertiary, modifier = Modifier.size(18.dp))
    }
}

/** Compact segmented control, like the desktop's 分组 / 项目 tabs. */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = ZTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surfaceLow)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) c.surface else Color.Transparent)
                    .border(1.dp, if (active) c.line else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(label, color = if (active) c.fg else c.fgSecondary, fontSize = 12.sp, fontWeight = if (active) FontWeight.Medium else FontWeight.Normal)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Status
// ---------------------------------------------------------------------------

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(7.dp).clip(CircleShape).background(color))
}

/** "● 已就绪" style inline status, as on the desktop remote-control dialog. */
@Composable
fun StatusText(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusDot(color)
        Text(text, color = ZTheme.colors.fgSecondary, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// Screen chrome
// ---------------------------------------------------------------------------

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = ZTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButtonCircle(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", onClick = onBack, tint = c.fgSecondary)
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, color = c.fg, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = c.fgTertiary, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1)
        }
        trailing()
    }
}

@Composable
fun ErrorPanel(
    title: String,
    reasons: List<String>,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
) {
    val c = ZTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(PageInset),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, color = c.fg, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("可能的原因", color = c.fgTertiary, fontSize = 12.sp)
            reasons.forEach { Text("·  $it", color = c.fgSecondary, fontSize = 14.sp, lineHeight = 21.sp) }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = primary.first, onClick = primary.second)
        if (secondary != null) SecondaryButton(text = secondary.first, onClick = secondary.second)
    }
}
