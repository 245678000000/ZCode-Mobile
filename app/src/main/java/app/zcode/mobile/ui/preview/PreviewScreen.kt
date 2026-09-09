package app.zcode.mobile.ui.preview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Intent
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.ArtifactKind
import app.zcode.mobile.ui.components.Hairline
import app.zcode.mobile.ui.components.ScreenHeader
import app.zcode.mobile.ui.theme.Ink
import app.zcode.mobile.ui.theme.InkRaised
import app.zcode.mobile.ui.theme.Mute
import app.zcode.mobile.ui.theme.Paper
import app.zcode.mobile.ui.theme.Sand
import java.io.File
import java.nio.charset.Charset

@Composable
fun PreviewScreen(
    artifact: Artifact,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        ScreenHeader(
            title = artifact.name,
            onBack = onBack,
            trailing = {
                TextButton(onClick = { share(context, artifact) }) { Text("Share", color = Sand) }
                TextButton(onClick = { openExternal(context, artifact) }) { Text("Open", color = Sand) }
            },
        )
        Hairline()
        when (artifact.kind) {
            ArtifactKind.Image -> {
                AsyncImage(
                    model = artifact.uri,
                    contentDescription = artifact.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            ArtifactKind.Html -> HtmlPreview(readText(context, artifact).orEmpty())
            ArtifactKind.Markdown -> HtmlPreview(markdownToHtml(readText(context, artifact).orEmpty()))
            ArtifactKind.Pdf -> PdfPreview(artifact)
            ArtifactKind.Code, ArtifactKind.Json, ArtifactKind.Text, ArtifactKind.Unknown -> {
                val text = readText(context, artifact) ?: "无法读取文件。"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                        .background(InkRaised)
                        .padding(16.dp),
                ) {
                    Text(
                        text = text,
                        color = Paper,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HtmlPreview(html: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(0xFF0F1114.toInt())
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, wrapHtml(html), "text/html", "utf-8", null)
            }
        },
        update = { it.loadDataWithBaseURL(null, wrapHtml(html), "text/html", "utf-8", null) },
    )
}

@Composable
private fun PdfPreview(artifact: Artifact) {
    val context = LocalContext.current
    val bitmaps = remember(artifact.uri) { renderPdf(context, artifact) }
    DisposableEffect(bitmaps) { onDispose { bitmaps.forEach { it.recycle() } } }
    if (bitmaps.isEmpty()) {
        Text("无法预览 PDF", color = Mute, modifier = Modifier.padding(24.dp))
    } else {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            bitmaps.forEach { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = artifact.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
        }
    }
}

private fun renderPdf(context: android.content.Context, artifact: Artifact): List<Bitmap> {
    return runCatching {
        val file = fileFrom(context, artifact) ?: return emptyList()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pages = mutableListOf<Bitmap>()
        val count = minOf(renderer.pageCount, 12)
        for (i in 0 until count) {
            val page = renderer.openPage(i)
            val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pages += bmp
        }
        renderer.close()
        pfd.close()
        pages
    }.getOrDefault(emptyList())
}

private fun readText(context: android.content.Context, artifact: Artifact): String? {
    return runCatching {
        context.contentResolver.openInputStream(artifact.uri)?.use {
            it.readBytes().toString(Charset.forName("UTF-8"))
        } ?: fileFrom(context, artifact)?.readText()
    }.getOrNull()
}

private fun fileFrom(context: android.content.Context, artifact: Artifact): File? {
    val path = artifact.uri.path ?: return null
    val file = File(path)
    if (file.exists()) return file
    return runCatching {
        val tmp = File(context.cacheDir, artifact.name)
        context.contentResolver.openInputStream(artifact.uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        tmp.takeIf { it.exists() }
    }.getOrNull()
}

private fun share(context: android.content.Context, artifact: Artifact) {
    val uri = shareableUri(context, artifact)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = artifact.mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

private fun openExternal(context: android.content.Context, artifact: Artifact) {
    val uri = shareableUri(context, artifact)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, artifact.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

private fun shareableUri(context: android.content.Context, artifact: Artifact): android.net.Uri {
    if (artifact.uri.scheme == "content") return artifact.uri
    val file = fileFrom(context, artifact) ?: return artifact.uri
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun wrapHtml(body: String): String = """
    <html><head>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <style>
      body { background:#0F1114; color:#ECEAE4; font:16px/1.55 -apple-system, sans-serif; padding:20px; }
      a { color:#C8B89A; } pre,code { font-family: ui-monospace, monospace; }
      pre { background:#171A1F; padding:12px; border-radius:10px; overflow:auto; }
      h1,h2,h3 { font-weight:500; }
    </style></head><body>$body</body></html>
""".trimIndent()

internal fun markdownToHtml(md: String): String {
    val escaped = md
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    val lines = escaped.split("\n")
    val out = StringBuilder()
    var inCode = false
    for (line in lines) {
        when {
            line.startsWith("```") -> {
                if (inCode) out.append("</pre>") else out.append("<pre>")
                inCode = !inCode
            }
            inCode -> out.append(line).append("\n")
            line.startsWith("### ") -> out.append("<h3>").append(line.removePrefix("### ")).append("</h3>")
            line.startsWith("## ") -> out.append("<h2>").append(line.removePrefix("## ")).append("</h2>")
            line.startsWith("# ") -> out.append("<h1>").append(line.removePrefix("# ")).append("</h1>")
            line.startsWith("- ") -> out.append("<div>· ").append(inlineMd(line.removePrefix("- "))).append("</div>")
            line.isBlank() -> out.append("<br/>")
            else -> out.append("<p>").append(inlineMd(line)).append("</p>")
        }
    }
    if (inCode) out.append("</pre>")
    return out.toString()
}

private fun inlineMd(line: String): String {
    return line
        .replace(Regex("`([^`]+)`"), "<code>$1</code>")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "<strong>$1</strong>")
        .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
}
