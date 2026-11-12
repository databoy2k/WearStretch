package com.db2k.wearstretch.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.db2k.wearstretch.model.Routine

/** A routine the phone suggested (e.g. a cool-down after an openGym workout). */
data class RoutineSuggestion(
    val routineId: String,
    val reason: String,
    val receivedAtMillis: Long,
    /** Set when pinned from the tile, which only knows the routine's name. */
    val routineName: String? = null
) {
    fun matches(routine: Routine) =
        routine.id == routineId || (routineName != null && routine.name == routineName)
}

object RoutineStore {
    val receivedRoutines = mutableStateListOf<Routine>()

    val suggestion = mutableStateOf<RoutineSuggestion?>(null)

    /** Suggestions go stale: a cool-down suggested hours ago is no longer relevant. */
    fun currentSuggestion(nowMillis: Long = System.currentTimeMillis()): RoutineSuggestion? =
        suggestion.value?.takeIf { nowMillis - it.receivedAtMillis < 3 * 60 * 60 * 1000L }
}
