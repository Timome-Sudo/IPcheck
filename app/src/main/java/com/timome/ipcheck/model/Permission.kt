package com.timome.ipcheck.model

import android.Manifest

enum class Permission(val permissionString: String, val displayName: String, val isRequired: Boolean) {
    INTERNET(Manifest.permission.INTERNET, "网络访问", true),
    ACCESS_WIFI_STATE(Manifest.permission.ACCESS_WIFI_STATE, "访问WiFi状态", true),
    ACCESS_NETWORK_STATE(Manifest.permission.ACCESS_NETWORK_STATE, "访问网络状态", true),
    ACCESS_FINE_LOCATION(Manifest.permission.ACCESS_FINE_LOCATION, "精确位置", true),
    IGNORE_BATTERY_OPTIMIZATIONS("android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", "忽略电池优化", false)
}