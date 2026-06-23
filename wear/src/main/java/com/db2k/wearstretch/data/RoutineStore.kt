package com.db2k.wearstretch.data

import androidx.compose.runtime.mutableStateListOf
import com.db2k.wearstretch.model.Routine

object RoutineStore {
    val receivedRoutines = mutableStateListOf<Routine>()
}
