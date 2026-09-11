package app.zcode.mobile.remote

import app.zcode.mobile.model.PageSnapshot
import app.zcode.mobile.model.RiskLevel
import app.zcode.mobile.model.SnapshotApproval

data class ApprovalSignals(
    val hasDialogOrModal: Boolean = false,
    val hasAllowButton: Boolean = false,
    val hasRejectButton: Boolean = false,
    val waitingContext: Boolean = false,
    val hasCommandOrTool: Boolean = false,
) {
    val score: Int
        get() {
            var n = 0
            if (hasDialogOrModal) n++
            if (hasAllowButton && hasRejectButton) n++
            else if (hasAllowButton || hasRejectButton) n++
            if (waitingContext) n++
            if (hasCommandOrTool) n++
            return n
        }

    /**
     * A dialog, or an allow+reject button pair, is required. Without that gate a lone
     * "确认" button plus any code block on the page would count as an approval.
     */
    val isApproval: Boolean
        get() = (hasDialogOrModal || (hasAllowButton && hasRejectButton)) && score >= 2
}

object ApprovalDetector {
    private val ALLOW = listOf("allow", "approve", "confirm", "always allow", "允许", "始终允许", "确认", "继续")
    private val REJECT = listOf("reject", "deny", "refuse", "拒绝", "不允许")
    private val WAITING = listOf("waiting for", "waiting approval", "needs confirmation", "permission", "authorization", "等待确认", "需要确认", "需要授权", "等待授权")

    fun signals(approval: SnapshotApproval?): ApprovalSignals {
        if (approval == null) return ApprovalSignals()
        return ApprovalSignals(
            hasDialogOrModal = approval.hasDialog,
            hasAllowButton = approval.hasAllow,
            hasRejectButton = approval.hasReject,
            waitingContext = approval.waitingContext,
            hasCommandOrTool = approval.command.isNotBlank(),
        )
    }

    fun isApproval(approval: SnapshotApproval?): Boolean = signals(approval).isApproval

    fun isApprovalFromPageText(
        body: String,
        hasDialog: Boolean,
        buttons: List<String>,
    ): Boolean {
        val lowerButtons = buttons.map { it.lowercase().trim() }
        val hasAllow = lowerButtons.any { button -> ALLOW.any { button == it || button.contains(it) } }
        val hasReject = lowerButtons.any { button -> REJECT.any { button == it || button.contains(it) } }
        val waiting = WAITING.any { body.lowercase().contains(it) }
        val commandLike = Regex("(?i)(rm |sudo |git |npm |pnpm |pip |chmod |curl |wget |delete |删除)").containsMatchIn(body)
        return ApprovalSignals(
            hasDialogOrModal = hasDialog,
            hasAllowButton = hasAllow,
            hasRejectButton = hasReject,
            waitingContext = waiting,
            hasCommandOrTool = commandLike,
        ).isApproval
    }

    fun riskLevel(command: String, description: String = ""): RiskLevel {
        val t = "$command $description".lowercase()
        if (t.isBlank()) return RiskLevel.UNKNOWN
        val high = listOf("rm -", "rm -rf", "unlink", "drop table", "format ", "mkfs", "删除", "destroy", "force push")
        val medium = listOf("bash", "sh -c", "zsh", "npm ", "pnpm ", "yarn ", "git ", "chmod", "curl ", "wget ", "sudo", "执行", "terminal")
        val low = listOf("read ", "cat ", "open ", "ls ", "stat ", "读取", "preview")
        return when {
            high.any { t.contains(it) } -> RiskLevel.HIGH
            medium.any { t.contains(it) } -> RiskLevel.MEDIUM
            low.any { t.contains(it) } -> RiskLevel.LOW
            else -> RiskLevel.UNKNOWN
        }
    }

    fun snapshotFromPage(snapshot: PageSnapshot): SnapshotApproval? {
        val approval = snapshot.approval ?: return null
        return approval.takeIf { isApproval(it) }
    }
}
