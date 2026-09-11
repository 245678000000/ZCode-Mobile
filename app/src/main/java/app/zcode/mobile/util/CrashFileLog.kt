package app.zcode.mobile.util

import android.app.Application
import android.util.Log
import java.io.File
import java.util.Date

/**
 * Last-chance crash breadcrumb for OEM 闪退 (HyperOS/MIUI).
 * Writes a sanitized stack to app-private storage so the next launch can be diagnosed.
 */
object CrashFileLog {
    const val FILE_NAME = "last-crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(app: Application, thread: Thread, error: Throwable) {
        val body = buildString {
            appendLine(Date().toString())
            appendLine("thread=${thread.name}")
            appendLine(AppLog.sanitize(Log.getStackTraceString(error)))
        }
        File(app.filesDir, FILE_NAME).writeText(body)
    }
}
