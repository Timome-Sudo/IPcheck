package com.timome.ipcheck.viewmodel

import androidx.lifecycle.ViewModel
import com.timome.ipcheck.model.Permission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionViewModel : ViewModel() {
    private val _permissionStates = MutableStateFlow<Map<Permission, Boolean>>(emptyMap())
    val permissionStates: StateFlow<Map<Permission, Boolean>> = _permissionStates.asStateFlow()

    private val _agreedToTerms = MutableStateFlow(false)
    val agreedToTerms: StateFlow<Boolean> = _agreedToTerms.asStateFlow()

    private val _countdown = MutableStateFlow(15)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _pendingPermission = MutableStateFlow<Permission?>(null)
    val pendingPermission: StateFlow<Permission?> = _pendingPermission.asStateFlow()

    init {
        val initialStates = Permission.values().associateWith { false }
        _permissionStates.value = initialStates
    }

    fun requestPermission(permission: Permission) {
        _pendingPermission.value = permission
    }

    fun onPermissionResult(permission: Permission, isGranted: Boolean) {
        updatePermissionState(permission, isGranted)
        _pendingPermission.value = null
    }

    fun updatePermissionState(permission: Permission, granted: Boolean) {
        val currentStates = _permissionStates.value.toMutableMap()
        currentStates[permission] = granted
        _permissionStates.value = currentStates
    }

    fun setAgreedToTerms(agreed: Boolean) {
        _agreedToTerms.value = agreed
    }

    fun startCountdown() {
        _countdown.value = 15
    }

    fun decrementCountdown() {
        if (_countdown.value > 0) {
            _countdown.value = _countdown.value - 1
        }
    }

    fun areRequiredPermissionsGranted(): Boolean {
        return Permission.values()
            .filter { it.isRequired }
            .all { _permissionStates.value[it] == true }
    }
}