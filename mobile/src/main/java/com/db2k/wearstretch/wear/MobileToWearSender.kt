package com.db2k.wearstretch.wear

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.db2k.wearstretch.model.DataLayerPaths
import com.db2k.wearstretch.model.Routine
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import java.io.File

@SuppressLint("VisibleForTests")
class MobileToWearSender(context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val gson = Gson()

    suspend fun syncAllRoutines(routines: List<Routine>) {
        try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.ROUTINE_LIST)
            val dataMap = putDataMapReq.dataMap
            dataMap.putString("routines_json", gson.toJson(routines))
            dataMap.putLong("timestamp", System.currentTimeMillis())
            
            // Add custom image assets
            routines.flatMap { it.stretches }.forEach { stretch ->
                stretch.customImageUri?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val asset = Asset.createFromBytes(file.readBytes())
                        dataMap.putAsset("asset_${stretch.id}", asset)
                    }
                }
            }
            
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()
            
            // Trigger watch refresh
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerPaths.REFRESH_ROUTINES, byteArrayOf()).await()
            }
            Log.d("StretchWear", "Synced ${routines.size} routines")
        } catch (e: Exception) {
            Log.e("StretchWear", "Sync all routines failed")
            throw e
        }
    }

    suspend fun syncSettings(settings: com.db2k.wearstretch.data.AppSettings) {
        try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.SETTINGS)
            val dataMap = putDataMapReq.dataMap
            dataMap.putBoolean("track_hr", settings.trackHeartRate)
            dataMap.putBoolean("track_location", settings.trackLocation)
            dataMap.putBoolean("track_steps", settings.trackSteps)
            dataMap.putBoolean("show_animations", settings.showAnimations)
            dataMap.putInt("start_delay", settings.startDelaySeconds)
            dataMap.putString("mobile_version", com.db2k.wearstretch.BuildConfig.VERSION_NAME)
            dataMap.putLong("timestamp", System.currentTimeMillis())
            
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq).await()
            Log.d("StretchWear", "Settings synced")
        } catch (_: Exception) {
            Log.e("StretchWear", "Settings sync failed")
        }
    }

    suspend fun launchWatchApp() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            for (node in nodes) {
                messageClient.sendMessage(node.id, DataLayerPaths.START_APP, byteArrayOf()).await()
            }
        } catch (_: Exception) {
            Log.e("StretchWear", "Failed to launch watch app")
        }
    }

    suspend fun clearOldIndividualRoutines() {
        try {
            val items = dataClient.dataItems.await()
            for (item in items) {
                if (item.uri.path?.startsWith("/routines/") == true) {
                    dataClient.deleteDataItems(item.uri).await()
                }
            }
        } catch (_: Exception) {
            Log.e("StretchWear", "Failed to clear old routines")
        }
    }
}
