package app.zcode.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.model.Device
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.ui.components.Chip
import app.zcode.mobile.ui.components.GroupLabel
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.IconButtonCircle
import app.zcode.mobile.ui.components.ListRow
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.Panel
import app.zcode.mobile.ui.components.RoundAction
import app.zcode.mobile.ui.components.StatusText
import app.zcode.mobile.ui.components.ZGlyph
import app.zcode.mobile.ui.components.ZOutline
import app.zcode.mobile.ui.theme.ZTheme
import app.zcode.mobile.util.RemoteUrl
import java.util.Calendar

@Composable
fun HomeScreen(
    device: Device?,
    connection: ConnectionState,
    tasks: List<Task>,
    onOpenRemote: () -> Unit,
    onSend: (String) -> Unit,
    onTask: (Task) -> Unit,
    onApproval: (Task) -> Unit,
    onVoice: () -> Unit,
    onReconnect: () -> Unit,
    onChangeDevice: () -> Unit,
    onSettings: () -> Unit,
    voiceEnabled: Boolean = true,
    observerSlot: @Composable () -> Unit = {},
) {
    val c = ZTheme.colors
    var draft by remember { mutableStateOf("") }
    val waiting = tasks.filter { it.status == TaskStatus.WAITING_APPROVAL }
    val active = tasks.filter { it.status == TaskStatus.RUNNING || it.status == TaskStatus.QUEUED }
    val recent = tasks.filter { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.FAILED || it.status == TaskStatus.CANCELLED }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        onSend(text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface)
            .imePadding(),
    ) {
        // The shared WebView is hosted here at real screen size but placed just below the
        // viewport: a tiny host gave the page a tiny viewport, which some layouts never recover from.
        val screen = LocalConfiguration.current
        val density = LocalDensity.current
        Box(
            Modifier.layout { measurable, _ ->
                val w = with(density) { screen.screenWidthDp.dp.roundToPx() }
                val h = with(density) { screen.screenHeightDp.dp.roundToPx() }
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(0, 0) { placeable.place(0, h * 2) }
            },
        ) { observerSlot() }

        // Top bar: wordmark left, connection + settings right.
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(start = PageInset, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZGlyph(size = 14.dp)
            Spacer(Modifier.size(8.dp))
            Text("ZCode", color = c.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            StatusText(text = connectionLabel(connection), color = connectionColor(connection, c.success, c.attention, c.danger, c.fgTertiary))
            Spacer(Modifier.size(4.dp))
            IconButtonCircle(Icons.Outlined.Settings, contentDescription = "设置", onClick = onSettings, tint = c.fgSecondary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageInset),
        ) {
            // Hero, as on the desktop home: outlined Z, greeting, composer, suggestion chips.
            Spacer(Modifier.height(if (tasks.isEmpty()) 44.dp else 20.dp))
            if (tasks.isEmpty()) {
                ZOutline(size = 132.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(28.dp))
            }
            Text(
                text = "${greeting()}，有什么想让我帮忙的吗",
                color = c.fg,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Panel(padding = 0.dp) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    textStyle = TextStyle(color = c.fg, fontSize = 15.sp, lineHeight = 22.sp),
                    cursorBrush = SolidColor(c.fg),
                    minLines = 2,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text("向 ZCode 提问，任务会发送到电脑上的 ZCode Desktop", color = c.fgTertiary, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                        inner()
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (voiceEnabled) {
                        IconButtonCircle(Icons.Outlined.Mic, contentDescription = "语音输入", onClick = onVoice, tint = c.fgSecondary, size = 32.dp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(device?.name ?: "未连接", color = c.fgTertiary, fontSize = 12.sp)
                    Spacer(Modifier.size(10.dp))
                    RoundAction(Icons.Outlined.ArrowUpward, contentDescription = "发送", onClick = { send() }, enabled = draft.isNotBlank() && device != null)
                }
            }
            Spacer(Modifier.height(14.dp))
            // One line; scrolls sideways on narrow screens instead of wrapping.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Chip("打开 Remote 页面", onClick = onOpenRemote)
                Chip("重新连接", onClick = onReconnect)
                Chip("更换设备", onClick = onChangeDevice)
            }

            if (tasks.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                TaskGroup(label = "等待确认", tasks = waiting, onClick = onApproval)
                TaskGroup(label = "进行中", tasks = active, onClick = onTask)
                TaskGroup(label = "最近", tasks = recent.take(10), onClick = onTask)
            } else {
                Spacer(Modifier.height(36.dp))
                Text(
                    text = device?.remoteUrl?.let { "已连接 ${RemoteUrl.displayHost(it)} · 打开 Remote 页面后任务会显示在这里" } ?: "尚未连接设备",
                    color = c.fgTertiary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TaskGroup(label: String, tasks: List<Task>, onClick: (Task) -> Unit) {
    if (tasks.isEmpty()) return
    val c = ZTheme.colors
    GroupLabel(label, trailing = tasks.size.toString())
    tasks.forEachIndexed { index, task ->
        if (index > 0) Hairline()
        ListRow(
            title = task.title,
            caption = task.currentStep?.takeIf { it.isNotBlank() },
            meta = statusLabel(task.status),
            metaColor = when (task.status) {
                TaskStatus.WAITING_APPROVAL -> c.attention
                TaskStatus.RUNNING -> c.success
                TaskStatus.FAILED -> c.danger
                else -> c.fgTertiary
            },
            onClick = { onClick(task) },
        )
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..10 -> "上午好呀"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

private fun connectionLabel(state: ConnectionState): String = when (state) {
    ConnectionState.CONNECTED -> "已连接"
    ConnectionState.CONNECTING -> "连接中"
    ConnectionState.SESSION_EXPIRED -> "已过期"
    ConnectionState.ERROR -> "连接异常"
    ConnectionState.DISCONNECTED -> "未连接"
}

private fun connectionColor(state: ConnectionState, ok: Color, warn: Color, bad: Color, idle: Color): Color = when (state) {
    ConnectionState.CONNECTED -> ok
    ConnectionState.CONNECTING -> warn
    ConnectionState.SESSION_EXPIRED, ConnectionState.ERROR -> bad
    ConnectionState.DISCONNECTED -> idle
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.RUNNING -> "运行中"
    TaskStatus.WAITING_APPROVAL -> "需要确认"
    TaskStatus.COMPLETED -> "已完成"
    TaskStatus.FAILED -> "失败"
    TaskStatus.QUEUED -> "排队中"
    TaskStatus.CANCELLED -> "已取消"
    TaskStatus.UNKNOWN -> ""
}
