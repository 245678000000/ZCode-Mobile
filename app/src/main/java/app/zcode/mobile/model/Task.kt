package app.zcode.mobile.model

data class Task(
    val id: String,
    val title: String,
    val summary: String? = null,
    val status: TaskStatus = TaskStatus.Running,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class TaskStatus {
    Queued,
    Running,
    Completed,
    Failed,
}
