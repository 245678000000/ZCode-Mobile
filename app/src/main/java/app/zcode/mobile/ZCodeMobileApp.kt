package app.zcode.mobile

import android.app.Application
import android.os.Build
import android.webkit.WebView
import app.zcode.mobile.notification.TaskNotificationManager
import app.zcode.mobile.util.AppLog
import app.zcode.mobile.util.CrashFileLog

class ZCodeMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashFileLog.install(this)
        prepareWebViewDirectory()
        runCatching { TaskNotificationManager(this) }
            .onFailure { AppLog.e(TAG, "notification init failed", it) }
    }

    private fun prepareWebViewDirectory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val process = getProcessName()
        if (process != packageName) {
            val suffix = process.substringAfter(':', process).ifBlank { "bg" }
            runCatching { WebView.setDataDirectorySuffix(suffix) }
                .onFailure { AppLog.e(TAG, "webview data dir failed", it) }
        }
    }

    companion object {
        private const val TAG = "ZCodeMobileApp"
    }
}
