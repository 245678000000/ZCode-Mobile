package app.zcode.mobile.remote

import app.zcode.mobile.model.PageSnapshot
import app.zcode.mobile.model.SnapshotApproval
import app.zcode.mobile.model.SnapshotArtifact
import app.zcode.mobile.model.SnapshotTask

/**
 * Regex-based HTML fixture parser used only by unit tests. Live extraction happens in
 * the injected MutationObserver (assets/zcode-observer.js); this is a test-side stand-in.
 */
object HtmlSnapshotExtractor {
    private val TAG_RE = Regex("<[^>]+>")
    private val DIALOG_RE = Regex("(?is)<[^>]*(role\\s*=\\s*['\"]?(dialog|alertdialog)|aria-modal\\s*=\\s*['\"]true)[^>]*>.*?</[^>]+>")
    private val BUTTON_RE = Regex("(?is)<button[^>]*>(.*?)</button>")
    private val HREF_RE = Regex("(?is)<a[^>]*href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE)
    private val DATA_STATUS_RE = Regex("(?is)data-status\\s*=\\s*[\"']([^\"']+)[\"']")
    private val TESTID_TASK_RE = Regex("(?is)data-testid\\s*=\\s*[\"']([^\"']*task[^\"']*)[\"'][^>]*>(.*?)</", RegexOption.IGNORE_CASE)

    fun extract(html: String, url: String = "https://remote.local/session"): PageSnapshot {
        val text = TAG_RE.replace(html, " ").replace(Regex("\\s+"), " ").trim()
        val tasks = extractTasks(html, text)
        val approval = extractApproval(html, text)
        val artifacts = extractArtifacts(html)
        return PageSnapshot(
            url = SensitiveSanitizer.url(url),
            title = tasks.firstOrNull()?.title.orEmpty(),
            connectionHint = if (text.contains("session expired", true) || text.contains("会话过期")) "expired" else "ok",
            sessionId = "session",
            sessionTitle = tasks.firstOrNull()?.title,
            tasks = tasks,
            approval = approval,
            artifacts = artifacts,
            observerActive = true,
        )
    }

    private fun extractTasks(html: String, text: String): List<SnapshotTask> {
        val fromAttr = DATA_STATUS_RE.findAll(html).mapIndexed { index, match ->
            val status = match.groupValues[1]
            val around = html.substring(maxOf(0, match.range.first - 200), minOf(html.length, match.range.last + 200))
            val title = TAG_RE.replace(around, " ").replace(Regex("\\s+"), " ").trim().take(80)
            SnapshotTask(id = "t$index", title = title.ifBlank { "Task $index" }, statusText = status)
        }.toList()
        if (fromAttr.isNotEmpty()) return fromAttr
        val status = TaskStatusMapper.fromText(text)
        val heading = Regex("<h[12][^>]*>(.*?)</h[12]>", RegexOption.IGNORE_CASE).find(html)
            ?.groupValues?.getOrNull(1)?.let { TAG_RE.replace(it, "").trim() }
        return if (heading != null) {
            listOf(SnapshotTask(id = heading.hashCode().toString(), title = heading, statusText = status.name))
        } else emptyList()
    }

    private fun extractApproval(html: String, text: String): SnapshotApproval? {
        val hasDialog = html.contains("role=\"dialog\"", true) ||
            html.contains("role='dialog'", true) ||
            html.contains("alertdialog", true) ||
            html.contains("aria-modal=\"true\"", true)
        val buttons = BUTTON_RE.findAll(html).map { TAG_RE.replace(it.groupValues[1], "").trim() }.toList()
        if (!ApprovalDetector.isApprovalFromPageText(text, hasDialog, buttons)) return null
        val command = Regex("<(?:pre|code)[^>]*>(.*?)</(?:pre|code)>", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
            ?.let { TAG_RE.replace(it, "").trim() }
            .orEmpty()
        return SnapshotApproval(
            id = "html-approval",
            title = "需要确认",
            description = text.take(160),
            command = command,
            hasDialog = hasDialog,
            hasAllow = buttons.any { it.contains("允许") || it.equals("Allow", true) || it.equals("Approve", true) },
            hasReject = buttons.any { it.contains("拒绝") || it.equals("Reject", true) || it.equals("Deny", true) },
            waitingContext = text.contains("确认") || text.contains("confirm", true),
            allowLabel = buttons.firstOrNull { it.contains("允许") || it.equals("Allow", true) },
            rejectLabel = buttons.firstOrNull { it.contains("拒绝") || it.equals("Reject", true) },
        )
    }

    private fun extractArtifacts(html: String): List<SnapshotArtifact> {
        return HREF_RE.findAll(html).mapNotNull { match ->
            val href = match.groupValues[1]
            val name = TAG_RE.replace(match.groupValues[2], "").trim().ifBlank { href.substringAfterLast('/') }
            if (!ArtifactTypeDetector.looksLikeArtifactName(name) && !ArtifactTypeDetector.looksLikeArtifactName(href)) {
                null
            } else {
                SnapshotArtifact(id = name, name = name, href = href)
            }
        }.toList()
    }
}
