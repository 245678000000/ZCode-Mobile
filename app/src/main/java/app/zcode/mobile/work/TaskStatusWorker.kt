package app.zcode.mobile.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.zcode.mobile.util.AppLog

/**
 * Placeholder worker for future desktop task-status polling.
 * First version does not talk to any private ZCode protocol.
 */
class TaskStatusWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        AppLog.d(TAG, "task status worker tick")
        return Result.success()
    }

    companion object {
        const val TAG = "TaskStatusWorker"
        const val UNIQUE_NAME = "zcode-task-status"
    }
}
