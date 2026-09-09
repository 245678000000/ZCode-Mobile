package app.zcode.mobile

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.zcode.mobile.data.AppSettings
import app.zcode.mobile.data.SettingsStore
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.Device
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.notification.TaskNotificationManager
import app.zcode.mobile.remote.SessionManager
import app.zcode.mobile.remote.ZCodeRemoteManager
import app.zcode.mobile.remote.ZCodeWebBridge
import app.zcode.mobile.security.SecureStorage
import app.zcode.mobile.util.NetworkMonitor
import app.zcode.mobile.util.RemoteUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val secureStorage = SecureStorage(application)
    val sessionManager = SessionManager()
    val remoteManager = ZCodeRemoteManager(secureStorage, sessionManager)
    val settingsStore = SettingsStore(application)
    val networkMonitor = NetworkMonitor(application)
    val notifications = TaskNotificationManager(application)
    val webBridge = ZCodeWebBridge { title, summary ->
        notifyTaskCompleted(title, summary)
    }

    val device: StateFlow<Device?> = remoteManager.device
    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )
    val online: StateFlow<Boolean> = networkMonitor.online.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        networkMonitor.isOnline(),
    )

    private val _pendingInject = MutableStateFlow<String?>(null)
    val pendingInject: StateFlow<String?> = _pendingInject.asStateFlow()

    private val _pendingShare = MutableStateFlow<String?>(null)
    val pendingShare: StateFlow<String?> = _pendingShare.asStateFlow()

    private val _openApproval = MutableStateFlow(false)
    val openApproval: StateFlow<Boolean> = _openApproval.asStateFlow()

    var lastApproval: ApprovalRequest = ApprovalRequest.demo()
        private set

    var webView: WebView? = null

    fun saveConnection(url: String): Boolean {
        if (!RemoteUrl.isValid(url)) return false
        remoteManager.save(url, "ZCode Desktop")
        return true
    }

    fun disconnect(clearWeb: Boolean = false) {
        webView?.stopLoading()
        remoteManager.disconnect(clearWeb)
        webView = null
    }

    fun clearWebViewData() {
        sessionManager.clear()
        webView?.clearCache(true)
        webView?.clearHistory()
        webView?.clearFormData()
    }

    fun queueInject(text: String) {
        _pendingInject.value = text
    }

    fun consumeInject(): String? {
        val value = _pendingInject.value
        _pendingInject.value = null
        return value
    }

    fun handleSharedText(text: String) {
        _pendingShare.value = text
    }

    fun consumeShare(): String? {
        val value = _pendingShare.value
        _pendingShare.value = null
        return value
    }

    fun consumeApprovalNav() {
        _openApproval.value = false
    }

    fun requestOpenApproval(request: ApprovalRequest = ApprovalRequest.demo()) {
        lastApproval = request
        _openApproval.value = true
        if (settings.value.approvalNotifications) {
            notifications.notifyApproval(request)
        }
    }

    fun notifyTaskCompleted(title: String, summary: String) {
        if (!settings.value.taskNotifications) return
        notifications.notifyTaskCompleted(
            Task(
                id = System.currentTimeMillis().toString(),
                title = title,
                summary = summary,
                status = TaskStatus.Completed,
            ),
        )
    }

    fun enqueueDownload(url: String, fileName: String?, mimeType: String?) {
        val context = getApplication<Application>()
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName ?: "ZCode download")
            setMimeType(mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName ?: "zcode-download",
            )
            val cookie = CookieManager.getInstance().getCookie(url)
            if (!cookie.isNullOrBlank()) {
                addRequestHeader("Cookie", cookie)
            }
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }

    fun sampleArtifact(): Artifact {
        val context = getApplication<Application>()
        val file = java.io.File(context.cacheDir, "welcome.md")
        if (!file.exists()) {
            context.assets.open("samples/welcome.md").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return Artifact.from("welcome.md", Uri.fromFile(file), "text/markdown")
    }
}
