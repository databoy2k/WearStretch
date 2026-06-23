package com.db2k.wearstretch.model

data class Stretch(
    val id: String,
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val breakDurationSeconds: Int = 5,
    val category: String = "Other",
    val isSplit: Boolean = false,
    val splitBreakDurationSeconds: Int = 5,
    val imageKey: String? = null,
    val customImageUri: String? = null
)
