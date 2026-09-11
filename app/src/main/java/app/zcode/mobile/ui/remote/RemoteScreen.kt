package app.zcode.mobile.ui.remote

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.zcode.mobile.remote.WebLogSink
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.remote.RemoteWebConfig
import app.zcode.mobile.remote.SessionManager
import app.zcode.mobile.remote.ZCodeDomObserver
import app.zcode.mobile.remote.ZCodeWebBridge
import app.zcode.mobile.remote.ZCodeWebView
import app.zcode.mobile.ui.components.ErrorPanel
import app.zcode.mobile.ui.components.IconButtonCircle
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.StatusText
import app.zcode.mobile.ui.theme.ZTheme
import androidx.compose.ui.text.font.FontWeight

@Composable
fun RemoteScreen(
    remoteUrl: String,
    settings: AppSettings,
    online: Boolean,
    sessionManager: SessionManager,
    bridge: ZCodeWebBridge,
    observer: ZCodeDomObserver,
    retainedWebView: () -> WebView?,
    pageState: RemotePageState,
    pageProgress: Int,
    onPageState: (RemotePageState) -> Unit,
    onProgress: (Int) -> Unit,
    onRendererGone: () -> Unit,
    log: WebLogSink,
    pendingInject: String?,
    onConsumeInject: () -> String?,
    onConnected: (Boolean) -> Unit,
    onConnection: (ConnectionState) -> Unit,
    onDownload: (String, String?, String?) -> Unit,
    onWebView: (WebView) -> Unit,
    onBackToHome: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    val c = ZTheme.colors
    var menu by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(pageState) {
        onConnected(pageState is RemotePageState.Ready)
    }

    // The Remote page is a SPA: the composer only exists inside a session, and it renders a
    // moment after navigation. The fill script opens the current session when needed
    // ("navigated"), so keep retrying; the draft is only consumed once it was delivered.
    LaunchedEffect(pageState, pendingInject, webView) {
        val view = webView ?: return@LaunchedEffect
        if (pageState !is RemotePageState.Ready) return@LaunchedEffect
        val text = pendingInject ?: return@LaunchedEffect
        repeat(20) { attempt ->
            val result = suspendCancellableCoroutine<ZCodeWebBridge.BridgeResult> { cont ->
                bridge.fillComposer(view, text, send = true) { if (cont.isActive) cont.resume(it) }
            }
            if (result == ZCodeWebBridge.BridgeResult.Filled) {
                onConsumeInject()
                return@LaunchedEffect
            }
            delay(if (result == ZCodeWebBridge.BridgeResult.Navigated || attempt < 4) 600L else 1000L)
        }
        onConsumeInject()
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
            .background(c.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButtonCircle(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = c.fgSecondary,
                onClick = {
                    val view = webView
                    if (view != null && view.canGoBack()) view.goBack() else onBackToHome()
                },
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("ZCode", color = c.fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                StatusText(
                    text = when (pageState) {
                        is RemotePageState.Ready -> "已连接"
                        is RemotePageState.Error -> "连接失败"
                        else -> if (pageProgress in 1..99) "加载中 $pageProgress%" else "连接中…"
                    },
                    color = when (pageState) {
                        is RemotePageState.Ready -> c.success
                        is RemotePageState.Error -> c.danger
                        else -> c.fgTertiary
                    },
                )
            }
            Box {
                IconButtonCircle(icon = Icons.Outlined.MoreVert, contentDescription = "菜单", onClick = { menu = true }, tint = c.fgSecondary)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface) {
                    DropdownMenuItem(text = { Text("刷新", color = c.fg) }, onClick = {
                        menu = false
                        webView?.reload()
                    })
                    DropdownMenuItem(text = { Text("重新连接", color = c.fg) }, onClick = {
                        menu = false
                        reloadToken++
                        onReconnect()
                    })
                    DropdownMenuItem(text = { Text("在浏览器中打开", color = c.fg) }, onClick = {
                        menu = false
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(remoteUrl)))
                    })
                    DropdownMenuItem(text = { Text("复制 Remote 链接", color = c.fg) }, onClick = {
                        menu = false
                        clipboard.setText(AnnotatedString(remoteUrl))
                    })
                    DropdownMenuItem(text = { Text("断开连接", color = c.danger) }, onClick = {
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
                    observer = observer,
                    retainedWebView = retainedWebView,
                    onState = onPageState,
                    onDownload = onDownload,
                    onConnection = onConnection,
                    onProgress = onProgress,
                    onRendererGone = onRendererGone,
                    log = log,
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
                        .background(c.surface),
                ) {
                    ErrorPanel(
                        title = "无法连接 ZCode",
                        reasons = listOf("网络连接异常", "电脑未启动 ZCode", "Remote Control 已关闭"),
                        primary = "重试" to { reloadToken++; webView?.reload() },
                        secondary = "重新连接" to onReconnect,
                    )
                }
            } else if (pageState is RemotePageState.Error) {
                val error = pageState
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(c.surface),
                ) {
                    ErrorPanel(
                        title = "无法连接 ZCode",
                        reasons = reasonsFor(error.kind) + listOfNotNull(error.detail?.let { "详情：$it" }),
                        primary = "重试" to {
                            onPageState(RemotePageState.Loading)
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
    RemoteErrorKind.SessionExpired -> listOf("Remote Session 已过期", "请在电脑上重新生成 Remote 二维码")
    RemoteErrorKind.RendererGone -> listOf("网页进程被系统回收或崩溃", "点重试会重新创建页面")
    RemoteErrorKind.Timeout -> listOf("电脑未启动 ZCode", "网络连接异常")
    RemoteErrorKind.Generic -> listOf(
        "电脑未启动 ZCode",
        "Remote Control 已关闭",
        "网络连接异常",
        "Remote Session 已过期",
    )
}


