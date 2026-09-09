package app.zcode.mobile.ui.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.model.Device
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.IconTile
import app.zcode.mobile.ui.components.Pill
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.Wordmark
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sage
import app.zcode.mobile.util.RemoteUrl

@Composable
fun HomeScreen(
    device: Device?,
    connected: Boolean,
    onOpenRemote: () -> Unit,
    onVoice: () -> Unit,
    onReconnect: () -> Unit,
    onChangeDevice: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Wordmark()
            Pill(text = if (connected) "已连接" else "已保存", color = if (connected) Sage else Mute)
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = device?.name ?: "ZCode Desktop",
            color = Paper,
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = device?.remoteUrl?.let { RemoteUrl.redacted(it) } ?: "未连接",
            color = Mute,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(24.dp))
        QuietCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("继续任务", color = Mute, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(
                    text = "打开电脑上正在运行的 ZCode Agent 会话。",
                    color = Paper,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton(text = "打开 ZCode", onClick = onOpenRemote)
            }
        }
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
