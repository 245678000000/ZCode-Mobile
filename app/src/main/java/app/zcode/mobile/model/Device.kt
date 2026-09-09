package app.zcode.mobile.model

data class Device(
    val name: String = "ZCode Desktop",
    val remoteUrl: String,
    val connected: Boolean = false,
)
