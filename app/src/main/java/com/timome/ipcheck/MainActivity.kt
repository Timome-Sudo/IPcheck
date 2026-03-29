package com.timome.ipcheck

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.timome.ipcheck.model.AppScreen
import com.timome.ipcheck.model.Permission
import com.timome.ipcheck.ui.screens.MainScreen
import com.timome.ipcheck.ui.screens.PermissionCenterScreen
import com.timome.ipcheck.ui.screens.TermsOfUseScreen
import com.timome.ipcheck.ui.screens.WelcomeScreen
import com.timome.ipcheck.ui.theme.IPCheckTheme
import com.timome.ipcheck.viewmodel.MainViewModel
import com.timome.ipcheck.viewmodel.OnboardingViewModel
import com.timome.ipcheck.viewmodel.PermissionViewModel
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("IPCheckPrefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    // 电池优化设置页面的 Launcher
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 从电池优化设置页面返回后的回调
        _batteryOptimizationSettingsReturned.value = true
    }

    private val _batteryOptimizationSettingsReturned = MutableStateFlow(false)
    val batteryOptimizationSettingsReturned: StateFlow<Boolean> = _batteryOptimizationSettingsReturned.asStateFlow()

    fun resetBatteryOptimizationFlag() {
        _batteryOptimizationSettingsReturned.value = false
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IPCheckTheme {
                AppNavigation()
            }
        }
    }

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            batteryOptimizationLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun checkAllPermissions(permissionViewModel: PermissionViewModel) {
        Permission.values().forEach { permission ->
            val isGranted = when (permission.permissionString) {
                "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" -> {
                    isBatteryOptimizationIgnored()
                }
                else -> {
                    checkPermission(permission.permissionString)
                }
            }
            permissionViewModel.updatePermissionState(permission, isGranted)
        }
    }
}

@Composable
fun AppNavigation() {
    val activity = LocalContext.current as MainActivity
    val onboardingViewModel: OnboardingViewModel = viewModel()
    val permissionViewModel: PermissionViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(activity.application)
    )

    val currentScreen by onboardingViewModel.currentScreen.collectAsState()
    val permissionStates by permissionViewModel.permissionStates.collectAsState()
    val devices by mainViewModel.devices.collectAsState()
    val scanState by mainViewModel.scanState.collectAsState()
    val elapsedTime by mainViewModel.elapsedTime.collectAsState()
    val scanProgress by mainViewModel.scanProgress.collectAsState()
    val scanProgressMax by mainViewModel.scanProgressMax.collectAsState()
    val classifiedCount by mainViewModel.classifiedCount.collectAsState()
    val nameFetchCount by mainViewModel.nameFetchCount.collectAsState()

    val onboardingCompleted by remember { mutableStateOf(activity.isOnboardingCompleted()) }

    // 当前正在请求的权限
    var pendingPermission by remember { mutableStateOf<Permission?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 权限请求结果回调
        val permission = pendingPermission
        if (permission != null) {
            permissionViewModel.onPermissionResult(permission, isGranted)
            pendingPermission = null
        }
        
        // 重新检查所有权限状态以确保实时更新
        activity.checkAllPermissions(permissionViewModel)
    }

    LaunchedEffect(Unit) {
        // 如果已完成引导，直接跳转到主界面
        if (onboardingCompleted) {
            onboardingViewModel.navigateToAppScreen(AppScreen.Main)
        }

        // 检查电池优化权限状态
        val batteryOptimizationIgnored = activity.isBatteryOptimizationIgnored()
        val batteryPermission = Permission.IGNORE_BATTERY_OPTIMIZATIONS
        permissionViewModel.updatePermissionState(batteryPermission, batteryOptimizationIgnored)
    }

    // 监听电池优化设置页面返回事件
    val batteryOptimizationReturned by activity.batteryOptimizationSettingsReturned.collectAsState()
    LaunchedEffect(batteryOptimizationReturned) {
        if (batteryOptimizationReturned) {
            activity.checkAllPermissions(permissionViewModel)
            activity.resetBatteryOptimizationFlag()
        }
    }

    val progress by onboardingViewModel.progress.collectAsState()

    

        androidx.compose.animation.AnimatedContent(

            targetState = currentScreen,

            transitionSpec = {

                val direction = if (initialState.index < targetState.index) {

                    androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left

                } else {

                    androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right

                }

                slideIntoContainer(

                    towards = direction,

                    animationSpec = tween(300)

                ) togetherWith slideOutOfContainer(

                    towards = direction,

                    animationSpec = tween(300)

                )

            },

            label = "screen_transition"

        ) { targetScreen ->

            when (targetScreen) {

                AppScreen.Welcome -> {

                    WelcomeScreen(

                        onBack = null,

                        onNext = { onboardingViewModel.navigateToNext() },

                        progress = progress

                    )

                }

                AppScreen.TermsOfUse -> {

                    val countdown by permissionViewModel.countdown.collectAsState()

                    val agreed by permissionViewModel.agreedToTerms.collectAsState()

    

                    LaunchedEffect(Unit) {

                        permissionViewModel.startCountdown()

                    }

    

                    LaunchedEffect(countdown) {

                        if (countdown > 0) {

                            kotlinx.coroutines.delay(1000)

                            permissionViewModel.decrementCountdown()

                        }

                    }

    

                    TermsOfUseScreen(

                        onBack = { onboardingViewModel.navigateBack() },

                        onNext = {

                            permissionViewModel.setAgreedToTerms(true)

                            onboardingViewModel.navigateToNext()

                        },

                        progress = progress

                    )

                }

                AppScreen.PermissionCenter -> {

                    val allRequiredGranted by remember {

                        derivedStateOf {

                            Permission.values()

                                .filter { it.isRequired }

                                .all { permissionStates[it] == true }

                        }
                    }

                    // 每次显示权限中心时检查所有权限状态
                    LaunchedEffect(Unit) {
                        activity.checkAllPermissions(permissionViewModel)
                    }

                    LaunchedEffect(allRequiredGranted) {
                        onboardingViewModel.setCanProceed(allRequiredGranted)
                    }

    

                    PermissionCenterScreen(

                        onBack = { onboardingViewModel.navigateBack() },

                        onComplete = {

                            activity.setOnboardingCompleted(true)

                            onboardingViewModel.navigateToNext()

                        },

                        onRequestPermission = { permission ->
                            if (permission.permissionString == "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") {
                                activity.openBatteryOptimizationSettings()
                            } else {
                                pendingPermission = permission
                                requestPermissionLauncher.launch(permission.permissionString)
                            }
                        },

                        onOpenAppSettings = {

                            activity.openAppSettings()

                        },

                        onShowToast = { message ->

                            activity.showToast(message)

                        },

                        permissionStates = permissionStates,

                        allRequiredGranted = allRequiredGranted,

                        progress = progress

                    )

                }

                AppScreen.Main -> {

                    MainScreen(

                        devices = devices,

                        scanState = scanState,

                        elapsedTime = elapsedTime,

                        scanProgress = scanProgress,

                        scanProgressMax = scanProgressMax,

                        classifiedCount = classifiedCount,

                        nameFetchCount = nameFetchCount,

                        onStartScan = { mainViewModel.startScan() },

                        onDeviceOnlineStatusChange = { index, isOnline ->

                            mainViewModel.updateDeviceOnlineStatus(index, isOnline)

                        }

                    )

                }

            }

        }
}

class MainViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}