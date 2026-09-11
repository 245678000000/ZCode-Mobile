package app.zcode.mobile.remote

import app.zcode.mobile.model.TaskStatus

object TaskStatusMapper {
    fun fromText(raw: String?): TaskStatus {
        val t = raw?.lowercase()?.trim().orEmpty()
        if (t.isEmpty()) return TaskStatus.UNKNOWN
        return when {
            containsAny(t, "waiting for confirm", "waiting approval", "needs confirmation", "待确认", "等待确认", "需要确认", "等待授权") ->
                TaskStatus.WAITING_APPROVAL
            containsAny(t, "running", "in progress", "working", "executing", "运行中", "正在执行", "执行中") ->
                TaskStatus.RUNNING
            containsAny(t, "completed", "complete", "done", "finished", "已完成", "完成") ->
                TaskStatus.COMPLETED
            containsAny(t, "failed", "failure", "error", "失败") ->
                TaskStatus.FAILED
            containsAny(t, "cancelled", "canceled", "已取消", "取消") ->
                TaskStatus.CANCELLED
            containsAny(t, "queued", "pending", "排队") ->
                TaskStatus.QUEUED
            else -> TaskStatus.UNKNOWN
        }
    }

    fun isTerminal(status: TaskStatus): Boolean =
        status == TaskStatus.COMPLETED || status == TaskStatus.FAILED || status == TaskStatus.CANCELLED

    private fun containsAny(haystack: String, vararg needles: String): Boolean =
        needles.any { haystack.contains(it) }
}
