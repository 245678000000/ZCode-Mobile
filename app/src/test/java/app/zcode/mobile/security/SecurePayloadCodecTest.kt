package app.zcode.mobile.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecurePayloadCodecTest {
    @Test
    fun jsonRoundTrip() {
        val json = SecurePayloadCodec.encodeJson("https://h/remote/x", "ZCode Desktop")
        val (url, name) = SecurePayloadCodec.decodeJson(json)
        assertEquals("https://h/remote/x", url)
        assertEquals("ZCode Desktop", name)
    }

    @Test
    fun packUnpack() {
        val iv = ByteArray(12) { it.toByte() }
        val cipher = byteArrayOf(1, 2, 3, 4)
        val packed = SecurePayloadCodec.pack(iv, cipher)
        val unpacked = SecurePayloadCodec.unpack(packed)
        assertArrayEquals(iv, unpacked!!.first)
        assertArrayEquals(cipher, unpacked.second)
        assertNull(SecurePayloadCodec.unpack(ByteArray(4)))
    }
}
