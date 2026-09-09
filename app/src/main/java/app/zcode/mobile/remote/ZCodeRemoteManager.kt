package app.zcode.mobile.remote

import app.zcode.mobile.model.Device
import app.zcode.mobile.security.SecureStorage
import app.zcode.mobile.util.RemoteUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ZCodeRemoteManager(
    private val secureStorage: SecureStorage,
    private val sessionManager: SessionManager,
) {
    private val _device = MutableStateFlow(loadDevice())
    val device: StateFlow<Device?> = _device.asStateFlow()

    val hasConnection: Boolean
        get() = !_device.value?.remoteUrl.isNullOrBlank()

    fun save(url: String, name: String = "ZCode Desktop") {
        val parsed = RemoteUrl.parse(url) ?: return
        val deviceName = name.ifBlank { RemoteUrl.displayHost(parsed.raw) }
        secureStorage.remoteUrl = parsed.raw
        secureStorage.deviceName = deviceName
        _device.value = Device(name = deviceName, remoteUrl = parsed.raw, connected = false)
    }

    fun markConnected(connected: Boolean) {
        val current = _device.value ?: return
        _device.value = current.copy(connected = connected)
    }

    fun reconnect() {
        _device.value = loadDevice()
    }

    fun disconnect(clearWebData: Boolean = false) {
        secureStorage.clearConnection()
        if (clearWebData) {
            sessionManager.clear()
        }
        _device.value = null
    }

    fun currentUrl(): String? = _device.value?.remoteUrl ?: secureStorage.remoteUrl

    private fun loadDevice(): Device? {
        val url = secureStorage.remoteUrl ?: return null
        if (!RemoteUrl.isValid(url)) return null
        return Device(
            name = secureStorage.deviceName.ifBlank { "ZCode Desktop" },
            remoteUrl = url,
            connected = false,
        )
    }
}
