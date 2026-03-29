package com.timome.ipcheck.viewmodel

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    object Classifying : ScanState()
    object FetchingNames : ScanState()
    object Completed : ScanState()
}