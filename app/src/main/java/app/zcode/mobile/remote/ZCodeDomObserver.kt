package app.zcode.mobile.remote

import android.content.Context
import android.webkit.WebView
import app.zcode.mobile.util.AppLog

class ZCodeDomObserver(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val selectors: ZCodeSelectorConfig = runCatching { ZCodeSelectorConfig.load(appContext) }
        .getOrElse {
            AppLog.e(TAG, "selector config missing", it)
            ZCodeSelectorConfig.fromJson("{}")
        }
    private val source: String = runCatching {
        appContext.assets.open("zcode-observer.js").bufferedReader().use { it.readText() }
    }.getOrDefault("function(){return 'missing';}")

    @Volatile
    var installed: Boolean = false
        private set

    fun install(webView: WebView) {
        val script = "(${source})(${selectors.toJson()});"
        webView.post {
            runCatching {
                webView.evaluateJavascript(script) { result ->
                    installed = result?.contains("ok") == true || result?.contains("already") == true
                    AppLog.d(TAG, "observer install=$result")
                }
            }.onFailure {
                installed = false
                AppLog.e(TAG, "observer inject failed", it)
            }
        }
    }

    fun scan(webView: WebView) {
        webView.post {
            webView.evaluateJavascript("window.__ZCodeScan && window.__ZCodeScan('manual');") {}
        }
    }

    companion object {
        private const val TAG = "ZCodeDomObserver"
    }
}
