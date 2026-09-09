package app.zcode.mobile.ui.connect

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.zcode.mobile.qr.QRScanner
import app.zcode.mobile.ui.components.GhostButton
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sand
import app.zcode.mobile.util.RemoteUrl

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var invalid by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(title = "扫描二维码", onBack = onBack)
        when {
            previewUrl != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    Text("识别成功", color = Paper, fontSize = 22.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = RemoteUrl.redacted(previewUrl!!),
                        color = Sand,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "连接信息已隐藏完整路径。点击连接后将安全保存。",
                        color = Mute,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton(text = "连接", onClick = { onScanned(previewUrl!!) })
                    Spacer(Modifier.height(10.dp))
                    GhostButton(text = "重新扫描", onClick = {
                        previewUrl = null
                        invalid = false
                    })
                }
            }
            !granted -> {
                Column(Modifier.padding(24.dp)) {
                    Text("需要相机权限才能扫描二维码。", color = Mute, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(text = "授予权限", onClick = { launcher.launch(Manifest.permission.CAMERA) })
                }
            }
            else -> {
                Box(Modifier.fillMaxSize()) {
                    QRScanner(
                        onQr = { value ->
                            val parsed = RemoteUrl.parse(value)
                            if (parsed != null) {
                                previewUrl = parsed.raw
                                invalid = false
                            } else {
                                invalid = true
                            }
                        },
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(240.dp)
                            .border(1.dp, Sand.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (invalid) "二维码不是有效的 http/https 地址" else "对准 ZCode Remote 二维码",
                            color = Paper,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
