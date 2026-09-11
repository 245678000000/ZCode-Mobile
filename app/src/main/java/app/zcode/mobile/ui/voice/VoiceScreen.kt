package app.zcode.mobile.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.SecondaryButton
import app.zcode.mobile.ui.theme.ZTheme
import app.zcode.mobile.voice.VoiceRecognizer
import app.zcode.mobile.voice.VoiceState

@Composable
fun VoiceScreen(
    onBack: () -> Unit,
    onSendToZCode: (String) -> Unit,
) {
    val c = ZTheme.colors
    val context = LocalContext.current
    val recognizer = remember { VoiceRecognizer(context) }
    val state by recognizer.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (it) recognizer.start()
    }

    DisposableEffect(Unit) { onDispose { recognizer.stop() } }
    LaunchedEffect(granted) { if (granted && state is VoiceState.Idle) recognizer.start() }

    val listening = state is VoiceState.Listening || state is VoiceState.Partial
    val transcript = when (val s = state) {
        is VoiceState.Partial -> s.text
        is VoiceState.Result -> s.text
        else -> ""
    }

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(title = "语音输入", onBack = {
            recognizer.stop()
            onBack()
        })
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = PageInset, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // The transcript is the content; it sits where a chat message would.
            Text(
                text = transcript.ifBlank {
                    when (state) {
                        is VoiceState.Error -> (state as VoiceState.Error).message
                        VoiceState.Listening -> "正在聆听…"
                        else -> if (granted) "点击麦克风，说出你想让 ZCode 做的事" else "需要麦克风权限"
                    }
                },
                color = when {
                    state is VoiceState.Error -> c.danger
                    transcript.isBlank() || state is VoiceState.Partial -> c.fgTertiary
                    else -> c.fg
                },
                fontSize = if (transcript.isBlank()) 15.sp else 20.sp,
                lineHeight = if (transcript.isBlank()) 22.sp else 30.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            MicButton(listening = listening, onClick = {
                if (!granted) launcher.launch(Manifest.permission.RECORD_AUDIO)
                else if (listening) recognizer.stop()
                else recognizer.start()
            })
            Spacer(Modifier.height(28.dp))
            if (transcript.isNotBlank() && !listening) {
                PrimaryButton(text = "发送到 ZCode", onClick = { onSendToZCode(transcript) })
                Spacer(Modifier.height(10.dp))
                SecondaryButton(text = "复制文字", onClick = { clipboard.setText(AnnotatedString(transcript)) })
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    val c = ZTheme.colors
    val pulse = rememberInfiniteTransition(label = "mic")
    val ring by pulse.animateFloat(1f, 1.5f, infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "ring")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        if (listening) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(ring)
                    .clip(CircleShape)
                    .background(c.fg.copy(alpha = (1.5f - ring).coerceIn(0f, 0.12f))),
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (listening) c.accent else c.surface)
                .border(1.dp, if (listening) c.accent else c.lineStrong, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (listening) Icons.Outlined.Stop else Icons.Outlined.Mic,
                contentDescription = if (listening) "停止" else "开始录音",
                tint = if (listening) c.onAccent else c.fg,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
