package app.zcode.mobile.remote

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import app.zcode.mobile.util.AppLog

class SessionManager {
    private val cookies: CookieManager = CookieManager.getInstance()

    fun attach(webView: WebView) {
        cookies.setAcceptCookie(true)
        // The Remote page is single-origin; third-party cookies are not needed.
        cookies.setAcceptThirdPartyCookies(webView, false)
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
