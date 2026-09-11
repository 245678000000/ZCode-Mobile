package app.zcode.mobile.model

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN,
}

data class ApprovalRequest(
    val id: String,
    val title: String,
    val description: String,
    val command: String,
    val riskLevel: RiskLevel,
    val source: String = "dom",
    val timestamp: Long = System.currentTimeMillis(),
    val taskId: String? = null,
    val allowLabel: String? = null,
    val rejectLabel: String? = null,
) {
    companion object {
        fun demo(): ApprovalRequest = ApprovalRequest(
            id = "demo-approval-1",
            title = "ZCode · 需要授权",
            description = "删除 3 个旧文件",
            command = "rm -rf legacy/",
            riskLevel = RiskLevel.HIGH,
            source = "developer-demo",
        )
    }
}
