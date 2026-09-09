package app.zcode.mobile.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.QuietCard
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sand
import app.zcode.mobile.voice.VoiceRecognizer
import app.zcode.mobile.voice.VoiceState

@Composable
fun VoiceScreen(
    onBack: () -> Unit,
    onSendToZCode: (String) -> Unit,
) {
    val context = LocalContext.current
    val recognizer = remember { VoiceRecognizer(context) }
    val state by recognizer.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (it) recognizer.start()
    }

    DisposableEffect(Unit) {
        onDispose { recognizer.stop() }
    }

    LaunchedEffect(granted) {
        if (granted && state is VoiceState.Idle) {
            recognizer.start()
        }
    }

    val transcript = when (val s = state) {
        is VoiceState.Partial -> s.text
        is VoiceState.Result -> s.text
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = "语音任务", onBack = {
            recognizer.stop()
            onBack()
        })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (state) {
                    VoiceState.Listening, is VoiceState.Partial -> "正在聆听……"
                    is VoiceState.Result -> "识别完成"
                    is VoiceState.Error -> sMessage(state)
                    else -> "点击麦克风开始"
                },
                color = Paper,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "说完以后可以复制，或发送到 ZCode。",
                color = Mute,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Sand)
                    .clickable {
                        if (!granted) {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        } else if (state is VoiceState.Listening || state is VoiceState.Partial) {
                            recognizer.stop()
                        } else {
                            recognizer.start()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state is VoiceState.Listening || state is VoiceState.Partial) {
                        Icons.Outlined.Stop
                    } else {
                        Icons.Outlined.Mic
                    },
                    contentDescription = "麦克风",
                    tint = Ink,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            if (transcript.isNotBlank() || state is VoiceState.Error) {
                QuietCard {
                    Text(
                        text = transcript.ifBlank { sMessage(state) },
                        color = Paper,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (transcript.isNotBlank()) {
                PrimaryButton(text = "发送到 ZCode", onClick = { onSendToZCode(transcript) })
                Spacer(Modifier.height(10.dp))
                GhostButton(
                    text = "复制",
                    onClick = { clipboard.setText(AnnotatedString(transcript)) },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

private fun sMessage(state: VoiceState): String = when (state) {
    is VoiceState.Error -> state.message
    else -> ""
}
