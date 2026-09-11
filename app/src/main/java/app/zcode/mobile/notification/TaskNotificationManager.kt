package app.zcode.mobile.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.zcode.mobile.MainActivity
import app.zcode.mobile.R
import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.Task

class TaskNotificationManager(context: Context) {
    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    init {
        ensureChannel()
    }

    fun notifyTaskCompleted(task: Task, openTaskId: String? = task.id) {
        notify(
            id = task.id.hashCode(),
            title = "任务已完成",
            text = task.title,
            high = false,
        ) {
            putExtra(EXTRA_OPEN_TASK_ID, openTaskId ?: task.id)
        }
    }

    fun notifyTaskFailed(task: Task, openTaskId: String? = task.id) {
        notify(
            id = ("fail-" + task.id).hashCode(),
            title = "任务执行失败",
            text = task.title,
            high = true,
        ) {
            putExtra(EXTRA_OPEN_TASK_ID, openTaskId ?: task.id)
        }
    }

    fun notifyApproval(request: ApprovalRequest) {
        notify(
            id = request.id.hashCode(),
            title = "ZCode · 需要确认",
            text = request.description.ifBlank { request.title },
            high = true,
        ) {
            putExtra(EXTRA_OPEN_APPROVAL, true)
            putExtra(EXTRA_APPROVAL_ID, request.id)
        }
    }

    private fun notify(
        id: Int,
        title: String,
        text: String,
        high: Boolean,
        extras: Intent.() -> Unit,
    ) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            extras()
        }
        val pending = PendingIntent.getActivity(
            appContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zcode)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText("ZCode")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(if (high) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the call.
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "ZCode task and approval updates"
            }
            val system = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            system.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "zcode_tasks"
        const val CHANNEL_NAME = "ZCode Tasks"
        const val EXTRA_OPEN_APPROVAL = "open_approval"
        const val EXTRA_APPROVAL_ID = "approval_id"
        const val EXTRA_OPEN_TASK_ID = "open_task_id"
    }
}
