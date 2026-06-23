package com.db2k.wearstretch.data

import android.content.Context
import androidx.core.content.edit
import com.db2k.wearstretch.model.Routine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoutineRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("routines_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveRoutines(routines: List<Routine>) {
        val json = gson.toJson(routines)
        sharedPreferences.edit { putString("saved_routines", json) }
    }

    fun loadRoutines(): List<Routine> {
        val json = sharedPreferences.getString("saved_routines", null)
        if ((json == null) || (json == "[]")) {
            return PresetData.presetRoutines
        }
        val type = object : TypeToken<List<Routine>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (_: Exception) {
            PresetData.presetRoutines
        }
    }
}
