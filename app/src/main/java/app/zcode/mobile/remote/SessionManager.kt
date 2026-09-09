package app.zcode.mobile.remote

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import app.zcode.mobile.util.AppLog

class SessionManager {
    private val cookies: CookieManager = CookieManager.getInstance()

    fun attach(webView: WebView) {
        cookies.setAcceptCookie(true)
        cookies.setAcceptThirdPartyCookies(webView, true)
    }

    fun persist() {
        cookies.flush()
    }

    fun clear() {
        cookies.removeAllCookies(null)
        cookies.flush()
        WebStorage.getInstance().deleteAllData()
        AppLog.d(TAG, "WebView session cleared")
    }

    companion object {
        private const val TAG = "SessionManager"
    }
}
