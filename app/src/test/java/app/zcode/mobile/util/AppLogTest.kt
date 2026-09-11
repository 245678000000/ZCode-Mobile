package app.zcode.mobile.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLogTest {
    @Test
    fun sanitizeRedactsAuthMaterial() {
        val raw = "url=https://h/x token=abc123 cookie=sid=xyz"
        val out = AppLog.sanitize(raw)
        assertFalse(out.contains("abc123"))
        assertFalse(out.contains("sid=xyz"))
        assertTrue(out.contains("token=•••"))
        assertTrue(out.contains("cookie=•••"))
    }
}
