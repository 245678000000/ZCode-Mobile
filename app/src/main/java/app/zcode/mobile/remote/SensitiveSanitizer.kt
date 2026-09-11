package app.zcode.mobile.remote

object SensitiveSanitizer {
    private val PATTERNS = listOf(
        Regex("(?i)(cookie|set-cookie|authorization|token|session|secret|password)[=:]\\s*[^\\s&]+"),
        Regex("(?i)bearer\\s+[a-z0-9._\\-]+"),
        Regex("(?i)(access_token|refresh_token|id_token)=[^&\\s]+"),
    )

    fun text(value: String?, max: Int = 500): String {
        if (value.isNullOrBlank()) return ""
        var out = value.take(max)
        PATTERNS.forEach { regex ->
            out = regex.replace(out) { match ->
                val label = match.groupValues.getOrNull(1)?.ifBlank { null } ?: "secret"
                "$label:•••"
            }
        }
        return out
    }

    fun url(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return try {
            val q = value.indexOf('?')
            val h = value.indexOf('#')
            val cut = when {
                q >= 0 && h >= 0 -> minOf(q, h)
                q >= 0 -> q
                h >= 0 -> h
                else -> value.length
            }
            text(value.substring(0, cut), 300)
        } catch (_: Exception) {
            "•••"
        }
    }
}
