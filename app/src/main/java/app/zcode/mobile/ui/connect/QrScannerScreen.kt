package app.zcode.mobile.ui.connect

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.zcode.mobile.qr.QRScanner
import app.zcode.mobile.ui.components.CodeBlock
import app.zcode.mobile.ui.components.PageInset
import app.zcode.mobile.ui.components.Panel
import app.zcode.mobile.ui.components.PrimaryButton
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.components.SecondaryButton
import app.zcode.mobile.ui.theme.ZTheme
import app.zcode.mobile.util.RemoteUrl

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit,
) {
    val c = ZTheme.colors
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

    Column(modifier = Modifier.fillMaxSize().background(c.surface)) {
        ScreenHeader(title = "扫描二维码", onBack = onBack)
        when {
            previewUrl != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PageInset, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(32.dp))
                    Text("识别成功", color = c.fg, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    CodeBlock(text = RemoteUrl.redacted(previewUrl!!))
                    Text(
                        text = "完整地址已隐藏，连接后会加密保存在本机。",
                        color = c.fgSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton(text = "连接", onClick = { onScanned(previewUrl!!) })
                    SecondaryButton(text = "重新扫描", onClick = {
                        previewUrl = null
                        invalid = false
                    })
                }
            }
            !granted -> {
                Column(Modifier.padding(horizontal = PageInset, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("需要相机权限", color = c.fg, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("扫描电脑端 ZCode 的二维码需要使用相机。", color = c.fgSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                    Spacer(Modifier.height(4.dp))
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
                    Viewfinder(modifier = Modifier.align(Alignment.Center).size(240.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(PageInset),
                    ) {
                        Panel {
                            Text(
                                text = if (invalid) "这个二维码不是有效的 ZCode Remote 地址" else "对准电脑屏幕上的二维码",
                                color = if (invalid) c.danger else c.fg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "电脑端 ZCode 左下角 → 移动端远程控制",
                                color = c.fgSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Four corner brackets, like a camera viewfinder. */
@Composable
private fun Viewfinder(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val len = 28.dp.toPx()
        val stroke = 3.dp.toPx()
        val w = size.width
        val h = size.height
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(Color.White, Offset(x, y), Offset(x + len * dx, y), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(x, y), Offset(x, y + len * dy), stroke, StrokeCap.Round)
        }
        corner(0f, 0f, 1f, 1f)
        corner(w, 0f, -1f, 1f)
        corner(0f, h, 1f, -1f)
        corner(w, h, -1f, -1f)
    }
}
