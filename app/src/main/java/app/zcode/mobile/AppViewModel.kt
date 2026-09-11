package app.zcode.mobile

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.MutableContextWrapper
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.zcode.mobile.data.AppSettings
import app.zcode.mobile.data.SettingsStore
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.model.Device
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.notification.EventNotifier
import app.zcode.mobile.notification.TaskNotificationManager
import app.zcode.mobile.remote.SessionManager
import app.zcode.mobile.remote.RemotePageState
import app.zcode.mobile.remote.ZCodeDomObserver
import app.zcode.mobile.remote.ZCodeEventRepository
import app.zcode.mobile.remote.ZCodeRemoteManager
import app.zcode.mobile.remote.ZCodeWebBridge
import app.zcode.mobile.security.SecureStorage
import app.zcode.mobile.util.AppLog
import app.zcode.mobile.util.NetworkMonitor
import app.zcode.mobile.util.RemoteUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val secureStorage = SecureStorage(application)
    val sessionManager = SessionManager()
    val remoteManager = ZCodeRemoteManager(secureStorage, sessionManager)
    val settingsStore = SettingsStore(application)
    val networkMonitor = NetworkMonitor(application)
    val notifications = TaskNotificationManager(application)
    val observer = ZCodeDomObserver(application)

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    private val eventNotifier = EventNotifier(notifications) { settings.value }
    val events = ZCodeEventRepository(onAccepted = { eventNotifier.onEvent(it) })

    val webBridge = ZCodeWebBridge(
        onParsedEvent = { event ->
            events.ingestEvent(event)
        },
        onSnapshot = { snapshot ->
            events.ingestSnapshot(snapshot)
            if (snapshot.connectionHint == "expired") {
                events.ingestConnection(ConnectionState.SESSION_EXPIRED)
            }
        },
        onTaskEvent = { title, summary ->
            events.ingestEvent(
                TaskCompleted(
                    taskId = "legacy-${title.hashCode()}",
                    title = title.ifBlank { "ZCode" },
                    summary = summary,
                ),
            )
        },
    )

    val device: StateFlow<Device?> = remoteManager.device
    val online: StateFlow<Boolean> = networkMonitor.online.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        networkMonitor.isOnline(),
    )

    /**
     * Load state of the shared WebView. It lives here, not in RemoteScreen, because the
     * same WebView is mounted hidden on Home; the page usually finishes loading there,
     * and a screen-local state would never see that onPageFinished.
     */
    private val _pageState = MutableStateFlow<RemotePageState>(RemotePageState.Idle)
    val pageState: StateFlow<RemotePageState> = _pageState.asStateFlow()

    fun updatePageState(state: RemotePageState) {
        _pageState.value = state
    }

    private val _pageProgress = MutableStateFlow(0)
    val pageProgress: StateFlow<Int> = _pageProgress.asStateFlow()

    fun updatePageProgress(progress: Int) {
        _pageProgress.value = progress
    }

    /** Last ~200 WebView diagnostics (navigation, console, errors); sanitized, shown in Developer. */
    private val _webLog = MutableStateFlow<List<String>>(emptyList())
    val webLog: StateFlow<List<String>> = _webLog.asStateFlow()
    private val webLogFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)

    fun logWeb(line: String) {
        val entry = "${webLogFormat.format(java.util.Date())} ${AppLog.sanitize(line)}"
        _webLog.value = (_webLog.value + entry).takeLast(200)
    }

    /** The renderer died; the WebView is unusable. Drop it so the next mount builds a new one. */
    fun onRendererGone() {
        runCatching { webView?.destroy() }
        webView = null
    }

    private val _pendingInject = MutableStateFlow<String?>(null)
    val pendingInject: StateFlow<String?> = _pendingInject.asStateFlow()

    private val _pendingShare = MutableStateFlow<String?>(null)
    val pendingShare: StateFlow<String?> = _pendingShare.asStateFlow()

    private val _openApproval = MutableStateFlow<String?>(null)
    val openApproval: StateFlow<String?> = _openApproval.asStateFlow()

    private val _openTaskId = MutableStateFlow<String?>(null)
    val openTaskId: StateFlow<String?> = _openTaskId.asStateFlow()

    var lastApproval: ApprovalRequest = ApprovalRequest.demo()
        private set

    var webView: WebView? = null
        private set

    /**
     * The WebView outlives the Activity, so it is created on a [MutableContextWrapper]
     * around the application context. [ZCodeWebView] swaps the base context to the
     * hosting Activity while attached (JS dialogs / file choosers need a window) and back
     * to the application context on release, so no Activity is ever retained here.
     */
    fun ensureWebView(context: Context): WebView? {
        val existing = webView
        if (existing != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing
        }
        return runCatching { WebView(MutableContextWrapper(context.applicationContext)) }
            .onFailure { AppLog.e("AppViewModel", "WebView create failed", it) }
            .getOrNull()
            ?.also { webView = it }
    }

    override fun onCleared() {
        runCatching { webView?.destroy() }
        webView = null
        super.onCleared()
    }

    fun saveConnection(url: String): Boolean {
        if (!RemoteUrl.isValid(url)) return false
        remoteManager.save(url, "ZCode Desktop")
        return true
    }

    fun disconnect(clearWeb: Boolean = false) {
        webView?.stopLoading()
        _pageState.value = RemotePageState.Idle
        if (clearWeb) {
            webView?.destroy()
            webView = null
        }
        remoteManager.disconnect(clearWeb)
        clearEvents()
    }

    fun clearWebViewData() {
        sessionManager.clear()
        webView?.clearCache(true)
        webView?.clearHistory()
        webView?.clearFormData()
        clearEvents()
    }

    private fun clearEvents() {
        events.clear()
        eventNotifier.reset()
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

    fun consumeApprovalNav(): String? {
        val id = _openApproval.value
        _openApproval.value = null
        return id
    }

    fun consumeTaskNav(): String? {
        val id = _openTaskId.value
        _openTaskId.value = null
        return id
    }

    fun requestOpenApproval(id: String? = null, request: ApprovalRequest? = null): Boolean {
        val resolved = request
            ?: id?.let { events.approvalById(it) }
            ?: events.approvals.value.firstOrNull()
            ?: if (settings.value.developerMode || BuildConfig.DEBUG) ApprovalRequest.demo() else null
        if (resolved != null) {
            lastApproval = resolved
            _openApproval.value = resolved.id
            return true
        }
        return false
    }

    fun requestOpenTask(id: String) {
        _openTaskId.value = id
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

    fun injectDeveloperFixture() {
        if (!BuildConfig.DEBUG && !settings.value.developerMode) return
        val json = """
            {
              "url":"https://remote.local/session",
              "title":"Legal SkillsHub",
              "connectionHint":"ok",
              "sessionId":"demo-session",
              "sessionTitle":"Legal SkillsHub",
              "observerActive":true,
              "tasks":[
                {"id":"t-run","title":"Legal SkillsHub","status":"运行中","step":"Agent 正在修改文件"},
                {"id":"t-wait","title":"代码清理","status":"等待确认","step":"需要你的确认"},
                {"id":"t-done","title":"案例整理","status":"已完成","step":"已完成"}
              ],
              "approval":{
                "id":"ap-1","title":"需要确认","description":"删除旧构建文件",
                "command":"rm -rf dist/","hasDialog":true,"hasAllow":true,"hasReject":true,
                "waitingContext":true
              },
              "artifacts":[{"id":"a1","name":"implementation.md","href":"implementation.md"}],
              "messages":[{"id":"m1","text":"正在修改首页 React 组件"}]
            }
        """.trimIndent()
        ZCodeEventParserSafe.ingest(events, json)
    }

}

private object ZCodeEventParserSafe {
    fun ingest(repo: ZCodeEventRepository, json: String) {
        app.zcode.mobile.remote.ZCodeEventParser.parseSnapshot(json)?.let { repo.ingestSnapshot(it) }
    }
}
