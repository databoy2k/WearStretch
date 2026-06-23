package com.db2k.wearstretch.data

import android.content.Context
import androidx.core.content.edit
import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StretchRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("stretches_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveStretches(stretches: List<Stretch>) {
        val json = gson.toJson(stretches)
        sharedPreferences.edit { putString("saved_stretches", json) }
    }

    fun loadStretches(): List<Stretch> {
        val json = sharedPreferences.getString("saved_stretches", null)
        if (json == null || json == "[]") {
            return PresetData.presetStretches
        }
        val type = object : TypeToken<List<Stretch>>() {}.type
        return try {
            val list: List<Stretch> = gson.fromJson(json, type)
            // Migration: Ensure category is never null for old data
            list.map { stretch ->
                @Suppress("SENSELESS_COMPARISON")
                if (stretch.category == null) {
                    stretch.copy(category = "Other")
                } else {
                    stretch
                }
            }
        } catch (_: Exception) {
            PresetData.presetStretches
        }
    }

    fun getExportJson(stretches: List<Stretch>, routines: List<Routine>): String {
        val exportData = mutableMapOf<String, Any>()
        exportData["stretches"] = stretches
        exportData["routines"] = routines
        return gson.toJson(exportData)
    }

    fun parseImportJson(json: String): Pair<List<Stretch>, List<Routine>>? {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(json, type)
            
            val stretchesJson = gson.toJson(data["stretches"])
            val routinesJson = gson.toJson(data["routines"])
            
            val stretchListType = object : TypeToken<List<Stretch>>() {}.type
            val routineListType = object : TypeToken<List<Routine>>() {}.type
            
            val stretches: List<Stretch> = gson.fromJson(stretchesJson, stretchListType)
            val routines: List<Routine> = gson.fromJson(routinesJson, routineListType)
            
            Pair(stretches, routines)
        } catch (_: Exception) {
            null
        }
    }
}
