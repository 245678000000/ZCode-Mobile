package app.zcode.mobile.util

import java.net.URI

data class RemoteUrlParse(
    val raw: String,
    val scheme: String,
    val host: String,
    val isHttps: Boolean,
)

object RemoteUrl {
    fun normalize(input: String): String = input.trim()

    fun isValid(input: String): Boolean = parse(input) != null

    /**
     * Accepts https to any host, and plain http only to LAN / loopback / CGNAT (Tailscale)
     * hosts. A Remote URL carries the session secret in its path; sending that in clear
     * text across the public internet is never acceptable.
     */
    fun parse(input: String): RemoteUrlParse? {
        val raw = normalize(input)
        if (raw.isEmpty()) return null
        return try {
            val uri = URI(raw)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null
            val host = uri.host?.lowercase() ?: return null
            if (host.isBlank()) return null
            if (scheme == "http" && !isPrivateHost(host)) return null
            RemoteUrlParse(raw = raw, scheme = scheme, host = host, isHttps = scheme == "https")
        } catch (_: Exception) {
            null
        }
    }

    /** True when the input is a URL but was rejected only because it is http to a public host. */
    fun isPublicHttp(input: String): Boolean {
        val raw = normalize(input)
        return try {
            val uri = URI(raw)
            val host = uri.host?.lowercase() ?: return false
            uri.scheme?.lowercase() == "http" && !isPrivateHost(host)
        } catch (_: Exception) {
            false
        }
    }

    fun displayHost(input: String): String {
        val parsed = parse(input) ?: return "ZCode Desktop"
        return when {
            isPrivateHost(parsed.host) -> "ZCode Desktop"
            parsed.host.contains("zcode") -> "ZCode Desktop"
            else -> parsed.host
        }
    }

    fun redacted(input: String): String {
        val parsed = parse(input) ?: return "—"
        return try {
            val uri = URI(parsed.raw)
            val path = uri.path.orEmpty()
            val redactedPath = if (path.length <= 12) {
                path.ifBlank { "/" }
            } else {
                path.take(8) + "••••"
            }
            buildString {
                append(parsed.scheme)
                append("://")
                append(parsed.host)
                if (uri.port > 0 && uri.port != 80 && uri.port != 443) {
                    append(":")
                    append(uri.port)
                }
                append(redactedPath)
            }
        } catch (_: Exception) {
            "${parsed.scheme}://${parsed.host}/••••"
        }
    }

    fun origin(input: String): String? {
        val parsed = parse(input) ?: return null
        return try {
            val uri = URI(parsed.raw)
            buildString {
                append(parsed.scheme)
                append("://")
                append(parsed.host)
                if (uri.port > 0) {
                    append(":")
                    append(uri.port)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isSameOrigin(remoteUrl: String, other: String): Boolean {
        val a = origin(remoteUrl) ?: return false
        val b = origin(other) ?: return false
        return a.equals(b, ignoreCase = true)
    }

    fun looksLikeRemoteUrl(input: String): Boolean {
        val parsed = parse(input) ?: return false
        val path = runCatching { URI(parsed.raw).path.orEmpty().lowercase() }.getOrDefault("")
        return path.contains("remote") || parsed.host.contains("zcode") || parsed.host.contains("localhost") || isPrivateHost(parsed.host)
    }

    fun isPrivateHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return true
        if (host.endsWith(".local")) return true
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { it.toIntOrNull() != null }) {
            val a = parts[0].toInt()
            val b = parts[1].toInt()
            return a == 10 ||
                (a == 192 && b == 168) ||
                (a == 172 && b in 16..31) ||
                (a == 100 && b in 64..127) || // CGNAT, used by Tailscale
                (a == 169 && b == 254) || // link-local
                a == 127
        }
        return false
    }
}
