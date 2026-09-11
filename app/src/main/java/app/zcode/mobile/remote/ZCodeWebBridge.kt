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

    /**
     * Puts [text] into the Remote page composer. With [send] the send button is pressed
     * once the editor has enabled it. evaluateJavascript cannot await a Promise, so the
     * click is a second, delayed evaluation.
     */
    fun fillComposer(webView: WebView, text: String, send: Boolean = false, onResult: (BridgeResult) -> Unit) {
        val payload = JSONObject.quote(text)
        val script = FILL_SCRIPT.replace("__PAYLOAD__", payload)
        webView.evaluateJavascript(script) { raw ->
            val result = when (raw?.trim('"')) {
                "filled" -> BridgeResult.Filled
                "navigated" -> BridgeResult.Navigated
                "not_found" -> BridgeResult.NotFound
                else -> BridgeResult.Unknown
            }
            if (result == BridgeResult.Filled && send) {
                clickSend(webView, attempt = 0) { onResult(BridgeResult.Filled) }
            } else {
                onResult(result)
            }
        }
    }

    /** The editor enables the send button on its next update; poll briefly for it. */
    private fun clickSend(webView: WebView, attempt: Int, done: () -> Unit) {
        main.postDelayed({
            webView.evaluateJavascript(SEND_SCRIPT) { raw ->
                val state = raw?.trim('"')
                AppLog.d(TAG, "composer send=$state attempt=$attempt")
                if (state == "disabled" && attempt < 12) clickSend(webView, attempt + 1, done) else done()
            }
        }, if (attempt == 0) 200L else 300L)
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

    enum class BridgeResult { Filled, Navigated, NotFound, Unknown }

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
              // ZCode's composer is a Lexical editor and exposes a programmatic setter.
              const composer = document.querySelector('[data-testid="v4-composer-input"]');
              if (!composer) {
                // On the task list there is no composer: open the session the desktop has
                // selected (or the most recent one) and let the caller retry.
                const row = document.querySelector('button[data-testid^="task-item-"][data-state="selected"]')
                  || document.querySelector('button[data-testid^="task-item-"]');
                if (row) { row.click(); return 'navigated'; }
              }
              const bridge = composer && composer.__zcodeLexicalInputE2E;
              if (bridge && typeof bridge.setText === 'function') {
                bridge.setText(text);
                if (bridge.focus) bridge.focus();
                return 'filled';
              }
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
              } else if (!document.execCommand('insertText', false, text)) {
                el.innerText = text;
                el.dispatchEvent(new InputEvent('input', { bubbles: true, data: text }));
              }
              return 'filled';
            })();
        """.trimIndent()

        private val SEND_SCRIPT = """
            (function() {
              const btn = document.querySelector('[data-testid="v4-composer-send"]');
              if (!btn) return 'not_found';
              if (btn.disabled || btn.getAttribute('aria-disabled') === 'true') return 'disabled';
              btn.click();
              return 'sent';
            })();
        """.trimIndent()
    }
}
