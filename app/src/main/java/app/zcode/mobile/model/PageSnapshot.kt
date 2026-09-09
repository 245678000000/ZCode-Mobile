package app.zcode.mobile.model

data class SnapshotTask(
    val id: String,
    val title: String,
    val statusText: String? = null,
    val step: String? = null,
    val agentName: String? = null,
)

data class SnapshotApproval(
    val id: String,
    val title: String,
    val description: String,
    val command: String,
    val hasDialog: Boolean,
    val hasAllow: Boolean,
    val hasReject: Boolean,
    val waitingContext: Boolean,
    val allowLabel: String? = null,
    val rejectLabel: String? = null,
)

data class SnapshotArtifact(
    val id: String,
    val name: String,
    val href: String? = null,
    val mime: String? = null,
)

data class SnapshotMessage(
    val id: String,
    val text: String,
)

data class PageSnapshot(
    val url: String = "",
    val title: String = "",
    val connectionHint: String? = null,
    val sessionId: String? = null,
    val sessionTitle: String? = null,
    val tasks: List<SnapshotTask> = emptyList(),
    val approval: SnapshotApproval? = null,
    val artifacts: List<SnapshotArtifact> = emptyList(),
    val messages: List<SnapshotMessage> = emptyList(),
    val observerActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
