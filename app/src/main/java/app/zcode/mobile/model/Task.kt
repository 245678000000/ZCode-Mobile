package app.zcode.mobile.model

enum class TaskStatus {
    UNKNOWN,
    QUEUED,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class Task(
    val id: String,
    val sessionId: String? = null,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.UNKNOWN,
    val progress: Float? = null,
    val currentStep: String? = null,
    val agentName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val artifacts: List<Artifact> = emptyList(),
    val approvalCount: Int = 0,
) {
    val summary: String?
        get() = currentStep ?: description
}
