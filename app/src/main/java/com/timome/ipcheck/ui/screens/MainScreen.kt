package com.timome.ipcheck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timome.ipcheck.model.Device
import com.timome.ipcheck.ui.theme.OfflineRed
import com.timome.ipcheck.ui.theme.OnlineGreen
import com.timome.ipcheck.ui.theme.ScanningBlue
import com.timome.ipcheck.viewmodel.ScanState as ViewModelScanState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    devices: List<Device>,
    scanState: ViewModelScanState,
    elapsedTime: Long,
    scanProgress: Int,
    scanProgressMax: Int,
    classifiedCount: Int,
    nameFetchCount: Int,
    onStartScan: () -> Unit,
    onDeviceOnlineStatusChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("设备列表") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScanSection(
                scanState = scanState,
                elapsedTime = elapsedTime,
                scanProgress = scanProgress,
                scanProgressMax = scanProgressMax,
                classifiedCount = classifiedCount,
                nameFetchCount = nameFetchCount,
                onStartScan = onStartScan
            )

            if (devices.isNotEmpty()) {
                Divider()
                DeviceList(
                    devices = devices,
                    onDeviceOnlineStatusChange = onDeviceOnlineStatusChange
                )
            } else {
                EmptyState(scanState = scanState)
            }
        }
    }
}

@Composable
fun ScanSection(
    scanState: ViewModelScanState,
    elapsedTime: Long,
    scanProgress: Int,
    scanProgressMax: Int,
    classifiedCount: Int,
    nameFetchCount: Int,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = scanState == ViewModelScanState.Idle,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Button(
                onClick = onStartScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "开始扫描",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始扫描")
            }
        }

        AnimatedVisibility(
            visible = scanState != ViewModelScanState.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (scanState == ViewModelScanState.Scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 8.dp,
                        color = ScanningBlue
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { scanProgressMax.takeIf { it > 0 }?.let { scanProgress.toFloat() / it } ?: 0f },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 8.dp,
                        color = ScanningBlue
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = getScanStatusText(scanState, scanProgress, scanProgressMax, classifiedCount, nameFetchCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatElapsedTime(elapsedTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DeviceList(
    devices: List<Device>,
    onDeviceOnlineStatusChange: (Int, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(devices, key = { it.ipv4Address ?: it.ipv6Address ?: it.hashCode() }) { device ->
            DeviceCard(device = device)
        }
    }
}

@Composable
fun DeviceCard(device: Device) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = device.type.iconRes),
                contentDescription = device.type.displayName,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (device.isLocalDevice) "本机 ${device.name}" else device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = device.displayAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (device.isOnline) OnlineGreen.copy(alpha = 0.2f) else OfflineRed.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (device.isOnline) "在线" else "离线",
                    color = if (device.isOnline) OnlineGreen else OfflineRed,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(scanState: ViewModelScanState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (scanState) {
                    ViewModelScanState.Idle -> "点击开始扫描按钮开始扫描网络设备"
                    ViewModelScanState.Scanning -> "正在扫描网络..."
                    ViewModelScanState.Classifying -> "正在分类设备..."
                    ViewModelScanState.FetchingNames -> "正在获取设备名称..."
                    ViewModelScanState.Completed -> "点击开始扫描按钮开始扫描网络设备"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getScanStatusText(
    scanState: ViewModelScanState,
    scanProgress: Int,
    scanProgressMax: Int,
    classifiedCount: Int,
    nameFetchCount: Int
): String {
    return when (scanState) {
        ViewModelScanState.Idle -> ""
        ViewModelScanState.Scanning -> "正在扫描..."
        ViewModelScanState.Classifying -> "分类设备: $classifiedCount/${scanProgressMax / 2}"
        ViewModelScanState.FetchingNames -> "获取名称: $nameFetchCount/${scanProgressMax / 2}"
        ViewModelScanState.Completed -> "扫描完成"
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}