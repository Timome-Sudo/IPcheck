package com.timome.ipcheck.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

    val githubUrl = "https://github.com/Timome-Sudo/IPcheck"

    val checkUpdate = {
        isCheckingUpdate = true
        latestVersion = null
        updateAvailable = false
        updateMessage = ""
    }

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
                    // 在LaunchedEffect中调用suspend函数
                },
                onDownloadUpdate = {
                    openUrl(context, "$githubUrl/releases")
                }
            )

            // 处理检查更新逻辑
            LaunchedEffect(isCheckingUpdate) {
                if (isCheckingUpdate) {
                    checkForUpdate(
                        context = context,
                        currentVersion = versionName,
                        onSuccess = { version, message ->
                            latestVersion = version
                            updateAvailable = true
                            updateMessage = message
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
                    Text("前往下载")
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
    onSuccess: (String, String) -> Unit,
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

                // 移除版本号前的 'v' 字符（如果有）
                val latestVersion = tagName.removePrefix("v")

                if (latestVersion.isNotEmpty() && latestVersion != currentVersion) {
                    val message = if (name.isNotEmpty()) {
                        "$name\n$body"
                    } else {
                        body
                    }
                    onSuccess(latestVersion, message)
                } else {
                    onSuccess(latestVersion, "当前已是最新版本")
                }
            } else {
                onError("检查更新失败: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            onError("检查更新失败: ${e.message}")
        }
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}