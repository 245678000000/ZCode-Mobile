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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.IconButtonCircle
import app.zcode.mobile.ui.components.ListRow
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.Panel
import app.zcode.mobile.ui.components.RoundAction
import app.zcode.mobile.ui.components.ZOutline
import app.zcode.mobile.ui.theme.ZTheme
import app.zcode.mobile.util.RemoteUrl

@Composable
fun ConnectScreen(
    initialUrl: String? = null,
    onScan: () -> Unit,
    onConnect: (String) -> Boolean,
) {
    val c = ZTheme.colors
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

    fun submit() {
        val parsed = RemoteUrl.parse(url)
        if (parsed == null) {
            error = if (RemoteUrl.isPublicHttp(url)) {
                "公网地址必须使用 https://，http:// 仅限局域网地址"
            } else {
                "请输入以 http:// 或 https:// 开头的有效地址"
            }
        } else if (!onConnect(parsed.raw)) {
            error = "无法保存连接"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PageInset),
    ) {
        Spacer(Modifier.height(72.dp))
        ZOutline(size = 120.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(32.dp))
        Text("连接你的 ZCode Desktop", color = c.fg, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "在电脑上打开 ZCode，点左下角的「移动端远程控制」，用手机扫码，或复制链接粘贴到下面。代码、终端和 Agent 仍然在电脑上运行。",
            color = c.fgSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(28.dp))

        ListRow(title = "扫描二维码", caption = "用相机对准电脑屏幕上的二维码", chevron = true, onClick = onScan)
        Hairline()
        ListRow(title = "粘贴连接地址", caption = "电脑端点「复制链接」后粘贴到这里", chevron = true, onClick = { showPaste = true })
        Hairline()

        if (showPaste) {
            Spacer(Modifier.height(24.dp))
            Panel(padding = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            error = null
                        },
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = c.fg, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        cursorBrush = SolidColor(c.fg),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                        decorationBox = { inner ->
                            if (url.isEmpty()) Text("https://…", color = c.fgTertiary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            inner()
                        },
                    )
                    IconButtonCircle(Icons.Outlined.ContentPaste, contentDescription = "粘贴", tint = c.fgSecondary, size = 32.dp, onClick = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val pasted = clip.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        if (pasted.isNotBlank()) {
                            url = pasted.trim()
                            error = null
                        }
                    })
                    Spacer(Modifier.size(4.dp))
                    RoundAction(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "连接", onClick = { submit() }, enabled = url.isNotBlank())
                }
            }
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = c.danger, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(48.dp))
        Text(
            text = "ZCode Mobile 是非官方客户端。ZCode 及相关商标归其所有者所有。",
            color = c.fgTertiary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(24.dp))
    }
}
