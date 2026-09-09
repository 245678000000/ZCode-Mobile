package app.zcode.mobile.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.model.ZCodeEvent
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.SectionLabel
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = task.title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(task.title, color = Paper, fontFamily = FontFamily.Serif, fontSize = 26.sp)
            Text("状态：${statusLabel(task.status)}", color = Mute, fontSize = 14.sp)
            if (task.progress != null) {
                Text("进度：${(task.progress * 100).toInt()}%", color = Mute, fontSize = 13.sp)
            }
            task.agentName?.let { Text("Agent：$it", color = Mute, fontSize = 13.sp) }
            task.currentStep?.let {
                SectionLabel("当前步骤")
                QuietCard { Text(it, color = Paper, fontSize = 15.sp, lineHeight = 22.sp) }
            }
            if (events.isNotEmpty()) {
                SectionLabel("最近事件")
                QuietCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        events.take(12).forEach { ev ->
                            Text(
                                text = "${fmt.format(Date(ev.timestamp))}  ${ev.type}",
                                color = Paper,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
            if (artifacts.isNotEmpty()) {
                SectionLabel("Artifacts")
                artifacts.forEach { art ->
                    QuietCard(onClick = { onOpenArtifact(art) }) {
                        Text(art.name, color = Paper, fontSize = 14.sp)
                        Text(art.kind.name, color = Mute, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (approval != null) {
                SectionLabel("Approval")
                QuietCard(onClick = onOpenApproval) {
                    Text(approval.title, color = Paper, fontSize = 15.sp)
                    Text(approval.description, color = Mute, fontSize = 13.sp)
                }
            }
            PrimaryButton(text = "打开 Remote", onClick = onOpenRemote)
            GhostButton(text = "返回", onClick = onBack)
        }
    }
}

private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.RUNNING -> "正在执行"
    TaskStatus.WAITING_APPROVAL -> "等待确认"
    TaskStatus.COMPLETED -> "已完成"
    TaskStatus.FAILED -> "失败"
    TaskStatus.QUEUED -> "排队中"
    TaskStatus.CANCELLED -> "已取消"
    TaskStatus.UNKNOWN -> "未知"
}
