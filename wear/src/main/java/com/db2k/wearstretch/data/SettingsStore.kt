package com.db2k.wearstretch.data

import androidx.compose.runtime.mutableStateOf

object SettingsStore {
    var trackHeartRate = mutableStateOf(true)
    var trackLocation = mutableStateOf(false)
    var trackSteps = mutableStateOf(true)
    var showAnimations = mutableStateOf(true)
    var startDelaySeconds = mutableStateOf(10)
    var mobileVersion = mutableStateOf("")
}
