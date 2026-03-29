package com.timome.ipcheck.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.timome.ipcheck.model.Device
import com.timome.ipcheck.model.DeviceType
import com.timome.ipcheck.network.NetworkScanner
import com.timome.ipcheck.viewmodel.ScanState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _scanProgressMax = MutableStateFlow(0)
    val scanProgressMax: StateFlow<Int> = _scanProgressMax.asStateFlow()

    private val _classifiedCount = MutableStateFlow(0)
    val classifiedCount: StateFlow<Int> = _classifiedCount.asStateFlow()

    private val _nameFetchCount = MutableStateFlow(0)
    val nameFetchCount: StateFlow<Int> = _nameFetchCount.asStateFlow()

    private val networkScanner = NetworkScanner(application.applicationContext)
    private var scanJob: Job? = null
    private var timerJob: Job? = null

    fun startScan() {
        if (_scanState.value != ScanState.Idle) return

        _scanState.value = ScanState.Scanning
        _devices.value = emptyList()
        _elapsedTime.value = 0L
        _scanProgress.value = 0
        _scanProgressMax.value = 0
        _classifiedCount.value = 0
        _nameFetchCount.value = 0

        startTimer()

        scanJob = viewModelScope.launch {
            try {
                val scannedDevices = networkScanner.scanNetwork()
                _devices.value = scannedDevices
                _scanState.value = ScanState.Classifying
                
                // 设置总进度最大值为设备数量的2倍（分类+获取名称）
                _scanProgressMax.value = scannedDevices.size * 2

                classifyDevices(scannedDevices)

                _scanState.value = ScanState.FetchingNames
                // 不重置进度，继续累加

                fetchDeviceNames(scannedDevices)

                _scanState.value = ScanState.Completed
            } catch (e: Exception) {
                _scanState.value = ScanState.Idle
            } finally {
                stopTimer()
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        stopTimer()
        _scanState.value = ScanState.Idle
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedTime.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private suspend fun classifyDevices(devices: List<Device>) {
        val updatedDevices = devices.mapIndexed { index, device ->
            val type = networkScanner.identifyDeviceType(device.ipv4Address, device.ipv6Address)
            _classifiedCount.value = index + 1
            _scanProgress.value = index + 1
            device.copy(type = type)
        }
        _devices.value = updatedDevices
    }

    private suspend fun fetchDeviceNames(devices: List<Device>) {
        val updatedDevices = devices.mapIndexed { index, device ->
            val name = try {
                networkScanner.getDeviceName(device.ipv4Address, device.ipv6Address, 3000)
            } catch (e: Exception) {
                "未知设备"
            }
            _nameFetchCount.value = index + 1
            // 进度继续累加：已分类数量 + 当前索引 + 1
            _scanProgress.value = _classifiedCount.value + index + 1
            device.copy(name = name)
        }
        _devices.value = updatedDevices
    }

    fun updateDeviceOnlineStatus(index: Int, isOnline: Boolean) {
        val currentList = _devices.value.toMutableList()
        if (index < currentList.size) {
            currentList[index] = currentList[index].copy(isOnline = isOnline)
            _devices.value = currentList
        }
    }

    fun getFormattedElapsedTime(): String {
        val hours = TimeUnit.SECONDS.toHours(_elapsedTime.value)
        val minutes = TimeUnit.SECONDS.toMinutes(_elapsedTime.value) % 60
        val seconds = _elapsedTime.value % 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        stopTimer()
    }
}