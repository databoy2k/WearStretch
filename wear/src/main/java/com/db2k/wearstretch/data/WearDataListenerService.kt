package com.db2k.wearstretch.data

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import com.db2k.wearstretch.model.DataLayerPaths
import com.db2k.wearstretch.model.Routine
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking

@SuppressLint("VisibleForTests")
class WearDataListenerService : WearableListenerService() {
    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            if (path == DataLayerPaths.ROUTINE_LIST) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    runBlocking {
                        processRoutinesList(event.dataItem)
                    }
                }
            } else if (path == DataLayerPaths.SETTINGS) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    processSettings(event.dataItem)
                }
            } else if (path?.startsWith("/routines/") == true) {
                // Handle legacy individual items by deleting them
                Wearable.getDataClient(this).deleteDataItems(event.dataItem.uri)
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WearStretch", "Message received on path: ${messageEvent.path}")
        if (messageEvent.path == DataLayerPaths.REFRESH_ROUTINES) {
            Log.d("WearStretch", "Refresh signal received")
        } else if (messageEvent.path == DataLayerPaths.START_APP) {
            Log.d("WearStretch", "Start app signal received")
            val intent = Intent(this, com.db2k.wearstretch.presentation.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun processRoutinesList(dataItem: DataItem) {
        val dataMapItem = DataMapItem.fromDataItem(dataItem)
        val dataMap = dataMapItem.dataMap
        val json = dataMap.getString("routines_json") ?: return
        val type = object : TypeToken<List<Routine>>() {}.type
        val routines: List<Routine> = gson.fromJson(json, type)
        
        Log.d("WearStretch", "Received list of ${routines.size} routines")
        
        RoutineStore.receivedRoutines.clear()
        RoutineStore.receivedRoutines.addAll(routines)
    }

    private fun processSettings(dataItem: DataItem) {
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
        SettingsStore.trackHeartRate.value = dataMap.getBoolean("track_hr")
        SettingsStore.trackLocation.value = dataMap.getBoolean("track_location")
        SettingsStore.trackSteps.value = dataMap.getBoolean("track_steps")
        SettingsStore.showAnimations.value = dataMap.getBoolean("show_animations")
        SettingsStore.startDelaySeconds.value = dataMap.getInt("start_delay", 10)
        SettingsStore.mobileVersion.value = dataMap.getString("mobile_version") ?: ""
        Log.d("WearStretch", "Settings updated from phone")
    }
}
