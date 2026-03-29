package com.timome.ipcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timome.ipcheck.model.AppScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {
    private val _currentScreen: MutableStateFlow<AppScreen> = MutableStateFlow(AppScreen.Welcome)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _canProceed = MutableStateFlow(false)
    val canProceed: StateFlow<Boolean> = _canProceed.asStateFlow()

    private var countdownJob: Job? = null

    fun navigateToNext() {
        when (_currentScreen.value) {
            AppScreen.Welcome -> {
                _currentScreen.value = AppScreen.TermsOfUse
                _progress.value = 0.33f
            }
            AppScreen.TermsOfUse -> {
                _currentScreen.value = AppScreen.PermissionCenter
                _progress.value = 0.66f
                _canProceed.value = false
            }
            AppScreen.PermissionCenter -> {
                _currentScreen.value = AppScreen.Main
                _progress.value = 1f
            }
            AppScreen.Main -> {}
        }
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            AppScreen.TermsOfUse -> {
                _currentScreen.value = AppScreen.Welcome
                _progress.value = 0f
            }
            AppScreen.PermissionCenter -> {
                _currentScreen.value = AppScreen.TermsOfUse
                _progress.value = 0.33f
            }
            else -> {}
        }
    }

    fun navigateToAppScreen(screen: AppScreen) {
        _currentScreen.value = screen
        when (screen) {
            AppScreen.Welcome -> _progress.value = 0f
            AppScreen.TermsOfUse -> _progress.value = 0.33f
            AppScreen.PermissionCenter -> _progress.value = 0.66f
            AppScreen.Main -> _progress.value = 1f
        }
    }

    fun startCountdown(onComplete: () -> Unit) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            delay(15000)
            onComplete()
        }
    }

    fun setCanProceed(canProceed: Boolean) {
        _canProceed.value = canProceed
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}