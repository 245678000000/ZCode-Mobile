package app.zcode.mobile.remote

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.zcode.mobile.BuildConfig
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.util.AppLog
import app.zcode.mobile.util.RemoteUrl

sealed class RemotePageState {
    data object Idle : RemotePageState()
    data object Loading : RemotePageState()
    data class Ready(val title: String?) : RemotePageState()
    data class Error(val kind: RemoteErrorKind, val detail: String? = null) : RemotePageState()
}

enum class RemoteErrorKind {
    Offline,
    Dns,
    Ssl,
    Http404,
    Http500,
    SessionExpired,
    Timeout,
    Generic,
}

data class RemoteWebConfig(
    val remoteUrl: String,
    val allowDownloads: Boolean,
    val allowExternalLinks: Boolean,
    val allowFileAccess: Boolean,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZCodeWebView(
    config: RemoteWebConfig,
    sessionManager: SessionManager,
    bridge: ZCodeWebBridge,
    modifier: Modifier = Modifier,
    observer: ZCodeDomObserver? = null,
    retainedWebView: WebView? = null,
    onState: (RemotePageState) -> Unit,
    onDownload: (String, String?, String?) -> Unit,
    onConnection: (ConnectionState) -> Unit = {},
    webViewRef: (WebView) -> Unit,
) {
    val origin = remember(config.remoteUrl) { RemoteUrl.origin(config.remoteUrl) }
    val httpsRemote = remember(config.remoteUrl) { RemoteUrl.parse(config.remoteUrl)?.isHttps == true }

    DisposableEffect(Unit) {
        onDispose { sessionManager.persist() }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val webView = retainedWebView?.also { existing ->
                (existing.parent as? ViewGroup)?.removeView(existing)
            } ?: WebView(MutableContextWrapper(context.applicationContext))
            // Bind to the hosting Activity only while attached; released below.
            (webView.context as? MutableContextWrapper)?.baseContext = context
            webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                configureSettings(this, config, httpsRemote)
                sessionManager.attach(this)
                removeJavascriptInterface(ZCodeWebBridge.JS_NAME)
                removeJavascriptInterface(ZCodeWebBridge.LEGACY_JS_NAME)
                addJavascriptInterface(bridge, ZCodeWebBridge.JS_NAME)
                addJavascriptInterface(bridge, ZCodeWebBridge.LEGACY_JS_NAME)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onState(RemotePageState.Loading)
                        onConnection(ConnectionState.CONNECTING)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        sessionManager.persist()
                        onState(RemotePageState.Ready(view?.title))
                        onConnection(ConnectionState.CONNECTED)
                        view?.let { observer?.install(it) }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        return handleUrl(context, url, origin, config.allowExternalLinks)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        val target = url ?: return false
                        return handleUrl(context, target, origin, config.allowExternalLinks)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame != true) return
                        val kind = when (error?.errorCode) {
                            ERROR_HOST_LOOKUP -> RemoteErrorKind.Dns
                            ERROR_TIMEOUT -> RemoteErrorKind.Timeout
                            ERROR_CONNECT, ERROR_FAILED_SSL_HANDSHAKE -> RemoteErrorKind.Ssl
                            else -> RemoteErrorKind.Generic
                        }
                        onState(RemotePageState.Error(kind, error?.description?.toString()))
                        onConnection(ConnectionState.ERROR)
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        if (request?.isForMainFrame != true) return
                        when (val code = errorResponse?.statusCode ?: return) {
                            401, 403 -> {
                                onState(RemotePageState.Error(RemoteErrorKind.SessionExpired, "HTTP $code"))
                                onConnection(ConnectionState.SESSION_EXPIRED)
                            }
                            404 -> onState(RemotePageState.Error(RemoteErrorKind.Http404, "HTTP $code"))
                            in 500..599 -> onState(RemotePageState.Error(RemoteErrorKind.Http500, "HTTP $code"))
                            else -> Unit
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.cancel()
                        onState(RemotePageState.Error(RemoteErrorKind.Ssl, "TLS error"))
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message?,
                    ): Boolean {
                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                        // Throwaway WebView: only used to learn the popup's target URL, then destroyed.
                        val temp = WebView(context.applicationContext)
                        temp.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return true
                                handleUrl(context, url, origin, config.allowExternalLinks, loadInParent = {
                                    this@apply.loadUrl(it)
                                })
                                view?.post { runCatching { view.destroy() } }
                                return true
                            }
                        }
                        transport.webView = temp
                        resultMsg.sendToTarget()
                        return true
                    }
                }
                setDownloadListener(DownloadListener { url, _, contentDisposition, mimeType, _ ->
                    if (!config.allowDownloads) return@DownloadListener
                    val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    onDownload(url, name, mimeType)
                })
                webViewRef(this)
                val current = url
                if (current.isNullOrBlank() || current == "about:blank") {
                    loadUrl(config.remoteUrl)
                }
            }
        },
        update = { view ->
            webViewRef(view)
        },
        onRelease = { view ->
            sessionManager.persist()
            // Drop the Activity reference; the WebView itself is retained by the ViewModel.
            (view.context as? MutableContextWrapper)?.baseContext = view.context.applicationContext
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureSettings(webView: WebView, config: RemoteWebConfig, httpsRemote: Boolean) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        loadWithOverviewMode = true
        useWideViewPort = true
        cacheMode = WebSettings.LOAD_DEFAULT
        mixedContentMode = if (httpsRemote) {
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        } else {
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        allowFileAccess = config.allowFileAccess
        allowContentAccess = config.allowFileAccess
        safeBrowsingEnabled = true
        if (!userAgentString.contains("ZCodeMobile/")) {
            userAgentString = "$userAgentString ZCodeMobile/${BuildConfig.VERSION_NAME}"
        }
    }
    CookieManager.getInstance().setAcceptCookie(true)
    WebView.setWebContentsDebuggingEnabled(false)
}

private fun handleUrl(
    context: Context,
    url: String,
    origin: String?,
    allowExternal: Boolean,
    loadInParent: ((String) -> Unit)? = null,
): Boolean {
    val scheme = runCatching { Uri.parse(url).scheme?.lowercase() }.getOrNull()
    if (scheme == "http" || scheme == "https") {
        val same = origin != null && RemoteUrl.isSameOrigin(origin, url)
        if (same) {
            loadInParent?.invoke(url)
            return loadInParent != null
        }
        if (allowExternal) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } else {
            AppLog.d("ZCodeWebView", "blocked external navigation")
        }
        return true
    }
    if (scheme == "about" || scheme == "javascript") return false
    if (scheme == "intent" || scheme == "market") return true
    if (scheme == "file" || scheme == "content") return true
    return true
}
