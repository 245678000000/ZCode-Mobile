package app.zcode.mobile.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.BuildConfig
import app.zcode.mobile.data.AppSettings
import app.zcode.mobile.data.SettingsStore
import app.zcode.mobile.model.Device
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.SectionLabel
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sand
import app.zcode.mobile.util.RemoteUrl
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    device: Device?,
    settings: AppSettings,
    store: SettingsStore,
    onBack: () -> Unit,
    onReconnect: () -> Unit,
    onClearConnection: () -> Unit,
    onClearWebData: () -> Unit,
    onDemoNotification: () -> Unit,
    onDemoApproval: () -> Unit,
    onPreviewSample: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onDemoNotification()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = "设置", onBack = onBack)
        Hairline()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionLabel("Connected Device")
            QuietCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(device?.name ?: "ZCode Desktop", color = Paper, fontSize = 16.sp)
                    Text(
                        text = device?.remoteUrl?.let { RemoteUrl.redacted(it) } ?: "未连接",
                        color = Mute,
                        fontSize = 13.sp,
                    )
                }
            }
            PrimaryButton(text = "重新连接", onClick = onReconnect)
            GhostButton(text = "清除连接", onClick = onClearConnection)

            SectionLabel("Voice")
            ToggleRow("开启语音输入", settings.voiceEnabled) {
                scope.launch { store.setVoiceEnabled(it) }
            }

            SectionLabel("Notifications")
            ToggleRow("任务完成通知", settings.taskNotifications) {
                scope.launch { store.setTaskNotifications(it) }
            }
            ToggleRow("授权请求通知", settings.approvalNotifications) {
                scope.launch { store.setApprovalNotifications(it) }
            }
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onDemoNotification()
                }
            }) { Text("发送演示任务通知", color = Sand) }
            TextButton(onClick = onDemoApproval) { Text("打开演示授权请求", color = Sand) }

            SectionLabel("Security")
            ToggleRow("允许下载文件", settings.allowDownloads) {
                scope.launch { store.setAllowDownloads(it) }
            }
            ToggleRow("允许打开外部链接", settings.allowExternalLinks) {
                scope.launch { store.setAllowExternalLinks(it) }
            }
            GhostButton(text = "清除 WebView 数据", onClick = onClearWebData)

            SectionLabel("Preview")
            TextButton(onClick = onPreviewSample) { Text("打开示例 Markdown", color = Sand) }

            SectionLabel("About")
            QuietCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ZCode Mobile", color = Paper, fontSize = 16.sp)
                    Text("Version ${BuildConfig.VERSION_NAME}", color = Mute, fontSize = 13.sp)
                    Text("Unofficial Remote Client", color = Mute, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This is an unofficial community client for ZCode. ZCode and related trademarks belong to their respective owners.",
                        color = Mute,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Paper, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Sand, checkedThumbColor = Ink),
        )
    }
}
