package com.timome.ipcheck.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShowTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName ?: ""
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toString()
    }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf<String>("") }
    var updateAvailable by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String>("") }
    var downloadUrl by remember { mutableStateOf<String>("") }
    var fileSize by remember { mutableStateOf<Long>(0) }
    var fileName by remember { mutableStateOf<String>("") }

    // 下载相关状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float>(0f) }
    var downloadedBytes by remember { mutableStateOf<Long>(0) }
    var downloadSpeed by remember { mutableStateOf<String>("") }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var currentMirrorIndex by remember { mutableIntStateOf(0) }

    val githubUrl = "https://github.com/Timome-Sudo/IPcheck"

    // GitHub镜像站列表
    val mirrorSites = listOf(
        "https://gh-proxy.com",
        "https://mirror.ghproxy.com",
        "https://ghps.cc",
        "https://gh.api.99988866.xyz",
        "https://ghdl.feizhuqwq.com"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 应用图标
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "应用图标",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 应用名称
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "IPCheck",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // 应用版本
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "版本 $versionName ($versionCode)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 检查更新卡片
            UpdateCard(
                isCheckingUpdate = isCheckingUpdate,
                updateAvailable = updateAvailable,
                updateMessage = updateMessage,
                latestVersion = latestVersion,
                currentVersion = versionName,
                onCheckUpdate = {
                    isCheckingUpdate = true
                },
                onDownloadUpdate = {
                    showDownloadDialog = true
                }
            )

            // 处理检查更新逻辑
            LaunchedEffect(isCheckingUpdate) {
                if (isCheckingUpdate) {
                    checkForUpdate(
                        context = context,
                        currentVersion = versionName,
                        onSuccess = { version, message, url, size, name ->
                            latestVersion = version
                            updateAvailable = true
                            updateMessage = message
                            downloadUrl = url
                            fileSize = size
                            fileName = name
                            isCheckingUpdate = false
                        },
                        onError = { message ->
                            updateMessage = message
                            isCheckingUpdate = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GitHub仓库卡片
            InfoCard(
                icon = Icons.Default.Check,
                title = "GitHub仓库",
                description = githubUrl,
                onClick = { openUrl(context, githubUrl) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 条款卡片
            InfoCard(
                icon = Icons.Default.Info,
                title = "条款",
                description = "查看免责声明",
                onClick = onShowTerms
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 开发者卡片
            InfoCard(
                icon = Icons.Default.Person,
                title = "开发者",
                description = "timome",
                onClick = { openUrl(context, "https://github.com/Timome-Sudo") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 下载进度对话框
        if (showDownloadDialog && isDownloading) {
            DownloadProgressDialog(
                fileName = fileName,
                progress = downloadProgress,
                downloadedBytes = downloadedBytes,
                totalBytes = fileSize,
                speed = downloadSpeed,
                onDismiss = { }
            )
        }

        // 下载错误对话框
        if (showErrorDialog) {
            DownloadErrorDialog(
                error = downloadError ?: "未知错误",
                downloadUrl = downloadUrl,
                onRetry = {
                    // 重试：重置镜像站索引，使用原始URL
                    currentMirrorIndex = 0
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        startDownload(context, downloadUrl, fileName, fileSize, 
                            { progress, bytes, speed ->
                                downloadProgress = progress
                                downloadedBytes = bytes
                                downloadSpeed = speed
                            },
                            { error ->
                                downloadError = error
                                showErrorDialog = true
                                isDownloading = false
                            }
                        )
                    }
                    showErrorDialog = false
                    isDownloading = true
                },
                onUseMirror = {
                    // 使用镜像站：依次尝试所有镜像站
                    if (currentMirrorIndex < mirrorSites.size) {
                        val mirrorUrl = mirrorSites[currentMirrorIndex]
                        val proxiedUrl = "$mirrorUrl/$downloadUrl"
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            startDownload(context, proxiedUrl, fileName, fileSize,
                                { progress, bytes, speed ->
                                    downloadProgress = progress
                                    downloadedBytes = bytes
                                    downloadSpeed = speed
                                },
                                { error ->
                                    downloadError = error
                                    showErrorDialog = true
                                    isDownloading = false
                                }
                            )
                        }
                        currentMirrorIndex++
                        showErrorDialog = false
                        isDownloading = true
                    } else {
                        // 所有镜像站都尝试过了，显示错误信息
                        showErrorDialog = false
                        isDownloading = false
                    }
                },
                onOpenBrowser = {
                    openUrl(context, downloadUrl)
                    showErrorDialog = false
                },
                onCopyLink = {
                    copyToClipboard(context, downloadUrl)
                },
                onCopyError = {
                    copyToClipboard(context, downloadError ?: "未知错误")
                },
                onDismiss = {
                    showErrorDialog = false
                    isDownloading = false
                }
            )
        }

        // 下载完成对话框
        if (showDownloadDialog && !isDownloading && downloadProgress >= 1f && downloadError == null) {
            AlertDialog(
                onDismissRequest = { showDownloadDialog = false },
                title = { Text("下载完成") },
                text = { Text("APK文件已下载完成，请到下载文件夹查看。") },
                confirmButton = {
                    TextButton(onClick = { showDownloadDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }

    // 开始下载
    LaunchedEffect(showDownloadDialog) {
        if (showDownloadDialog && !isDownloading && downloadUrl.isNotEmpty()) {
            currentMirrorIndex = 0
            isDownloading = true
            downloadProgress = 0f
            downloadedBytes = 0L
            downloadSpeed = "等待中..."
            downloadError = null

            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                startDownload(context, downloadUrl, fileName, fileSize,
                    { progress, bytes, speed ->
                        downloadProgress = progress
                        downloadedBytes = bytes
                        downloadSpeed = speed
                    },
                    { error ->
                        downloadError = error
                        showErrorDialog = true
                        isDownloading = false
                    }
                )
            }
        }
    }
}

@Composable
fun UpdateCard(
    isCheckingUpdate: Boolean,
    updateAvailable: Boolean,
    updateMessage: String,
    latestVersion: String,
    currentVersion: String,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "检查更新",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (updateAvailable) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (updateMessage.isNotEmpty()) {
                    Text(
                        text = updateMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (updateAvailable && latestVersion.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "发现新版本 $latestVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDownloadUpdate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("下载更新")
                }
            } else if (!updateAvailable && !isCheckingUpdate && updateMessage.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前已是最新版本",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DownloadProgressDialog(
    fileName: String,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    speed: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("正在下载")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = speed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("后台下载")
            }
        }
    )
}

@Composable
fun DownloadErrorDialog(
    error: String,
    downloadUrl: String,
    onRetry: () -> Unit,
    onUseMirror: () -> Unit,
    onOpenBrowser: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyError: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("下载失败")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "下载失败，请尝试以下方式：",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Divider()
                
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重试")
                }
                
                Button(
                    onClick = onUseMirror,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("使用镜像站下载")
                }
                
                OutlinedButton(
                    onClick = onOpenBrowser,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("用浏览器下载")
                }
                
                OutlinedButton(
                    onClick = onCopyLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("复制下载链接")
                }
                
                OutlinedButton(
                    onClick = onCopyError,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("复制错误信息")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

suspend fun checkForUpdate(
    context: Context,
    currentVersion: String,
    onSuccess: (String, String, String, Long, String) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/Timome-Sudo/IPcheck/releases/latest"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                val body = json.optString("body", "")
                val name = json.optString("name", "")
                
                // 获取APK下载链接
                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                var fileSize = 0L
                var fileName = ""
                
                if (assets != null && assets.length() > 0) {
                    val asset = assets.getJSONObject(0)
                    downloadUrl = asset.optString("browser_download_url", "")
                    fileSize = asset.optLong("size", 0L)
                    fileName = asset.optString("name", "")
                }

                // 移除版本号前的 'v' 字符（如果有）
                val latestVersion = tagName.removePrefix("v")

                if (latestVersion.isNotEmpty() && latestVersion != currentVersion) {
                    val message = if (name.isNotEmpty()) {
                        "$name\n$body"
                    } else {
                        body
                    }
                    onSuccess(latestVersion, message, downloadUrl, fileSize, fileName)
                } else {
                    onSuccess(latestVersion, "当前已是最新版本", "", 0L, "")
                }
            } else {
                onError("检查更新失败: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            onError("检查更新失败: ${e.message}")
        }
    }
}

suspend fun startDownload(
    context: Context,
    url: String,
    fileName: String,
    fileSize: Long,
    onProgress: (Float, Long, String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val request = android.app.DownloadManager.Request(Uri.parse(url))
        request.setTitle("下载更新")
        request.setDescription("正在下载 $fileName")
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val downloadId = downloadManager.enqueue(request)
        
        // 使用协程来查询下载进度
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val query = android.app.DownloadManager.Query().setFilterById(downloadId)
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()
            
            while (true) {
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloadedIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                    val reasonIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_REASON)
                    
                    if (bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0 && statusIndex >= 0) {
                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)
                        val status = cursor.getInt(statusIndex)
                        
                        if (bytesTotal > 0) {
                            val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                            
                            // 计算下载速度
                            val currentTime = System.currentTimeMillis()
                            val timeDiff = (currentTime - lastTime) / 1000.0
                            if (timeDiff > 0) {
                                val speedBytes = bytesDownloaded - lastBytes
                                val speedBytesPerSec = (speedBytes / timeDiff).toLong()
                                val speed = formatSpeed(speedBytesPerSec)
                                onProgress(progress, bytesDownloaded, speed)
                                lastBytes = bytesDownloaded
                                lastTime = currentTime
                            }
                        }
                        
                        if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                            break
                        } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                            val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                            val errorMsg = when (reason) {
                                android.app.DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "文件已存在"
                                android.app.DownloadManager.ERROR_DEVICE_NOT_FOUND -> "设备未找到"
                                android.app.DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
                                android.app.DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP数据错误"
                                android.app.DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "重定向过多"
                                android.app.DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "未处理的HTTP代码"
                                android.app.DownloadManager.ERROR_UNKNOWN -> "未知错误"
                                else -> "下载失败（错误码：$reason）"
                            }
                            onError(errorMsg)
                            break
                        }
                    }
                }
                cursor.close()
                kotlinx.coroutines.delay(500)
            }
        }
    } catch (e: Exception) {
        onError("下载失败: ${e.message}")
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("文本", text)
    clipboard.setPrimaryClip(clip)
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

fun formatSpeed(bytesPerSecond: Long): String {
    return "${formatBytes(bytesPerSecond)}/s"
}