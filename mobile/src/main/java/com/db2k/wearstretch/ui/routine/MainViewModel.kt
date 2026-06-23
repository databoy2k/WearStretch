package com.db2k.wearstretch.ui.routine

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.db2k.wearstretch.data.AppSettings
import com.db2k.wearstretch.data.PresetData
import com.db2k.wearstretch.data.RoutineRepository
import com.db2k.wearstretch.data.SettingsRepository
import com.db2k.wearstretch.data.StretchRepository
import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch
import com.db2k.wearstretch.wear.MobileToWearSender
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel : ViewModel() {
    var routines = mutableStateListOf<Routine>()
    var libraryStretches = mutableStateListOf<Stretch>()
    
    // State for the builder
    var currentRoutineName = mutableStateOf("My New Routine")
    var defaultBreakDuration = mutableStateOf("5")
    var selectedStretches = mutableStateListOf<Stretch>()
    
    var editingRoutine = mutableStateOf<Routine?>(null)
    var editingStretch = mutableStateOf<Stretch?>(null)
    
    var settings = mutableStateOf(AppSettings())
    
    private var wearDataSender: MobileToWearSender? = null
    private var routineRepository: RoutineRepository? = null
    private var stretchRepository: StretchRepository? = null
    private var settingsRepository: SettingsRepository? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (wearDataSender == null) {
            wearDataSender = MobileToWearSender(context.applicationContext)
        }
        if (routineRepository == null) {
            routineRepository = RoutineRepository(context.applicationContext)
            routines.clear()
            routines.addAll(routineRepository!!.loadRoutines())
        }
        if (stretchRepository == null) {
            stretchRepository = StretchRepository(context.applicationContext)
            libraryStretches.clear()
            libraryStretches.addAll(stretchRepository!!.loadStretches())
        }
        if (settingsRepository == null) {
            settingsRepository = SettingsRepository(context.applicationContext)
            settings.value = settingsRepository!!.loadSettings()
            syncSettings()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settings.value = newSettings
        settingsRepository?.saveSettings(newSettings)
        syncSettings()
    }

    fun reloadSettings(context: Context) {
        if (settingsRepository == null) {
            settingsRepository = SettingsRepository(context.applicationContext)
        }
        settings.value = settingsRepository!!.loadSettings()
    }

    private fun syncSettings() {
        viewModelScope.launch {
            wearDataSender?.syncSettings(settings.value)
        }
    }

    fun saveStretch(updatedStretch: Stretch) {
        val index = libraryStretches.indexOfFirst { it.id == updatedStretch.id }
        if (index != -1) {
            libraryStretches[index] = updatedStretch
        } else {
            libraryStretches.add(updatedStretch)
        }
        stretchRepository?.saveStretches(libraryStretches.toList())
        
        // Also update any routines that use this stretch
        routines.forEachIndexed { rIndex, routine ->
            val sIndices = routine.stretches.indices.filter { routine.stretches[it].id == updatedStretch.id }
            if (sIndices.isNotEmpty()) {
                val newStretches = routine.stretches.toMutableList()
                sIndices.forEach { newStretches[it] = updatedStretch }
                routines[rIndex] = routine.copy(stretches = newStretches)
            }
        }
        routineRepository?.saveRoutines(routines.toList())
        editingStretch.value = null
    }

    fun getExportData(): String {
        return stretchRepository?.getExportJson(libraryStretches.toList(), routines.toList()) ?: ""
    }

    fun importData(json: String, mode: ImportMode) {
        val imported = stretchRepository?.parseImportJson(json) ?: return
        val newStretches = imported.first
        val newRoutines = imported.second

        when (mode) {
            ImportMode.ADD_ONLY -> {
                newStretches.forEach { stretch ->
                    if (libraryStretches.none { it.id == stretch.id }) {
                        libraryStretches.add(stretch)
                    }
                }
                newRoutines.forEach { routine ->
                    if (routines.none { it.id == routine.id }) {
                        routines.add(routine)
                    }
                }
            }
            ImportMode.OVERWRITE -> {
                newStretches.forEach { stretch ->
                    val index = libraryStretches.indexOfFirst { it.id == stretch.id }
                    if (index != -1) {
                        libraryStretches[index] = stretch
                    } else {
                        libraryStretches.add(stretch)
                    }
                }
                newRoutines.forEach { routine ->
                    val index = routines.indexOfFirst { it.id == routine.id }
                    if (index != -1) {
                        routines[index] = routine
                    } else {
                        routines.add(routine)
                    }
                }
            }
            ImportMode.REMOVE_MISSING -> {
                libraryStretches.clear()
                libraryStretches.addAll(newStretches)
                routines.clear()
                routines.addAll(newRoutines)
            }
        }
        
        stretchRepository?.saveStretches(libraryStretches.toList())
        routineRepository?.saveRoutines(routines.toList())
        
        // Sync everything to watch
        viewModelScope.launch {
            wearDataSender?.syncAllRoutines(routines.toList())
        }
    }

    fun startEditingRoutine(routine: Routine) {
        editingRoutine.value = routine
        currentRoutineName.value = routine.name
        defaultBreakDuration.value = routine.defaultBreakDurationSeconds.toString()
        selectedStretches.clear()
        selectedStretches.addAll(routine.stretches)
    }

    fun cancelEditingRoutine() {
        editingRoutine.value = null
        currentRoutineName.value = "My New Routine"
        defaultBreakDuration.value = "5"
        selectedStretches.clear()
    }

    fun addStretchToRoutine(stretch: Stretch) {
        val breakTime = defaultBreakDuration.value.toIntOrNull() ?: 5
        selectedStretches.add(stretch.copy(breakDurationSeconds = breakTime))
    }

    fun applyGlobalBreakTimeToAll(seconds: Int) {
        selectedStretches.indices.forEach { i ->
            selectedStretches[i] = selectedStretches[i].copy(breakDurationSeconds = seconds)
        }
    }

    fun removeStretchFromRoutine(index: Int) {
        if (index in selectedStretches.indices) {
            selectedStretches.removeAt(index)
        }
    }

    fun moveStretch(fromIndex: Int, toIndex: Int) {
        if (fromIndex in selectedStretches.indices && toIndex in selectedStretches.indices) {
            val item = selectedStretches.removeAt(fromIndex)
            selectedStretches.add(toIndex, item)
        }
    }

    fun saveAndSyncRoutine(onSuccess: () -> Unit) {
        if (selectedStretches.isEmpty()) {
            appContext?.let { Toast.makeText(it, "Add some stretches first!", Toast.LENGTH_SHORT).show() }
            return
        }
        
        val breakTime = defaultBreakDuration.value.toIntOrNull() ?: 5
        
        val newRoutine = Routine(
            id = editingRoutine.value?.id ?: UUID.randomUUID().toString(),
            name = currentRoutineName.value,
            stretches = selectedStretches.toList(),
            defaultBreakDurationSeconds = breakTime
        )
        
        if (editingRoutine.value != null) {
            val index = routines.indexOfFirst { it.id == newRoutine.id }
            if (index != -1) routines[index] = newRoutine
        } else {
            routines.add(newRoutine)
        }
        
        routineRepository?.saveRoutines(routines.toList())
        
        viewModelScope.launch {
            try {
                wearDataSender?.syncAllRoutines(routines.toList())
                wearDataSender?.launchWatchApp()
                appContext?.let { Toast.makeText(it, "Routine saved and synced!", Toast.LENGTH_SHORT).show() }
                editingRoutine.value = null
                onSuccess()
            } catch (_: Exception) {
                appContext?.let { Toast.makeText(it, "Sync failed. Is your watch connected?", Toast.LENGTH_LONG).show() }
            }
        }
        
        // Reset builder
        selectedStretches.clear()
        currentRoutineName.value = "My New Routine"
    }

    fun deleteRoutine(routine: Routine) {
        routines.remove(routine)
        routineRepository?.saveRoutines(routines.toList())
        viewModelScope.launch {
            try {
                wearDataSender?.syncAllRoutines(routines.toList())
                appContext?.let { Toast.makeText(it, "Routine deleted", Toast.LENGTH_SHORT).show() }
            } catch (_: Exception) {
                appContext?.let { Toast.makeText(it, "Deletion sync failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun clearAllDataLayer() {
        viewModelScope.launch {
            try {
                wearDataSender?.clearOldIndividualRoutines()
                wearDataSender?.syncAllRoutines(routines.toList())
                appContext?.let { Toast.makeText(it, "Data Layer cleaned up", Toast.LENGTH_SHORT).show() }
            } catch (_: Exception) {
                appContext?.let { Toast.makeText(it, "Cleanup failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun resetToPresets() {
        libraryStretches.clear()
        libraryStretches.addAll(PresetData.presetStretches)
        stretchRepository?.saveStretches(libraryStretches.toList())

        routines.clear()
        routines.addAll(PresetData.presetRoutines)
        routineRepository?.saveRoutines(routines.toList())

        viewModelScope.launch {
            wearDataSender?.syncAllRoutines(routines.toList())
        }
        
        appContext?.let { Toast.makeText(it, "Reset to defaults", Toast.LENGTH_SHORT).show() }
    }
}

enum class ImportMode {
    ADD_ONLY, OVERWRITE, REMOVE_MISSING
}
