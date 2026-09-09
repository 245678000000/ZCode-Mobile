package app.zcode.mobile.remote

import android.webkit.JavascriptInterface
import android.webkit.WebView
import app.zcode.mobile.util.AppLog
import org.json.JSONObject

/**
 * Generic, selector-tolerant bridge. Does not hard-code ZCode DOM ids.
 * Tries common chat/composer fields, then falls back to clipboard-style no-op.
 */
class ZCodeWebBridge(
    private val onTaskEvent: (String, String) -> Unit = { _, _ -> },
) {
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
    fun onTaskCompleted(title: String, summary: String) {
        AppLog.d(TAG, "task event from page")
        onTaskEvent(title, summary)
    }

    enum class BridgeResult { Filled, NotFound, Unknown }

    companion object {
        private const val TAG = "ZCodeWebBridge"
        const val JS_NAME = "ZCodeMobile"

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
