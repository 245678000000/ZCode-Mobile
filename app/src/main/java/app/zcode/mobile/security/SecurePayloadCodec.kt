package app.zcode.mobile.security

import org.json.JSONObject

object SecurePayloadCodec {
    fun encodeJson(remoteUrl: String?, deviceName: String): String {
        return JSONObject()
            .put("remote_url", remoteUrl ?: "")
            .put("device_name", deviceName)
            .toString()
    }

    fun decodeJson(json: String): Pair<String?, String> {
        val obj = JSONObject(json)
        val url = obj.optString("remote_url").ifBlank { null }
        val name = obj.optString("device_name", "ZCode Desktop")
        return url to name
    }

    fun pack(iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val packed = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, packed, 0, iv.size)
        System.arraycopy(ciphertext, 0, packed, iv.size, ciphertext.size)
        return packed
    }

    fun unpack(packed: ByteArray, ivSize: Int = 12): Pair<ByteArray, ByteArray>? {
        if (packed.size <= ivSize) return null
        val iv = packed.copyOfRange(0, ivSize)
        val cipher = packed.copyOfRange(ivSize, packed.size)
        return iv to cipher
    }
}
