package app.zcode.mobile.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteUrlTest {
    @Test
    fun acceptsHttpHttpsOnly() {
        assertTrue(RemoteUrl.isValid("https://example.com/remote/abc"))
        assertTrue(RemoteUrl.isValid("http://192.168.1.8:8080/remote"))
        assertTrue(RemoteUrl.isValid("http://100.101.102.103:8080/remote"))
        assertTrue(RemoteUrl.isValid("http://my-mac.local:8080/remote"))
        assertFalse(RemoteUrl.isValid("http://example.com/remote/abc"))
        assertTrue(RemoteUrl.isPublicHttp("http://example.com/remote/abc"))
        assertFalse(RemoteUrl.isValid("ftp://x"))
        assertFalse(RemoteUrl.isValid("javascript:alert(1)"))
        assertFalse(RemoteUrl.isValid("not a url"))
        assertFalse(RemoteUrl.isValid(""))
    }

    @Test
    fun redactsPath() {
        val redacted = RemoteUrl.redacted("https://host.example/remote/super-secret-token")
        assertTrue(redacted.contains("host.example"))
        assertTrue(!redacted.contains("super-secret-token"))
    }
}
