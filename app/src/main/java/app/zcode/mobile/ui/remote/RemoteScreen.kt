package app.zcode.mobile.ui.remote

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import app.zcode.mobile.data.AppSettings
import app.zcode.mobile.remote.RemoteErrorKind
import app.zcode.mobile.remote.RemotePageState
import app.zcode.mobile.remote.RemoteWebConfig
import app.zcode.mobile.remote.SessionManager
import app.zcode.mobile.remote.ZCodeWebBridge
import app.zcode.mobile.remote.ZCodeWebView
import app.zcode.mobile.ui.components.ErrorPanel
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.StatusDot
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sage

@Composable
fun RemoteScreen(
    remoteUrl: String,
    settings: AppSettings,
    online: Boolean,
    sessionManager: SessionManager,
    bridge: ZCodeWebBridge,
    pendingInject: String?,
    onConsumeInject: () -> String?,
    onConnected: (Boolean) -> Unit,
    onDownload: (String, String?, String?) -> Unit,
    onWebView: (WebView) -> Unit,
    onBackToHome: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    var pageState by remember { mutableStateOf<RemotePageState>(RemotePageState.Idle) }
    var menu by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(pageState) {
        onConnected(pageState is RemotePageState.Ready)
    }

    LaunchedEffect(pageState, pendingInject, webView) {
        if (pageState is RemotePageState.Ready && pendingInject != null && webView != null) {
            val text = onConsumeInject() ?: return@LaunchedEffect
            bridge.fillComposer(webView!!, text) { }
        }
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onBackToHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        val view = webView
                        if (view != null && view.canGoBack()) view.goBack() else onBackToHome()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = Paper, fontSize = 20.sp)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ZCode", color = Paper, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(connected = pageState is RemotePageState.Ready)
                        Text(
                            text = if (pageState is RemotePageState.Ready) "  Connected" else "  Connecting",
                            color = if (pageState is RemotePageState.Ready) Sage else Mute,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { menu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "菜单", tint = Paper)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Refresh") }, onClick = {
                        menu = false
                        webView?.reload()
                    })
                    DropdownMenuItem(text = { Text("Reconnect") }, onClick = {
                        menu = false
                        reloadToken++
                        onReconnect()
                    })
                    DropdownMenuItem(text = { Text("Open in Browser") }, onClick = {
                        menu = false
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(remoteUrl)))
                    })
                    DropdownMenuItem(text = { Text("Copy Remote URL") }, onClick = {
                        menu = false
                        clipboard.setText(AnnotatedString(remoteUrl))
                    })
                    DropdownMenuItem(text = { Text("Disconnect") }, onClick = {
                        menu = false
                        onDisconnect()
                    })
                }
            }
        }
        Hairline()
        Box(Modifier.weight(1f)) {
            keyReload(reloadToken) {
                ZCodeWebView(
                    config = RemoteWebConfig(
                        remoteUrl = remoteUrl,
                        allowDownloads = settings.allowDownloads,
                        allowExternalLinks = settings.allowExternalLinks,
                        allowFileAccess = false,
                    ),
                    sessionManager = sessionManager,
                    bridge = bridge,
                    modifier = Modifier.fillMaxSize(),
                    onState = { pageState = it },
                    onDownload = onDownload,
                    webViewRef = {
                        webView = it
                        onWebView(it)
                    },
                )
            }
            if (!online) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink),
                ) {
                    ErrorPanel(
                        title = "无法连接 ZCode",
                        reasons = listOf("网络连接异常", "电脑未启动 ZCode", "Remote Control 已关闭"),
                        primary = "重试" to { reloadToken++; webView?.reload() },
                        secondary = "重新连接" to onReconnect,
                    )
                }
            } else if (pageState is RemotePageState.Error) {
                val error = pageState as RemotePageState.Error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink),
                ) {
                    ErrorPanel(
                        title = "无法连接 ZCode",
                        reasons = reasonsFor(error.kind),
                        primary = "重试" to {
                            pageState = RemotePageState.Loading
                            webView?.reload()
                        },
                        secondary = "重新连接" to onReconnect,
                    )
                }
            }
        }
    }
}

@Composable
private fun keyReload(token: Int, content: @Composable () -> Unit) {
    androidx.compose.runtime.key(token) { content() }
}

private fun reasonsFor(kind: RemoteErrorKind): List<String> = when (kind) {
    RemoteErrorKind.Offline -> listOf("网络连接异常", "电脑未启动 ZCode")
    RemoteErrorKind.Dns -> listOf("Remote URL 失效", "DNS 无法解析", "电脑未启动 ZCode")
    RemoteErrorKind.Ssl -> listOf("证书不受信任", "Remote Session 已过期")
    RemoteErrorKind.Http404 -> listOf("Remote Control 已关闭", "Remote Session 已过期", "Remote URL 失效")
    RemoteErrorKind.Http500 -> listOf("电脑端 ZCode 异常", "Remote Control 已关闭")
    RemoteErrorKind.Timeout -> listOf("电脑未启动 ZCode", "网络连接异常")
    RemoteErrorKind.Generic -> listOf(
        "电脑未启动 ZCode",
        "Remote Control 已关闭",
        "网络连接异常",
        "Remote Session 已过期",
    )
}


