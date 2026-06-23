package com.db2k.wearstretch.model

data class Routine(
    val id: String,
    val name: String,
    val stretches: List<Stretch>,
    val defaultBreakDurationSeconds: Int = 5
)
