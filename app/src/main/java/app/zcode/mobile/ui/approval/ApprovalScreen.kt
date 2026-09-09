package app.zcode.mobile.ui.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.RiskLevel
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.Pill
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.theme.Amber
import app.zcode.mobile.ui.theme.Clay
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ApprovalScreen(
    request: ApprovalRequest,
    onBack: () -> Unit,
    onOpenRemote: () -> Unit,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(request.timestamp))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = "需要确认", onBack = onBack)
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(request.title, color = Paper, fontFamily = FontFamily.Serif, fontSize = 26.sp)
            Text(request.description, color = Mute, fontSize = 15.sp, lineHeight = 22.sp)
            Row {
                Pill(
                    text = when (request.riskLevel) {
                        RiskLevel.LOW -> "低风险（启发式）"
                        RiskLevel.MEDIUM -> "中风险（启发式）"
                        RiskLevel.HIGH -> "高风险（启发式）"
                        RiskLevel.UNKNOWN -> "风险未知"
                    },
                    color = when (request.riskLevel) {
                        RiskLevel.LOW -> Sage
                        RiskLevel.MEDIUM -> Amber
                        RiskLevel.HIGH -> Clay
                        RiskLevel.UNKNOWN -> Mute
                    },
                )
            }
            QuietCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("COMMAND", color = Mute, fontSize = 11.sp, letterSpacing = 1.2.sp)
                    Text(request.command.ifBlank { "—" }, color = Paper, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text("来源：${request.source}", color = Mute, fontSize = 12.sp)
                    Text("时间：$time", color = Mute, fontSize = 12.sp)
                }
            }
            if (!request.canActSafely) {
                Text(
                    text = "当前版本需要在 Remote 页面完成确认。允许 / 拒绝不会直接点击网页按钮，以避免误操作。",
                    color = Mute,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                PrimaryButton(text = "打开 ZCode Remote", onClick = onOpenRemote)
                Spacer(Modifier.height(4.dp))
                GhostButton(text = "返回", onClick = onBack)
            } else {
                GhostButton(text = "拒绝", onClick = onDeny)
                PrimaryButton(text = "允许", onClick = onAllow)
                GhostButton(text = "打开 Remote", onClick = onOpenRemote)
            }
        }
    }
}
