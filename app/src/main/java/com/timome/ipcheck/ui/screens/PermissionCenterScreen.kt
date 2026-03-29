package com.timome.ipcheck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timome.ipcheck.R
import com.timome.ipcheck.model.Permission
import com.timome.ipcheck.ui.theme.PermissionGranted
import com.timome.ipcheck.ui.theme.PermissionPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCenterScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRequestPermission: (Permission) -> Unit,
    onOpenAppSettings: () -> Unit,
    onShowToast: (String) -> Unit,
    permissionStates: Map<Permission, Boolean>,
    allRequiredGranted: Boolean,
    progress: Float = 0.66f,
    modifier: Modifier = Modifier
) {
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
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        enabled = allRequiredGranted
                    ) {
                        Text("完成")
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
                    text = "我们需要以下权限来提供完整的服务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                PermissionGroup(
                    title = "主要权限",
                    permissions = Permission.values().filter { it.isRequired },
                    permissionStates = permissionStates,
                    onRequestPermission = onRequestPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onShowToast = onShowToast
                )

                Spacer(modifier = Modifier.height(24.dp))

                PermissionGroup(
                    title = "次要权限",
                    permissions = Permission.values().filter { !it.isRequired },
                    permissionStates = permissionStates,
                    onRequestPermission = onRequestPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onShowToast = onShowToast
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionGroup(
    title: String,
    permissions: List<Permission>,
    permissionStates: Map<Permission, Boolean>,
    onRequestPermission: (Permission) -> Unit,
    onOpenAppSettings: () -> Unit,
    onShowToast: (String) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    permissions.forEach { permission ->
        val granted = permissionStates[permission] == true
        PermissionCard(
            permission = permission,
            granted = granted,
            onRequestPermission = { onRequestPermission(permission) },
            onOpenAppSettings = onOpenAppSettings,
            onShowToast = onShowToast
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun PermissionCard(
    permission: Permission,
    granted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var requestCount by remember { mutableStateOf(0) }
    val showSettingsButton = requestCount >= 3 && !granted

    val backgroundColor = if (granted) PermissionGranted.copy(alpha = 0.3f) else PermissionPending.copy(alpha = 0.1f)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (granted) {
                    // 权限授予成功，显示勾图标
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已授予",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // 权限未授予，显示锁图标
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = permission.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when {
                            granted -> "已授予"
                            showSettingsButton -> "请在系统设置中手动授予"
                            else -> "待获取"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = !granted && !showSettingsButton,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Button(
                    onClick = {
                        requestCount++
                        onRequestPermission()
                    },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("去获取", style = MaterialTheme.typography.bodySmall)
                }
            }

            AnimatedVisibility(
                visible = showSettingsButton,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OutlinedButton(
                    onClick = {
                        onOpenAppSettings()
                        onShowToast("请在系统设置中手动授予 ${permission.displayName} 权限")
                    },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("去设置", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}