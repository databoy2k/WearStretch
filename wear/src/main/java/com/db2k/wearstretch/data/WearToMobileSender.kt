package com.db2k.wearstretch.data

import android.content.Context
import com.db2k.wearstretch.model.DataLayerPaths
import com.google.android.gms.wearable.Wearable
import com.google.gson.JsonObject
import kotlinx.coroutines.tasks.await

class WearToMobileSender(context: Context) {
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun sendMessage(path: String, data: ByteArray) {
        val nodes = nodeClient.connectedNodes.await()
        for (node in nodes) {
            messageClient.sendMessage(node.id, path, data).await()
        }
    }

    suspend fun syncRecord(record: com.db2k.wearstretch.model.WorkoutRecord): Boolean {
        return try {
            val data = JsonObject().apply {
                addProperty("id", record.id)
                addProperty("name", record.routineName)
                addProperty("startTimeMillis", record.startTimeMillis)
                addProperty("endTimeMillis", record.endTimeMillis)
                val hrArray = com.google.gson.JsonArray()
                record.heartRateSamples.forEach { hrArray.add(it) }
                add("heartRateSamples", hrArray)
                addProperty("stepCount", record.stepCount)
                addProperty("location", record.location)
            }.toString().toByteArray()
            
            sendMessage(DataLayerPaths.WORKOUT_COMPLETED, data)
            true
        } catch (e: Exception) {
            android.util.Log.e("WearStretch", "Failed to sync record", e)
            false
        }
    }
}
