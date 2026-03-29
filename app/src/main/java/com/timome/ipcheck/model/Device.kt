package com.timome.ipcheck.model

data class Device(
    val ipv4Address: String? = null,
    val ipv6Address: String? = null,
    val name: String = "未知设备",
    val type: DeviceType = DeviceType.UNKNOWN,
    val isOnline: Boolean = true,
    val isLocalDevice: Boolean = false
) {
    val displayAddress: String
        get() = ipv4Address ?: ipv6Address ?: "未知"
}