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

    fun notifyTaskCompleted(task: Task) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REMOTE, true)
        }
        val pending = PendingIntent.getActivity(
            appContext,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zcode)
            .setContentTitle("任务已完成")
            .setContentText(task.summary ?: task.title)
            .setSubText("ZCode")
            .setStyle(NotificationCompat.BigTextStyle().bigText(task.summary ?: task.title))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(task.id.hashCode(), notification) }
    }

    fun notifyApproval(request: ApprovalRequest) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_APPROVAL, true)
            putExtra(EXTRA_APPROVAL_ID, request.id)
        }
        val pending = PendingIntent.getActivity(
            appContext,
            request.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zcode)
            .setContentTitle(request.title)
            .setContentText(request.description)
            .setSubText("ZCode")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { manager.notify(request.id.hashCode(), notification) }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
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
        const val EXTRA_OPEN_REMOTE = "open_remote"
        const val EXTRA_OPEN_APPROVAL = "open_approval"
        const val EXTRA_APPROVAL_ID = "approval_id"
    }
}
