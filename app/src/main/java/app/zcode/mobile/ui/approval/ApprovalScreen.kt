package app.zcode.mobile.ui.approval

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.RiskLevel
import app.zcode.mobile.ui.components.CodeBlock
import app.zcode.mobile.ui.components.GroupLabel
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.SecondaryButton
import app.zcode.mobile.ui.components.StatusText
import app.zcode.mobile.ui.theme.ZTheme
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ApprovalScreen(
    request: ApprovalRequest,
    onBack: () -> Unit,
    onOpenRemote: () -> Unit,
) {
    val c = ZTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val time = remember(request.timestamp, locale) {
        SimpleDateFormat("HH:mm", locale).format(Date(request.timestamp))
    }
    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(title = "", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageInset),
        ) {
            // ZCode marks permission prompts in orange ("完全访问"); reuse that here.
            Text("需要你的确认", color = c.attention, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(request.title, color = c.fg, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 30.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusText(
                    text = riskLabel(request.riskLevel),
                    color = when (request.riskLevel) {
                        RiskLevel.HIGH -> c.danger
                        RiskLevel.MEDIUM -> c.attention
                        RiskLevel.LOW -> c.success
                        RiskLevel.UNKNOWN -> c.fgTertiary
                    },
                )
                Text("$time · ${sourceLabel(request.source)}", color = c.fgTertiary, fontSize = 12.sp)
            }
            if (request.description.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(request.description, color = c.fgSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            }

            GroupLabel("Agent 想要执行")
            CodeBlock(
                text = request.command.ifBlank { "页面上没有识别到具体命令" },
                color = if (request.command.isBlank()) c.fgTertiary else c.fg,
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = "为避免误操作，App 不会替你点击网页上的允许或拒绝。请到 Remote 页面里完成确认。",
                color = c.fgTertiary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "去 Remote 页面确认", onClick = onOpenRemote)
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = "稍后处理", onClick = onBack)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun riskLabel(level: RiskLevel): String = when (level) {
    RiskLevel.LOW -> "低风险"
    RiskLevel.MEDIUM -> "中风险"
    RiskLevel.HIGH -> "高风险"
    RiskLevel.UNKNOWN -> "风险未知"
}

private fun sourceLabel(source: String): String = when (source) {
    "dom" -> "来自 Remote 页面"
    "developer-demo" -> "演示数据"
    else -> source
}
