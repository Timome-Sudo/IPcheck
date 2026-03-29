package com.timome.ipcheck.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    progress: Float = 0.33f,
    modifier: Modifier = Modifier
) {
    var agreed by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(15) }
    var canClick by remember { mutableStateOf(false) }
    var clickCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canClick = true
    }

    val forceEnable = clickCount >= 10

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("返回")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        enabled = agreed && (canClick || forceEnable)
                    ) {
                        Text("下一步")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "欢迎使用网络设备扫描器！",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "在使用本应用之前，请您仔细阅读以下重要条款：",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. **网络扫描权限**\n本应用需要访问您的网络权限以扫描和识别连接设备。\n\n" +
                          "2. **位置权限**\n应用需要位置权限以获取准确的网络信息。\n\n" +
                          "3. **数据隐私**\n所有扫描到的设备信息仅保存在本地设备上，不会上传到任何服务器。\n\n" +
                          "4. **使用目的**\n本应用仅用于个人网络管理，不得用于任何非法目的。\n\n" +
                          "5. **准确性声明**\n设备类型识别和名称获取基于网络特征分析，可能存在误差。\n\n" +
                          "6. **免责条款**\n使用本应用所产生的一切后果由用户自行承担。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = {
                            if (canClick || forceEnable) {
                                agreed = it
                            } else {
                                clickCount++
                                if (clickCount >= 10) {
                                    agreed = true
                                }
                            }
                        },
                        enabled = canClick || forceEnable
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            canClick -> "我已知晓"
                            forceEnable -> "我已知晓（强制确定）"
                            else -> "我已知晓（${countdown}s）"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (clickCount > 0 && clickCount < 10) {
                Text(
                    text = "点击 ${10 - clickCount} 次后可强制确定",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
