package com.timome.ipcheck.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.timome.ipcheck.model.Device
import com.timome.ipcheck.model.DeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xbill.DNS.Address
import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.ReverseMap
import org.xbill.DNS.Type
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class NetworkScanner(private val context: Context? = null) {

    suspend fun scanNetwork(): List<Device> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<Device>()

        try {
            val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcpInfo = wifiManager?.dhcpInfo

            if (dhcpInfo != null) {
                val subnet = getSubnet(dhcpInfo.ipAddress)
                devices.addAll(scanSubnet(subnet))
            }

            // 添加本机设备
            val localDevice = getLocalDevice()
            if (localDevice != null) {
                devices.add(0, localDevice)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        devices
    }

    private fun getSubnet(ipAddress: Int): String {
        val sb = StringBuilder()
        for (i in 0..2) {
            sb.append(ipAddress and 0xff)
            sb.append(".")
        }
        return sb.toString()
    }

    private suspend fun scanSubnet(subnet: String): List<Device> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<Device>()
        val maxTimeout = 5000 // 最大超时时间
        var consecutiveFailures = 0
        val maxConsecutiveFailures = 10 // 连续失败次数

        for (i in 1..254) {
            val ipAddress = "$subnet$i"
            try {
                val isReachable = isHostReachable(ipAddress, 1000)
                if (isReachable) {
                    val ipv6Address = tryGetIPv6Address(ipAddress)
                    devices.add(
                        Device(
                            ipv4Address = ipAddress,
                            ipv6Address = ipv6Address,
                            name = "未知设备",
                            type = DeviceType.UNKNOWN,
                            isOnline = true
                        )
                    )
                    consecutiveFailures = 0
                } else {
                    consecutiveFailures++
                }
            } catch (e: Exception) {
                consecutiveFailures++
            }

            // 如果连续多次失败，提前结束扫描
            if (consecutiveFailures >= maxConsecutiveFailures) {
                break
            }
        }

        devices
    }

    private suspend fun isHostReachable(ipAddress: String, timeout: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(ipAddress)
                address.isReachable(timeout)
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun tryGetIPv6Address(ipv4Address: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = InetAddress.getAllByName(InetAddress.getByName(ipv4Address).hostName)
                addresses.find { it is Inet6Address }?.hostAddress
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getLocalDevice(): Device? {
        try {
            val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcpInfo = wifiManager?.dhcpInfo
            val localIp = wifiManager?.connectionInfo?.ipAddress

            if (localIp != null) {
                val ipv4Address = intToIp(localIp)
                return Device(
                    ipv4Address = ipv4Address,
                    ipv6Address = null,
                    name = "本机设备",
                    type = DeviceType.UNKNOWN,
                    isOnline = true,
                    isLocalDevice = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun intToIp(ip: Int): String {
        return ((ip and 0xFF).toString() + "." +
                (ip shr 8 and 0xFF).toString() + "." +
                (ip shr 16 and 0xFF).toString() + "." +
                (ip shr 24 and 0xFF).toString())
    }

    suspend fun identifyDeviceType(ipv4Address: String?, ipv6Address: String?): DeviceType {
        return withContext(Dispatchers.IO) {
            val address = ipv4Address ?: ipv6Address ?: return@withContext DeviceType.UNKNOWN

            // 首先检查是否是路由器/光猫
            if (checkIfRouter(address)) {
                return@withContext DeviceType.ROUTER
            }

            // 检查设备类型，按照优先级顺序
            when {
                checkIfAndroidDevice(address) -> DeviceType.ANDROID
                checkIfWindowsDevice(address) -> DeviceType.WINDOWS
                checkIfMacDevice(address) -> DeviceType.MAC
                checkIfLinuxDevice(address) -> DeviceType.LINUX
                else -> DeviceType.UNKNOWN
            }
        }
    }

    private suspend fun checkIfRouter(address: String): Boolean {
        // 路由器通常是 .1 或 .254
        return address.endsWith(".1") || address.endsWith(".254") || checkCommonRouterPorts(address)
    }

    private suspend fun checkIfModem(address: String): Boolean {
        // 光猫通常也有路由器特征
        return checkCommonRouterPorts(address)
    }

    private suspend fun checkCommonRouterPorts(address: String): Boolean {
        return try {
            isPortOpen(address, 80) || isPortOpen(address, 8080) || isPortOpen(address, 443)
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun isPortOpen(address: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(address, port), 1000)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun checkIfAndroidDevice(address: String): Boolean {
        // Android 设备通常开放 ADB 端口或其他特征端口
        return try {
            // 检查常见 Android 端口
            val androidPorts = listOf(5555, 8000, 8080, 8888)
            for (port in androidPorts) {
                if (isPortOpen(address, port)) {
                    // 进一步检查是否真的是 Android 设备
                    if (checkDeviceFingerprint(address, "android")) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkIfWindowsDevice(address: String): Boolean {
        // Windows 设备通常开放 SMB、RDP 端口
        return try {
            val windowsPorts = listOf(139, 445, 3389)
            for (port in windowsPorts) {
                if (isPortOpen(address, port)) {
                    // 进一步检查是否真的是 Windows 设备
                    if (checkDeviceFingerprint(address, "windows")) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkIfMacDevice(address: String): Boolean {
        // Mac 设备通常开放 AFP、AirPlay 端口
        return try {
            val macPorts = listOf(548, 5000, 7000, 7001, 7100)
            for (port in macPorts) {
                if (isPortOpen(address, port)) {
                    // 进一步检查是否真的是 Mac 设备
                    if (checkDeviceFingerprint(address, "mac")) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkIfLinuxDevice(address: String): Boolean {
        // Linux 设备通常开放 SSH、FTP 端口
        return try {
            val linuxPorts = listOf(22, 21, 80, 443, 3000)
            for (port in linuxPorts) {
                if (isPortOpen(address, port)) {
                    // 进一步检查是否真的是 Linux 设备
                    if (checkDeviceFingerprint(address, "linux")) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkMacOui(address: String, expectedOui: String): Boolean {
        // 这里可以实现 MAC 地址 OUI 检查
        // 需要获取设备的 MAC 地址，需要 ARP 表
        return false
    }

    private suspend fun checkDeviceFingerprint(address: String, fingerprint: String): Boolean {
        // 通过 HTTP 请求尝试识别设备类型
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$address"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 1000
                connection.readTimeout = 1000
                connection.requestMethod = "GET"
                
                val response = try {
                    connection.inputStream.bufferedReader().readText()
                } catch (e: Exception) {
                    ""
                }
                
                response.lowercase().contains(fingerprint.lowercase())
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getDeviceName(ipv4Address: String?, ipv6Address: String?, timeout: Int): String {
        return withContext(Dispatchers.IO) {
            val address = ipv4Address ?: ipv6Address ?: return@withContext "未知设备"

            try {
                // 尝试1: 反向 DNS 查询
                val dnsName = getHostnameByReverseDNS(address)
                if (dnsName != null && dnsName.isNotEmpty() && dnsName != address) {
                    return@withContext dnsName
                }

                // 尝试2: NetBIOS 查询（用于 Windows 设备）
                val netbiosName = getNetbiosName(address, timeout)
                if (netbiosName != null && netbiosName.isNotEmpty() && netbiosName != address) {
                    return@withContext netbiosName
                }

                // 尝试3: 通过 MAC 地址 OUI 获取厂商信息
                val vendorInfo = getVendorByMac(address)
                if (vendorInfo != null && vendorInfo.isNotEmpty()) {
                    return@withContext vendorInfo
                }

                // 尝试4: 通过 HTTP 请求获取设备信息
                val httpInfo = getDeviceInfoFromHttp(address)
                if (httpInfo != null && httpInfo.isNotEmpty()) {
                    return@withContext httpInfo
                }

                "未知设备"
            } catch (e: Exception) {
                "未知设备"
            }
        }
    }

    private suspend fun getHostnameByReverseDNS(address: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inetAddress = InetAddress.getByName(address)
                val hostname = inetAddress.hostName
                if (hostname != address) {
                    hostname
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getNetbiosName(address: String, timeout: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inetAddress = InetAddress.getByName(address)
                val hostname = inetAddress.canonicalHostName
                if (hostname.isNotEmpty() && hostname != address) {
                    hostname
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getVendorByMac(address: String): String? {
        // 这里可以通过 ARP 表获取 MAC 地址，然后查询 OUI 数据库
        // 简化实现，返回 null
        return null
    }

    private suspend fun getDeviceInfoFromHttp(address: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "http://$address"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 2000
                connection.readTimeout = 2000
                connection.requestMethod = "GET"
                
                // 尝试获取服务器头信息
                val server = connection.getHeaderField("Server")
                if (server != null && server.isNotEmpty()) {
                    return@withContext server
                }

                // 尝试读取页面内容
                val response = try {
                    connection.inputStream.bufferedReader().readText()
                } catch (e: Exception) {
                    ""
                }
                
                // 查找常见的设备标识
                val patterns = listOf(
                    "TP-LINK", "Huawei", "Xiaomi", "Mi", "ASUS", "D-Link",
                    "Netgear", "Linksys", "Cisco", "ZTE", "H3C",
                    "Windows", "Mac OS", "Linux", "Android"
                )
                
                for (pattern in patterns) {
                    if (response.contains(pattern, ignoreCase = true)) {
                        return@withContext pattern
                    }
                }
                
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun checkOnlineStatus(device: Device): Boolean {
        return withContext(Dispatchers.IO) {
            val address = device.ipv4Address ?: device.ipv6Address ?: return@withContext false
            try {
                val inetAddress = InetAddress.getByName(address)
                inetAddress.isReachable(2000)
            } catch (e: Exception) {
                false
            }
        }
    }
}