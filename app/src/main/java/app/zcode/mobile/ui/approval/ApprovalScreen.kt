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

@Composable
fun ApprovalScreen(
    request: ApprovalRequest,
    onBack: () -> Unit,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = "需要授权", onBack = onBack)
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(request.title, color = Paper, fontFamily = FontFamily.Serif, fontSize = 26.sp)
            Text(request.description, color = Mute, fontSize = 15.sp, lineHeight = 22.sp)
            Row {
                Pill(
                    text = when (request.riskLevel) {
                        RiskLevel.Low -> "低风险"
                        RiskLevel.Medium -> "中风险"
                        RiskLevel.High -> "高风险"
                    },
                    color = when (request.riskLevel) {
                        RiskLevel.Low -> Sage
                        RiskLevel.Medium -> Amber
                        RiskLevel.High -> Clay
                    },
                )
            }
            QuietCard {
                Column {
                    Text("COMMAND", color = Mute, fontSize = 11.sp, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = request.command,
                        color = Paper,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                text = "第一版仅为本地演示。真正的 Agent 审批协议尚未接入。",
                color = Mute,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "拒绝", onClick = onDeny)
            PrimaryButton(text = "允许", onClick = onAllow)
        }
    }
}
