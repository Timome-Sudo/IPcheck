package com.timome.ipcheck.model

import androidx.annotation.DrawableRes
import com.timome.ipcheck.R

enum class DeviceType(val displayName: String, @DrawableRes val iconRes: Int) {
    ANDROID("安卓设备", R.drawable.round_ad_units_black_48dp),
    MAC("Mac设备", R.drawable.round_computer_black_48dp),
    WINDOWS("Windows设备", R.drawable.round_computer_black_48dp),
    LINUX("Linux设备", R.drawable.round_local_laundry_service_black_48dp),
    ROUTER("路由器", R.drawable.round_signal_wifi_4_bar_black_48dp),
    MODEM("光猫", R.drawable.round_signal_wifi_4_bar_black_48dp),
    UNKNOWN("未知设备", R.drawable.round_device_unknown_black_48dp)
}