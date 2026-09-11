package app.zcode.mobile.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.ZCodeEvent
import app.zcode.mobile.ui.components.CodeBlock
import app.zcode.mobile.ui.components.SecondaryButton
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.GroupLabel
import app.zcode.mobile.ui.theme.ZTheme

@Composable
fun DeveloperScreen(
    url: String,
    connection: ConnectionState,
    observerActive: Boolean,
    events: List<ZCodeEvent>,
    tasks: List<Task>,
    approvals: List<ApprovalRequest>,
    artifacts: List<Artifact>,
    dump: String,
    onBack: () -> Unit,
    onInjectFixture: () -> Unit,
) {
    val c = ZTheme.colors
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        ScreenHeader(title = "Developer", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageInset, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CodeBlock(
                text = buildString {
                    appendLine("url        $url")
                    appendLine("connection $connection")
                    appendLine("observer   ${if (observerActive) "active" else "idle"}")
                    append("bridge     ZCodeAndroidBridge")
                },
            )
            GroupLabel("Tasks ${tasks.size}")
            tasks.take(12).forEach {
                Text("${it.status}  ${it.title}", color = c.fg, fontSize = 13.sp)
            }
            GroupLabel("Approvals ${approvals.size}")
            approvals.take(8).forEach {
                Text("${it.riskLevel}  ${it.title}", color = c.fg, fontSize = 13.sp)
            }
            GroupLabel("Artifacts ${artifacts.size}")
            artifacts.take(8).forEach {
                Text(it.name, color = c.fg, fontSize = 13.sp)
            }
            GroupLabel("Events ${events.size}")
            events.take(50).forEach {
                Text("${it.type}  ${it.timestamp}", color = c.fgSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            PrimaryButton(text = "Copy sanitized debug info", onClick = {
                clipboard.setText(AnnotatedString(dump))
            })
            SecondaryButton(text = "Inject developer fixture", onClick = onInjectFixture)
            Text("Fixture 只在 Developer Mode 下可用，不会进入默认生产 UI。", color = c.fgSecondary, fontSize = 12.sp)
        }
    }
}
