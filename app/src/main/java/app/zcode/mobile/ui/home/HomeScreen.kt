package app.zcode.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.model.Device
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.IconTile
import app.zcode.mobile.ui.components.Pill
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.SectionLabel
import app.zcode.mobile.ui.components.Wordmark
import app.zcode.mobile.ui.theme.Amber
import app.zcode.mobile.ui.theme.Clay
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sage
import app.zcode.mobile.util.RemoteUrl
import androidx.compose.ui.graphics.Color

@Composable
fun HomeScreen(
    device: Device?,
    connection: ConnectionState,
    tasks: List<Task>,
    onOpenRemote: () -> Unit,
    onTask: (Task) -> Unit,
    onApproval: (Task) -> Unit,
    onVoice: () -> Unit,
    onReconnect: () -> Unit,
    onChangeDevice: () -> Unit,
    onSettings: () -> Unit,
    observerSlot: @Composable () -> Unit = {},
) {
    val running = tasks.filter { it.status == TaskStatus.RUNNING }
    val waiting = tasks.filter { it.status == TaskStatus.WAITING_APPROVAL }
    val done = tasks.filter { it.status == TaskStatus.COMPLETED || it.status == TaskStatus.FAILED }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Box(Modifier.size(1.dp)) { observerSlot() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Wordmark()
            Pill(
                text = connectionLabel(connection),
                color = connectionColor(connection),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = device?.name ?: "ZCode Desktop",
            color = Paper,
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
        )
        Text(
            text = device?.remoteUrl?.let { RemoteUrl.redacted(it) } ?: "未连接",
            color = Mute,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(24.dp))
        if (tasks.isEmpty()) {
            QuietCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("暂无可识别任务", color = Paper, fontSize = 16.sp)
                    Text(
                        text = "打开 Remote 后，App 会从页面可见状态里识别任务。识别失败不影响继续使用 WebView。",
                        color = Mute,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    )
                    PrimaryButton(text = "打开 ZCode Remote", onClick = onOpenRemote)
                }
            }
        } else {
            if (running.isNotEmpty()) {
                SectionLabel("正在进行")
                running.forEach { TaskRow(it, onTask) }
                Spacer(Modifier.height(16.dp))
            }
            if (waiting.isNotEmpty()) {
                SectionLabel("等待处理")
                waiting.forEach { task ->
                    TaskRow(task, onTask, action = "查看" to { onApproval(task) })
                }
                Spacer(Modifier.height(16.dp))
            }
            if (done.isNotEmpty()) {
                SectionLabel("已完成")
                done.forEach { TaskRow(it, onTask) }
                Spacer(Modifier.height(16.dp))
            }
            PrimaryButton(text = "打开 ZCode", onClick = onOpenRemote)
        }
        Spacer(Modifier.height(16.dp))
        Hairline()
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(
                title = "语音任务",
                caption = "说话，转成文字后发给 Agent",
                onClick = onVoice,
                modifier = Modifier.weight(1f),
            )
            IconTile(
                title = "设置",
                caption = "通知、安全与连接",
                onClick = onSettings,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        GhostButton(text = "重新连接", onClick = onReconnect)
        Spacer(Modifier.height(10.dp))
        GhostButton(text = "更换设备", onClick = onChangeDevice)
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onClick: (Task) -> Unit,
    action: Pair<String, () -> Unit>? = null,
) {
    QuietCard(onClick = { onClick(task) }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.title, color = Paper, fontSize = 16.sp)
            Text(statusLabel(task.status), color = statusColor(task.status), fontSize = 12.sp)
            task.currentStep?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Mute, fontSize = 13.sp, lineHeight = 18.sp)
            }
            if (action != null) {
                Text(
                    text = action.first,
                    color = Paper,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = action.second),
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

private fun connectionLabel(state: ConnectionState): String = when (state) {
    ConnectionState.CONNECTED -> "Connected"
    ConnectionState.CONNECTING -> "Connecting"
    ConnectionState.SESSION_EXPIRED -> "Expired"
    ConnectionState.ERROR -> "Error"
    ConnectionState.DISCONNECTED -> "Saved"
}

private fun connectionColor(state: ConnectionState): Color = when (state) {
    ConnectionState.CONNECTED -> Sage
    ConnectionState.CONNECTING -> Amber
    ConnectionState.SESSION_EXPIRED, ConnectionState.ERROR -> Clay
    ConnectionState.DISCONNECTED -> Mute
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.RUNNING -> "正在执行"
    TaskStatus.WAITING_APPROVAL -> "需要你的确认"
    TaskStatus.COMPLETED -> "已完成"
    TaskStatus.FAILED -> "失败"
    TaskStatus.QUEUED -> "排队中"
    TaskStatus.CANCELLED -> "已取消"
    TaskStatus.UNKNOWN -> "未知"
}

private fun statusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.RUNNING -> Sage
    TaskStatus.WAITING_APPROVAL -> Amber
    TaskStatus.FAILED -> Clay
    else -> Mute
}
