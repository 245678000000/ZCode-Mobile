package app.zcode.mobile.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.zcode.mobile.util.AppLog
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Remote URL and device name are encrypted with AES-GCM.
 * The key lives in Android Keystore and never leaves the device.
 */
class SecureStorage(context: Context) {
    private val lock = ReentrantLock()
    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val cache: Payload = lock.withLock { readUnlocked() }

    var remoteUrl: String?
        get() = lock.withLock { cache.remoteUrl }
        set(value) {
            lock.withLock {
                cache.remoteUrl = value
                writeUnlocked()
            }
        }

    var deviceName: String
        get() = lock.withLock { cache.deviceName.ifBlank { "ZCode Desktop" } }
        set(value) {
            lock.withLock {
                cache.deviceName = value
                writeUnlocked()
            }
        }

    fun clearConnection() {
        lock.withLock {
            cache.remoteUrl = null
            cache.deviceName = "ZCode Desktop"
            writeUnlocked()
        }
    }

    private fun readUnlocked(): Payload {
        if (!file.exists()) return Payload()
        return try {
            val decoded = Base64.decode(file.readText(), Base64.NO_WRAP)
            if (decoded.size < IV_SIZE + 1) return Payload()
            val iv = decoded.copyOfRange(0, IV_SIZE)
            val cipherBytes = decoded.copyOfRange(IV_SIZE, decoded.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            val json = JSONObject(String(cipher.doFinal(cipherBytes), Charsets.UTF_8))
            Payload(
                remoteUrl = json.optString(KEY_REMOTE_URL).ifBlank { null },
                deviceName = json.optString(KEY_DEVICE_NAME, "ZCode Desktop"),
            )
        } catch (t: Throwable) {
            AppLog.e(TAG, "failed to read secure payload", t)
            Payload()
        }
    }

    private fun writeUnlocked() {
        try {
            val json = JSONObject()
                .put(KEY_REMOTE_URL, cache.remoteUrl ?: "")
                .put(KEY_DEVICE_NAME, cache.deviceName)
                .toString()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
            val packed = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, packed, 0, iv.size)
            System.arraycopy(encrypted, 0, packed, iv.size, encrypted.size)
            file.writeText(Base64.encodeToString(packed, Base64.NO_WRAP))
        } catch (t: Throwable) {
            AppLog.e(TAG, "failed to write secure payload", t)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private class Payload(
        var remoteUrl: String? = null,
        var deviceName: String = "ZCode Desktop",
    )

    companion object {
        private const val TAG = "SecureStorage"
        private const val FILE_NAME = "zcode_secure.enc"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "zcode_mobile_aes"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val GCM_TAG_BITS = 128
        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_DEVICE_NAME = "device_name"
    }
}
