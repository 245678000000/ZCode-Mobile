package app.zcode.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.navigation.Routes
import app.zcode.mobile.notification.TaskNotificationManager
import app.zcode.mobile.ui.approval.ApprovalScreen
import app.zcode.mobile.ui.connect.ConnectScreen
import app.zcode.mobile.ui.connect.QrScannerScreen
import app.zcode.mobile.ui.home.HomeScreen
import app.zcode.mobile.ui.preview.PreviewScreen
import app.zcode.mobile.ui.remote.RemoteScreen
import app.zcode.mobile.ui.settings.SettingsScreen
import app.zcode.mobile.ui.splash.SplashScreen
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
                var scannedUrl by remember { mutableStateOf<String?>(null) }
                var previewArtifact by remember { mutableStateOf<Artifact?>(null) }

                LaunchedEffect(pendingShare) {
                    val shared = pendingShare ?: return@LaunchedEffect
                    val text = appViewModel.consumeShare() ?: return@LaunchedEffect
                    if (RemoteUrl.looksLikeRemoteUrl(text) || RemoteUrl.isValid(text)) {
                        scannedUrl = RemoteUrl.parse(text)?.raw
                        nav.navigate(Routes.Connect) {
                            launchSingleTop = true
                        }
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
                    if (openApproval) {
                        appViewModel.consumeApprovalNav()
                        nav.navigate(Routes.Approval)
                    }
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
                                nav.navigate(Routes.Remote) {
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
                            connected = device?.connected == true,
                            onOpenRemote = { nav.navigate(Routes.Remote) },
                            onVoice = { nav.navigate(Routes.Voice) },
                            onReconnect = {
                                appViewModel.remoteManager.reconnect()
                                nav.navigate(Routes.Remote)
                            },
                            onChangeDevice = {
                                nav.navigate(Routes.Connect)
                            },
                            onSettings = { nav.navigate(Routes.Settings) },
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
                                pendingInject = pendingInject,
                                onConsumeInject = { appViewModel.consumeInject() },
                                onConnected = { appViewModel.remoteManager.markConnected(it) },
                                onDownload = { downloadUrl, name, mime ->
                                    appViewModel.enqueueDownload(downloadUrl, name, mime)
                                },
                                onWebView = { appViewModel.webView = it },
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
                                },
                            )
                        }
                    }
                    composable(Routes.Voice) {
                        VoiceScreen(
                            onBack = { nav.popBackStack() },
                            onSendToZCode = { text ->
                                appViewModel.queueInject(text)
                                nav.navigate(Routes.Remote) {
                                    launchSingleTop = true
                                }
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
                    composable(Routes.Approval) {
                        ApprovalScreen(
                            request = appViewModel.lastApproval,
                            onBack = { nav.popBackStack() },
                            onAllow = { nav.popBackStack() },
                            onDeny = { nav.popBackStack() },
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
                                appViewModel.notifyTaskCompleted(
                                    "ZCode",
                                    "Legal SkillsHub 首页重构完成",
                                )
                            },
                            onDemoApproval = {
                                appViewModel.requestOpenApproval()
                            },
                            onPreviewSample = {
                                previewArtifact = appViewModel.sampleArtifact()
                                nav.navigate(Routes.Preview)
                            },
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
            appViewModel.requestOpenApproval()
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
