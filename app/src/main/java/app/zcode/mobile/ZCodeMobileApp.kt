package app.zcode.mobile

import android.app.Application
import app.zcode.mobile.notification.TaskNotificationManager
import app.zcode.mobile.work.TaskStatusWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ZCodeMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskNotificationManager(this)
        val work = PeriodicWorkRequestBuilder<TaskStatusWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TaskStatusWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work,
        )
    }
}
