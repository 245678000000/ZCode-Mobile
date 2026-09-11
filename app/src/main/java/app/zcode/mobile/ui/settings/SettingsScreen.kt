package app.zcode.mobile.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.BuildConfig
import app.zcode.mobile.data.AppSettings
import app.zcode.mobile.data.SettingsStore
import app.zcode.mobile.model.Device
import app.zcode.mobile.ui.components.GroupLabel
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.ListRow
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.ToggleRow
import app.zcode.mobile.ui.theme.ZTheme
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
    onDeveloper: () -> Unit,
) {
    val c = ZTheme.colors
    val scope = rememberCoroutineScope()
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onDemoNotification()
    }
    val togglePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // Turning a notification toggle on is the natural moment to ask for POST_NOTIFICATIONS.
    val requestIfNeeded: (Boolean) -> Unit = { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= 33) {
            togglePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val developer = BuildConfig.DEBUG || settings.developerMode

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(title = "设置", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageInset),
        ) {
            GroupLabel("设备")
            ListRow(
                title = device?.name ?: "未连接",
                caption = device?.remoteUrl?.let { RemoteUrl.redacted(it) },
            )
            Hairline()
            ListRow(title = "重新连接", chevron = true, onClick = onReconnect)
            Hairline()
            ListRow(title = "清除连接", caption = "删除已保存的 Remote 链接", titleColor = c.danger, onClick = onClearConnection)

            GroupLabel("通知")
            ToggleRow("任务完成", settings.taskNotifications, caption = "任务完成或失败时提醒") {
                requestIfNeeded(it)
                scope.launch { store.setTaskNotifications(it) }
            }
            Hairline()
            ToggleRow("需要确认", settings.approvalNotifications, caption = "Agent 等待你确认时提醒") {
                requestIfNeeded(it)
                scope.launch { store.setApprovalNotifications(it) }
            }
            if (developer) {
                Hairline()
                ListRow(title = "发送演示通知", onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onDemoNotification()
                    }
                })
                Hairline()
                ListRow(title = "打开演示确认请求", onClick = onDemoApproval)
            }

            GroupLabel("输入")
            ToggleRow("语音输入", settings.voiceEnabled, caption = "在首页输入框旁显示麦克风") {
                scope.launch { store.setVoiceEnabled(it) }
            }

            GroupLabel("安全")
            ToggleRow("允许下载文件", settings.allowDownloads, caption = "通过系统下载器保存 Remote 页面里的文件") {
                scope.launch { store.setAllowDownloads(it) }
            }
            Hairline()
            ToggleRow("允许打开外部链接", settings.allowExternalLinks, caption = "非 Remote 域名的链接交给系统浏览器") {
                scope.launch { store.setAllowExternalLinks(it) }
            }
            Hairline()
            ListRow(title = "清除 WebView 数据", caption = "Cookie、缓存与站点数据", titleColor = c.danger, onClick = onClearWebData)

            GroupLabel("更多")
            ListRow(title = "示例 Markdown 预览", chevron = true, onClick = onPreviewSample)
            if (BuildConfig.DEBUG) {
                Hairline()
                ToggleRow("Developer Mode", settings.developerMode) {
                    scope.launch { store.setDeveloperMode(it) }
                }
                Hairline()
                ListRow(title = "Debug Panel", chevron = true, onClick = onDeveloper)
            }

            GroupLabel("关于")
            ListRow(title = "ZCode Mobile", caption = "v${BuildConfig.VERSION_NAME}")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "非官方客户端。ZCode 及相关商标归其所有者所有。",
                color = c.fgTertiary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
