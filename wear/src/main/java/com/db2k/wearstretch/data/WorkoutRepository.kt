package com.db2k.wearstretch.data

import android.content.Context
import com.db2k.wearstretch.model.WorkoutRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WorkoutRepository(context: Context) {
    private val prefs = context.getSharedPreferences("workout_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveWorkout(record: WorkoutRecord) {
        val history = getHistory().toMutableList()
        history.add(0, record) // Newest first
        saveHistory(history)
    }

    fun updateSyncStatus(recordId: String, synced: Boolean) {
        val history = getHistory().map {
            if (it.id == recordId) it.copy(isSynced = synced) else it
        }
        saveHistory(history)
    }

    fun getHistory(): List<WorkoutRecord> {
        val json = prefs.getString("history", null) ?: return emptyList()
        val type = object : TypeToken<List<WorkoutRecord>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getUnsyncedWorkouts(): List<WorkoutRecord> {
        return getHistory().filter { !it.isSynced }
    }

    private fun saveHistory(history: List<WorkoutRecord>) {
        val json = gson.toJson(history)
        prefs.edit().putString("history", json).apply()
    }
}
