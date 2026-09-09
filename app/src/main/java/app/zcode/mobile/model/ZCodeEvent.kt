package app.zcode.mobile.model

sealed interface ZCodeEvent {
    val timestamp: Long
    val type: String
    fun dedupeKey(): String
}

data class SessionDetected(
    val sessionId: String,
    val title: String,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "SessionDetected"
    override fun dedupeKey(): String = "$type:$sessionId:$title"
}

data class TaskCreated(
    val taskId: String,
    val sessionId: String?,
    val title: String,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "TaskCreated"
    override fun dedupeKey(): String = "$type:$taskId"
}

data class TaskUpdated(
    val taskId: String,
    val title: String,
    val status: TaskStatus,
    val currentStep: String?,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "TaskUpdated"
    override fun dedupeKey(): String = "$type:$taskId:${status.name}:${currentStep.orEmpty()}:${title}"
}

data class TaskRunning(
    val taskId: String,
    val title: String,
    val summary: String?,
    val progress: Float?,
    val currentStep: String?,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "TaskRunning"
    override fun dedupeKey(): String = "$type:$taskId:${currentStep.orEmpty()}"
}

data class TaskWaiting(
    val taskId: String,
    val title: String,
    val summary: String?,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "TaskWaiting"
    override fun dedupeKey(): String = "$type:$taskId"
}

data class TaskCompleted(
    val taskId: String,
    val title: String,
    val summary: String?,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "TaskCompleted"
    override fun dedupeKey(): String = "$type:$taskId"
}

data class TaskFailed(
    val taskId: String,
    val title: String,
    val summary: String?,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "TaskFailed"
    override fun dedupeKey(): String = "$type:$taskId"
}

data class ApprovalRequired(
    val approvalId: String,
    val title: String,
    val description: String,
    val command: String,
    val riskLevel: RiskLevel,
    val taskId: String? = null,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ApprovalRequired"
    override fun dedupeKey(): String = "$type:$approvalId:$command"
}

data class ApprovalResolved(
    val approvalId: String,
    val accepted: Boolean?,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ApprovalResolved"
    override fun dedupeKey(): String = "$type:$approvalId:${accepted ?: "gone"}"
}

data class ApprovalExpired(
    val approvalId: String,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ApprovalExpired"
    override fun dedupeKey(): String = "$type:$approvalId"
}

data class ApprovalMismatch(
    val approvalId: String,
    val reason: String,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ApprovalMismatch"
    override fun dedupeKey(): String = "$type:$approvalId:$reason"
}

data class ArtifactCreated(
    val artifactId: String,
    val name: String,
    val kind: ArtifactKind,
    val url: String?,
    val taskId: String? = null,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ArtifactCreated"
    override fun dedupeKey(): String = "$type:$artifactId:$name"
}

data class MessageReceived(
    val messageId: String,
    val taskId: String?,
    val text: String,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "MessageReceived"
    override fun dedupeKey(): String = "$type:$messageId"
}

data class ConnectionLost(
    val reason: String? = null,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ConnectionLost"
    override fun dedupeKey(): String = type
}

data class ConnectionRestored(
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "ConnectionRestored"
    override fun dedupeKey(): String = type
}

data class ObserverUnknown(
    val detail: String? = null,
    override val timestamp: Long = now(),
) : ZCodeEvent {
    override val type: String = "Unknown"
    override fun dedupeKey(): String = "$type:${detail.orEmpty()}"
}

internal fun now(): Long = System.currentTimeMillis()
