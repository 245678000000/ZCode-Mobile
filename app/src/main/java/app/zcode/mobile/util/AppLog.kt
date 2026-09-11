package app.zcode.mobile.util

import android.util.Log
import app.zcode.mobile.BuildConfig

object AppLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, sanitize(message))
        }
    }

    /** Errors are logged in every build type; [sanitize] strips auth material first. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, sanitize(message), throwable)
    }

    fun sanitize(value: String): String {
        var out = value
        out = Regex("(?i)(token|session|authorization|cookie|set-cookie)=([^&\\s]+)").replace(out) {
            "${it.groupValues[1]}=•••"
        }
        out = Regex("(?i)(bearer\\s+)[a-z0-9._\\-]+").replace(out) { "${it.groupValues[1]}•••" }
        return out
    }
}
