package app.zcode.mobile.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.model.ZCodeEvent
import app.zcode.mobile.ui.components.CodeBlock
import app.zcode.mobile.ui.components.GroupLabel
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.ListRow
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.StatusText
import app.zcode.mobile.ui.theme.ZTheme
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun TaskDetailScreen(
    task: Task,
    events: List<ZCodeEvent>,
    artifacts: List<Artifact>,
    approval: ApprovalRequest?,
    onBack: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenApproval: () -> Unit,
    onOpenArtifact: (Artifact) -> Unit,
) {
    val c = ZTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(title = "", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageInset),
        ) {
            Text(task.title, color = c.fg, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 30.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusText(
                    text = statusLabel(task.status),
                    color = when (task.status) {
                        TaskStatus.RUNNING -> c.success
                        TaskStatus.WAITING_APPROVAL -> c.attention
                        TaskStatus.FAILED -> c.danger
                        else -> c.fgTertiary
                    },
                )
                task.agentName?.let { Text(it, color = c.fgTertiary, fontSize = 12.sp) }
                Text("更新于 ${fmt.format(Date(task.updatedAt))}", color = c.fgTertiary, fontSize = 12.sp)
            }
            if (task.progress != null) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = c.fg,
                    trackColor = c.surfaceLow,
                )
            }

            if (approval != null) {
                GroupLabel("需要你的确认")
                ListRow(
                    title = approval.title,
                    caption = approval.description.ifBlank { approval.command },
                    meta = "查看",
                    metaColor = c.attention,
                    onClick = onOpenApproval,
                )
                Hairline()
            }

            task.currentStep?.takeIf { it.isNotBlank() }?.let { step ->
                GroupLabel("当前步骤")
                Text(step, color = c.fg, fontSize = 15.sp, lineHeight = 23.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            task.description?.takeIf { it.isNotBlank() && it != task.currentStep }?.let { desc ->
                GroupLabel("摘要")
                CodeBlock(desc, color = c.fgSecondary)
            }

            if (artifacts.isNotEmpty()) {
                GroupLabel("文件", trailing = artifacts.size.toString())
                artifacts.forEachIndexed { index, art ->
                    if (index > 0) Hairline()
                    ListRow(title = art.name, caption = art.kind.name, meta = "打开", onClick = { onOpenArtifact(art) })
                }
            }

            if (events.isNotEmpty()) {
                GroupLabel("事件", trailing = events.size.toString())
                events.take(12).forEach { ev ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(fmt.format(Date(ev.timestamp)), color = c.fgTertiary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(eventLabel(ev.type), color = c.fgSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            PrimaryButton(text = "在 Remote 页面中查看", onClick = onOpenRemote)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun eventLabel(type: String): String = when (type) {
    "TaskCreated" -> "发现任务"
    "TaskUpdated" -> "状态更新"
    "TaskRunning" -> "开始执行"
    "TaskWaiting" -> "等待确认"
    "TaskCompleted" -> "任务完成"
    "TaskFailed" -> "任务失败"
    "ApprovalRequired" -> "请求确认"
    "ApprovalResolved" -> "确认已处理"
    "ArtifactCreated" -> "生成文件"
    "MessageReceived" -> "Agent 回复"
    "ConnectionLost" -> "连接中断"
    "ConnectionRestored" -> "连接恢复"
    "SessionDetected" -> "会话切换"
    else -> type
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.RUNNING -> "运行中"
    TaskStatus.WAITING_APPROVAL -> "需要确认"
    TaskStatus.COMPLETED -> "已完成"
    TaskStatus.FAILED -> "失败"
    TaskStatus.QUEUED -> "排队中"
    TaskStatus.CANCELLED -> "已取消"
    TaskStatus.UNKNOWN -> "状态未知"
}
