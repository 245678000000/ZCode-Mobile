package app.zcode.mobile.notification

import app.zcode.mobile.data.AppSettings
import app.zcode.mobile.model.ApprovalRequired
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.model.TaskFailed
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.model.ZCodeEvent

class EventNotifier(
    private val manager: TaskNotificationManager,
    private val settings: () -> AppSettings,
) {
    private val completed = mutableSetOf<String>()
    private val failed = mutableSetOf<String>()
    private val approvals = mutableSetOf<String>()

    fun onEvent(event: ZCodeEvent) {
        val s = settings()
        when (event) {
            is TaskCompleted -> {
                if (!s.taskNotifications) return
                if (!completed.add(event.taskId)) return
                manager.notifyTaskCompleted(
                    Task(
                        id = event.taskId,
                        title = event.title,
                        description = event.summary,
                        status = TaskStatus.COMPLETED,
                    ),
                    openTaskId = event.taskId,
                )
            }
            is TaskFailed -> {
                if (!s.taskNotifications) return
                if (!failed.add(event.taskId)) return
                manager.notifyTaskFailed(
                    Task(
                        id = event.taskId,
                        title = event.title,
                        description = event.summary,
                        status = TaskStatus.FAILED,
                    ),
                    openTaskId = event.taskId,
                )
            }
            is ApprovalRequired -> {
                if (!s.approvalNotifications) return
                if (!approvals.add(event.approvalId)) return
                manager.notifyApproval(
                    app.zcode.mobile.model.ApprovalRequest(
                        id = event.approvalId,
                        title = "ZCode · 需要确认",
                        description = event.description.ifBlank { event.title },
                        command = event.command,
                        riskLevel = event.riskLevel,
                        taskId = event.taskId,
                    ),
                )
            }
            else -> Unit
        }
    }
}
