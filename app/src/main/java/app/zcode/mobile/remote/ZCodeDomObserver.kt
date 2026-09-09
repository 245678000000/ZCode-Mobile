package app.zcode.mobile.remote

import android.content.Context
import android.webkit.WebView
import app.zcode.mobile.util.AppLog

class ZCodeDomObserver(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val selectors: ZCodeSelectorConfig = runCatching { ZCodeSelectorConfig.load(appContext) }
        .getOrElse {
            AppLog.e(TAG, "selector config missing", it)
            ZCodeSelectorConfig.fromJson("{}")
        }
    private val source: String = runCatching {
        appContext.assets.open("zcode-observer.js").bufferedReader().use { it.readText() }
    }.getOrDefault("function(){return 'missing';}")

    @Volatile
    var installed: Boolean = false
        private set

    fun install(webView: WebView) {
        val script = "(${source})(${selectors.toJson()});"
        webView.post {
            runCatching {
                webView.evaluateJavascript(script) { result ->
                    installed = result?.contains("ok") == true || result?.contains("already") == true
                    AppLog.d(TAG, "observer install=$result")
                }
            }.onFailure {
                installed = false
                AppLog.e(TAG, "observer inject failed", it)
            }
        }
    }

    fun scan(webView: WebView) {
        webView.post {
            webView.evaluateJavascript("window.__ZCodeScan && window.__ZCodeScan('manual');") {}
        }
    }

    /**
     * Re-validate and click an approval button. Returns a JS result string.
     * Never blindly clicks; requires matching dialog + labels.
     */
    fun clickApproval(
        webView: WebView,
        approvalId: String,
        allow: Boolean,
        expectedCommand: String,
        onResult: (String) -> Unit,
    ) {
        val labelGroup = if (allow) "allow" else "reject"
        val expected = org.json.JSONObject.quote(expectedCommand.take(120))
        val script = """
            (function(){
              try {
                if (!window.__ZCodeScan) return 'missing-observer';
                var snap = null;
                var last = null;
                try { last = JSON.parse(arguments && 0); } catch(e) {}
                var dialogs = document.querySelectorAll('[role="dialog"],[role="alertdialog"],[aria-modal="true"]');
                if (!dialogs.length) return 'mismatch-no-dialog';
                var cmd = $expected;
                var body = (dialogs[0].innerText || '');
                if (cmd && cmd.length > 4 && body.indexOf(cmd.slice(0, Math.min(24, cmd.length))) === -1) {
                  return 'mismatch-command';
                }
                var buttons = Array.prototype.slice.call(document.querySelectorAll('button,[role="button"]'));
                var re = '$labelGroup' === 'allow'
                  ? /^(allow|approve|confirm|always allow|允许|始终允许|确认)$/i
                  : /^(reject|deny|refuse|拒绝|不允许)$/i;
                var match = buttons.filter(function(b){
                  var t = ((b.getAttribute('aria-label')||'') + ' ' + (b.innerText||'')).trim();
                  return re.test(t.split('\n')[0].trim());
                });
                if (match.length !== 1) return 'mismatch-buttons:' + match.length;
                match[0].click();
                return 'clicked';
              } catch (e) { return 'error'; }
            })();
        """.trimIndent()
        webView.post {
            webView.evaluateJavascript(script) { raw ->
                onResult(raw?.trim('"') ?: "error")
            }
        }
    }

    companion object {
        private const val TAG = "ZCodeDomObserver"
    }
}
