package com.db2k.wearstretch.model

data class WorkoutRecord(
    val id: String,
    val routineName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val heartRateSamples: List<Int> = emptyList(),
    val stepCount: Int = 0,
    val location: String? = null,
    val isSynced: Boolean = false
)
