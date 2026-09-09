package app.zcode.mobile.ui.connect

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.IconTile
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.SectionLabel
import app.zcode.mobile.ui.components.Wordmark
import app.zcode.mobile.ui.theme.Clay
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.InkOverlay
import app.zcode.mobile.ui.theme.Line
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sand
import app.zcode.mobile.util.RemoteUrl

@Composable
fun ConnectScreen(
    initialUrl: String? = null,
    onScan: () -> Unit,
    onConnect: (String) -> Boolean,
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(initialUrl.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPaste by remember { mutableStateOf(!initialUrl.isNullOrBlank()) }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            url = initialUrl
            showPaste = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Wordmark()
        Spacer(Modifier.height(28.dp))
        Text(
            text = "连接 ZCode",
            color = Paper,
            fontFamily = FontFamily.Serif,
            fontSize = 30.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "手机只负责发起任务和查看结果。代码、终端、Git 和 Agent 仍在电脑上的 ZCode Desktop 执行。",
            color = Mute,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(28.dp))
        SectionLabel("连接方式")
        IconTile(
            title = "扫描二维码",
            caption = "扫描电脑端 ZCode Remote Control 二维码",
            onClick = onScan,
        )
        Spacer(Modifier.height(12.dp))
        IconTile(
            title = "粘贴连接地址",
            caption = "从剪贴板或手动输入 Remote URL",
            onClick = { showPaste = true },
        )
        if (showPaste) {
            Spacer(Modifier.height(20.dp))
            SectionLabel("ZCode Remote URL")
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://…/remote/…", color = Mute) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sand,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Paper,
                    unfocusedTextColor = Paper,
                    cursorColor = Sand,
                    focusedContainerColor = InkOverlay,
                    unfocusedContainerColor = InkOverlay,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val pasted = clip.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    if (pasted.isNotBlank()) {
                        url = pasted.trim()
                        error = null
                    }
                }) {
                    Text("Paste", color = Sand)
                }
            }
            if (error != null) {
                Text(text = error!!, color = Clay, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                text = "连接",
                onClick = {
                    val parsed = RemoteUrl.parse(url)
                    if (parsed == null) {
                        error = "请输入以 http:// 或 https:// 开头的有效地址"
                    } else if (!onConnect(parsed.raw)) {
                        error = "无法保存连接"
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            GhostButton(text = "取消", onClick = { showPaste = false })
        }
        Spacer(Modifier.height(36.dp))
        Text(
            text = "请先在电脑端 ZCode 中开启 Remote Control，然后扫描二维码或粘贴连接地址。",
            color = Mute,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "This is an unofficial community client for ZCode. ZCode and related trademarks belong to their respective owners.",
            color = Mute.copy(alpha = 0.7f),
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}
