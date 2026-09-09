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
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.SectionLabel
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper

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
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = "Developer", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            QuietCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("URL  $url", color = Paper, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text("Connection  $connection", color = Mute, fontSize = 13.sp)
                    Text("Observer  ${if (observerActive) "active" else "idle"}", color = Mute, fontSize = 13.sp)
                    Text("Bridge  ZCodeAndroidBridge", color = Mute, fontSize = 13.sp)
                }
            }
            SectionLabel("Tasks ${tasks.size}")
            tasks.take(12).forEach {
                Text("${it.status}  ${it.title}", color = Paper, fontSize = 13.sp)
            }
            SectionLabel("Approvals ${approvals.size}")
            approvals.take(8).forEach {
                Text("${it.riskLevel}  ${it.title}", color = Paper, fontSize = 13.sp)
            }
            SectionLabel("Artifacts ${artifacts.size}")
            artifacts.take(8).forEach {
                Text(it.name, color = Paper, fontSize = 13.sp)
            }
            SectionLabel("Events ${events.size}")
            events.take(50).forEach {
                Text("${it.type}  ${it.timestamp}", color = Mute, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            PrimaryButton(text = "Copy sanitized debug info", onClick = {
                clipboard.setText(AnnotatedString(dump))
            })
            GhostButton(text = "Inject developer fixture", onClick = onInjectFixture)
            Text("Fixture 只在 Developer Mode 下可用，不会进入默认生产 UI。", color = Mute, fontSize = 12.sp)
        }
    }
}
