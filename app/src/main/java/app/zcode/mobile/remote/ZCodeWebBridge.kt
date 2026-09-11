package app.zcode.mobile.remote

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import app.zcode.mobile.util.AppLog
import org.json.JSONObject

class ZCodeWebBridge(
    private val onParsedEvent: (app.zcode.mobile.model.ZCodeEvent) -> Unit = {},
    private val onSnapshot: (app.zcode.mobile.model.PageSnapshot) -> Unit = {},
    private val onTaskEvent: (String, String) -> Unit = { _, _ -> },
) {
    private val main = Handler(Looper.getMainLooper())

    fun fillComposer(webView: WebView, text: String, onResult: (BridgeResult) -> Unit) {
        val payload = JSONObject.quote(text)
        val script = FILL_SCRIPT.replace("__PAYLOAD__", payload)
        webView.evaluateJavascript(script) { raw ->
            val result = when (raw?.trim('"')) {
                "filled" -> BridgeResult.Filled
                "not_found" -> BridgeResult.NotFound
                else -> BridgeResult.Unknown
            }
            onResult(result)
        }
    }

    @JavascriptInterface
    fun onEvent(json: String) {
        if (!validPayload(json)) return
        val event = ZCodeEventParser.parseEvent(json) ?: return
        main.post { onParsedEvent(event) }
    }

    @JavascriptInterface
    fun onPageState(json: String) {
        if (!validPayload(json)) return
        val snapshot = ZCodeEventParser.parseSnapshot(json) ?: return
        main.post { onSnapshot(snapshot) }
    }

    @JavascriptInterface
    fun onApprovalDetected(json: String) {
        if (!validPayload(json)) return
        val wrapped = wrapType(json, "ApprovalRequired")
        val event = ZCodeEventParser.parseEvent(wrapped) ?: return
        main.post { onParsedEvent(event) }
    }

    @JavascriptInterface
    fun onArtifactDetected(json: String) {
        if (!validPayload(json)) return
        val wrapped = wrapType(json, "ArtifactCreated")
        val event = ZCodeEventParser.parseEvent(wrapped) ?: return
        main.post { onParsedEvent(event) }
    }

    @JavascriptInterface
    fun onTaskCompleted(title: String, summary: String) {
        AppLog.d(TAG, "legacy task event")
        main.post { onTaskEvent(SensitiveSanitizer.text(title), SensitiveSanitizer.text(summary)) }
    }

    private fun validPayload(json: String): Boolean {
        if (json.isBlank() || json.length > ZCodeEventParser.MAX_BYTES) return false
        return json.trimStart().startsWith("{")
    }

    private fun wrapType(json: String, type: String): String {
        return try {
            val obj = JSONObject(json)
            if (!obj.has("type")) obj.put("type", type)
            if (!obj.has("timestamp")) obj.put("timestamp", System.currentTimeMillis())
            obj.toString()
        } catch (_: Exception) {
            json
        }
    }

    enum class BridgeResult { Filled, NotFound, Unknown }

    companion object {
        private const val TAG = "ZCodeWebBridge"
        const val JS_NAME = "ZCodeAndroidBridge"
        const val LEGACY_JS_NAME = "ZCodeMobile"

        private val FILL_SCRIPT = """
            (function() {
              const text = __PAYLOAD__;
              const isVisible = (el) => {
                if (!el) return false;
                const rect = el.getBoundingClientRect();
                const style = window.getComputedStyle(el);
                return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
              };
              const candidates = Array.from(document.querySelectorAll(
                'textarea, [contenteditable="true"], [contenteditable=""], input[type="text"], input:not([type])'
              )).filter(isVisible);
              const el = candidates.sort((a, b) => b.getBoundingClientRect().width - a.getBoundingClientRect().width)[0];
              if (!el) return 'not_found';
              el.focus();
              if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
                const proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
                const desc = Object.getOwnPropertyDescriptor(proto, 'value');
                if (desc && desc.set) desc.set.call(el, text); else el.value = text;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
              } else {
                el.innerText = text;
                el.dispatchEvent(new InputEvent('input', { bubbles: true, data: text }));
              }
              return 'filled';
            })();
        """.trimIndent()
    }
}
