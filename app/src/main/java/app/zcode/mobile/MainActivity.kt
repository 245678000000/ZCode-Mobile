package app.zcode.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.navigation.Routes
import app.zcode.mobile.notification.TaskNotificationManager
import app.zcode.mobile.remote.RemoteWebConfig
import app.zcode.mobile.remote.ZCodeWebView
import app.zcode.mobile.ui.approval.ApprovalScreen
import app.zcode.mobile.ui.connect.ConnectScreen
import app.zcode.mobile.ui.connect.QrScannerScreen
import app.zcode.mobile.ui.developer.DeveloperScreen
import app.zcode.mobile.ui.home.HomeScreen
import app.zcode.mobile.ui.preview.PreviewScreen
import app.zcode.mobile.ui.remote.RemoteScreen
import app.zcode.mobile.ui.settings.SettingsScreen
import app.zcode.mobile.ui.splash.SplashScreen
import app.zcode.mobile.ui.task.TaskDetailScreen
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.ZCodeTheme
import app.zcode.mobile.ui.voice.VoiceScreen
import app.zcode.mobile.util.RemoteUrl

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        consumeIntent(intent)
        setContent {
            ZCodeTheme {
                val nav = rememberNavController()
                val device by appViewModel.device.collectAsStateWithLifecycle()
                val settings by appViewModel.settings.collectAsStateWithLifecycle()
                val online by appViewModel.online.collectAsStateWithLifecycle()
                val pendingInject by appViewModel.pendingInject.collectAsStateWithLifecycle()
                val pendingShare by appViewModel.pendingShare.collectAsStateWithLifecycle()
                val openApproval by appViewModel.openApproval.collectAsStateWithLifecycle()
                val openTaskId by appViewModel.openTaskId.collectAsStateWithLifecycle()
                val tasks by appViewModel.events.tasks.collectAsStateWithLifecycle()
                val approvals by appViewModel.events.approvals.collectAsStateWithLifecycle()
                val artifacts by appViewModel.events.artifacts.collectAsStateWithLifecycle()
                val events by appViewModel.events.events.collectAsStateWithLifecycle()
                val connection by appViewModel.events.connectionState.collectAsStateWithLifecycle()
                val observerActive by appViewModel.events.observerActive.collectAsStateWithLifecycle()
                val lastUrl by appViewModel.events.lastUrl.collectAsStateWithLifecycle()
                var scannedUrl by remember { mutableStateOf<String?>(null) }
                var previewArtifact by remember { mutableStateOf<Artifact?>(null) }
                var selectedTaskId by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(pendingShare) {
                    val shared = pendingShare ?: return@LaunchedEffect
                    val text = appViewModel.consumeShare() ?: return@LaunchedEffect
                    if (RemoteUrl.looksLikeRemoteUrl(text) || RemoteUrl.isValid(text)) {
                        scannedUrl = RemoteUrl.parse(text)?.raw
                        nav.navigate(Routes.Connect) { launchSingleTop = true }
                    } else {
                        appViewModel.queueInject(text)
                        if (device != null) {
                            nav.navigate(Routes.Remote) { launchSingleTop = true }
                        } else {
                            nav.navigate(Routes.Connect) { launchSingleTop = true }
                        }
                    }
                }

                LaunchedEffect(openApproval) {
                    if (openApproval != null) {
                        appViewModel.consumeApprovalNav()
                        nav.navigate(Routes.Approval)
                    }
                }
                LaunchedEffect(openTaskId) {
                    val id = openTaskId ?: return@LaunchedEffect
                    appViewModel.consumeTaskNav()
                    selectedTaskId = id
                    nav.navigate(Routes.TaskDetail)
                }

                NavHost(
                    navController = nav,
                    startDestination = Routes.Splash,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink)
                        .systemBarsPadding(),
                ) {
                    composable(Routes.Splash) {
                        SplashScreen(hasConnection = device != null) { has ->
                            if (has) {
                                nav.navigate(Routes.Home) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            } else {
                                nav.navigate(Routes.Connect) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(Routes.Connect) {
                        ConnectScreen(
                            initialUrl = scannedUrl,
                            onScan = { nav.navigate(Routes.QrScan) },
                            onConnect = { url ->
                                val ok = appViewModel.saveConnection(url)
                                if (ok) {
                                    scannedUrl = null
                                    nav.navigate(Routes.Home) {
                                        popUpTo(Routes.Connect) { inclusive = true }
                                    }
                                }
                                ok
                            },
                        )
                    }
                    composable(Routes.QrScan) {
                        QrScannerScreen(
                            onBack = { nav.popBackStack() },
                            onScanned = { url ->
                                scannedUrl = url
                                nav.popBackStack()
                            },
                        )
                    }
                    composable(Routes.Home) {
                        HomeScreen(
                            device = device,
                            connection = connection,
                            tasks = appViewModel.events.sortedTasks(),
                            onOpenRemote = { nav.navigate(Routes.Remote) },
                            onTask = { task ->
                                selectedTaskId = task.id
                                nav.navigate(Routes.TaskDetail)
                            },
                            onApproval = { task ->
                                val related = approvals.firstOrNull { it.taskId == task.id }
                                    ?: approvals.firstOrNull()
                                if (appViewModel.requestOpenApproval(id = related?.id, request = related)) {
                                    nav.navigate(Routes.Approval)
                                } else {
                                    nav.navigate(Routes.Remote)
                                }
                            },
                            onVoice = { nav.navigate(Routes.Voice) },
                            onReconnect = {
                                appViewModel.remoteManager.reconnect()
                                nav.navigate(Routes.Remote)
                            },
                            onChangeDevice = { nav.navigate(Routes.Connect) },
                            onSettings = { nav.navigate(Routes.Settings) },
                            observerSlot = {
                                val url = device?.remoteUrl
                                if (!url.isNullOrBlank()) {
                                    ZCodeWebView(
                                        config = RemoteWebConfig(
                                            remoteUrl = url,
                                            allowDownloads = settings.allowDownloads,
                                            allowExternalLinks = settings.allowExternalLinks,
                                            allowFileAccess = false,
                                        ),
                                        sessionManager = appViewModel.sessionManager,
                                        bridge = appViewModel.webBridge,
                                        modifier = Modifier.size(1.dp),
                                        observer = appViewModel.observer,
                                        retainedWebView = appViewModel.ensureWebView(this@MainActivity),
                                        onState = { },
                                        onDownload = { u, n, m -> appViewModel.enqueueDownload(u, n, m) },
                                        onConnection = { appViewModel.events.ingestConnection(it) },
                                        webViewRef = { },
                                    )
                                }
                            },
                        )
                    }
                    composable(Routes.Remote) {
                        val url = device?.remoteUrl
                        if (url.isNullOrBlank()) {
                            LaunchedEffect(Unit) {
                                nav.navigate(Routes.Connect) {
                                    popUpTo(Routes.Remote) { inclusive = true }
                                }
                            }
                        } else {
                            RemoteScreen(
                                remoteUrl = url,
                                settings = settings,
                                online = online,
                                sessionManager = appViewModel.sessionManager,
                                bridge = appViewModel.webBridge,
                                observer = appViewModel.observer,
                                retainedWebView = appViewModel.ensureWebView(this@MainActivity),
                                pendingInject = pendingInject,
                                onConsumeInject = { appViewModel.consumeInject() },
                                onConnected = { appViewModel.remoteManager.markConnected(it) },
                                onConnection = { appViewModel.events.ingestConnection(it) },
                                onDownload = { downloadUrl, name, mime ->
                                    appViewModel.enqueueDownload(downloadUrl, name, mime)
                                },
                                onWebView = { },
                                onBackToHome = {
                                    if (!nav.popBackStack(Routes.Home, false)) {
                                        nav.navigate(Routes.Home) {
                                            popUpTo(Routes.Remote) { inclusive = true }
                                        }
                                    }
                                },
                                onDisconnect = {
                                    appViewModel.disconnect(clearWeb = false)
                                    nav.navigate(Routes.Connect) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onReconnect = {
                                    appViewModel.remoteManager.reconnect()
                                    appViewModel.webView?.reload()
                                },
                            )
                        }
                    }
                    composable(Routes.Voice) {
                        VoiceScreen(
                            onBack = { nav.popBackStack() },
                            onSendToZCode = { text ->
                                appViewModel.queueInject(text)
                                nav.navigate(Routes.Remote) { launchSingleTop = true }
                            },
                        )
                    }
                    composable(Routes.Preview) {
                        val artifact = previewArtifact
                        if (artifact == null) {
                            LaunchedEffect(Unit) { nav.popBackStack() }
                        } else {
                            PreviewScreen(artifact = artifact, onBack = { nav.popBackStack() })
                        }
                    }
                    composable(Routes.TaskDetail) {
                        val task = selectedTaskId?.let { appViewModel.events.taskById(it) }
                            ?: appViewModel.events.activeTask
                        if (task == null) {
                            LaunchedEffect(Unit) { nav.popBackStack() }
                        } else {
                            TaskDetailScreen(
                                task = task,
                                events = events.filter { ev ->
                                    ev.toString().contains(task.id) || ev.type.contains("Task") || ev.type.contains("Message")
                                },
                                artifacts = artifacts.filter { it.taskId == null || it.taskId == task.id },
                                approval = approvals.firstOrNull { it.taskId == task.id } ?: approvals.firstOrNull(),
                                onBack = { nav.popBackStack() },
                                onOpenRemote = { nav.navigate(Routes.Remote) },
                                onOpenApproval = { nav.navigate(Routes.Approval) },
                                onOpenArtifact = { art ->
                                    if (art.localUri != null) {
                                        previewArtifact = art
                                        nav.navigate(Routes.Preview)
                                    } else {
                                        nav.navigate(Routes.Remote)
                                    }
                                },
                            )
                        }
                    }
                    composable(Routes.Approval) {
                        ApprovalScreen(
                            request = appViewModel.lastApproval,
                            onBack = { nav.popBackStack() },
                            onOpenRemote = { nav.navigate(Routes.Remote) },
                            onAllow = {
                                appViewModel.tryNativeApproval(true) { ok, _ ->
                                    if (!ok) nav.navigate(Routes.Remote)
                                    else nav.popBackStack()
                                }
                            },
                            onDeny = {
                                appViewModel.tryNativeApproval(false) { ok, _ ->
                                    if (!ok) nav.navigate(Routes.Remote)
                                    else nav.popBackStack()
                                }
                            },
                        )
                    }
                    composable(Routes.Developer) {
                        DeveloperScreen(
                            url = lastUrl.ifBlank { device?.remoteUrl?.let { RemoteUrl.redacted(it) }.orEmpty() },
                            connection = connection,
                            observerActive = observerActive,
                            events = events,
                            tasks = tasks,
                            approvals = approvals,
                            artifacts = artifacts,
                            dump = appViewModel.events.sanitizedDebugDump(),
                            onBack = { nav.popBackStack() },
                            onInjectFixture = { appViewModel.injectDeveloperFixture() },
                        )
                    }
                    composable(Routes.Settings) {
                        SettingsScreen(
                            device = device,
                            settings = settings,
                            store = appViewModel.settingsStore,
                            onBack = { nav.popBackStack() },
                            onReconnect = { nav.navigate(Routes.Remote) },
                            onClearConnection = {
                                appViewModel.disconnect(clearWeb = true)
                                nav.navigate(Routes.Connect) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onClearWebData = { appViewModel.clearWebViewData() },
                            onDemoNotification = {
                                appViewModel.injectDeveloperFixture()
                            },
                            onDemoApproval = {
                                appViewModel.requestOpenApproval(request = app.zcode.mobile.model.ApprovalRequest.demo())
                            },
                            onPreviewSample = {
                                previewArtifact = appViewModel.sampleArtifact()
                                nav.navigate(Routes.Preview)
                            },
                            onDeveloper = { nav.navigate(Routes.Developer) },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(TaskNotificationManager.EXTRA_OPEN_APPROVAL, false)) {
            val id = intent.getStringExtra(TaskNotificationManager.EXTRA_APPROVAL_ID)
            appViewModel.requestOpenApproval(id = id)
        }
        val taskId = intent.getStringExtra(TaskNotificationManager.EXTRA_OPEN_TASK_ID)
        if (!taskId.isNullOrBlank()) {
            appViewModel.requestOpenTask(taskId)
        }
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                appViewModel.handleSharedText(text)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        appViewModel.sessionManager.persist()
    }
}
