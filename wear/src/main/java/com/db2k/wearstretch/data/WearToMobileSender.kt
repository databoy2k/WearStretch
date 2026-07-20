package com.db2k.wearstretch.data

import android.content.Context
import android.util.Log
import com.db2k.wearstretch.model.DataLayerPaths
import com.db2k.wearstretch.model.WorkoutRecord
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.tasks.await

/**
 * Publishes completed workouts to the phone via the Wearable **DataClient**.
 *
 * A DataItem is persistent and auto-syncing: the watch writes it once and the
 * OS data layer delivers it to the phone whenever the phone is next reachable —
 * no retry loop, polling, or connectivity checks on the watch. The phone owns
 * the Health Connect sync and acks by deleting the item.
 */
class WearToMobileSender(context: Context) {
    private val dataClient = Wearable.getDataClient(context)

    /**
     * Writes [record] as a DataItem for the phone to consume. Returns true once
     * the item is durably in the data layer (delivery is then guaranteed by the
     * OS). Returns false only if the local write itself failed, in which case
     * the caller should leave the record unsynced for a later re-publish.
     */
    suspend fun publishRecord(record: WorkoutRecord): Boolean {
        return try {
            val json = JsonObject().apply {
                addProperty("id", record.id)
                addProperty("name", record.routineName)
                addProperty("startTimeMillis", record.startTimeMillis)
                addProperty("endTimeMillis", record.endTimeMillis)
                val hrArray = JsonArray()
                record.heartRateSamples.forEach { hrArray.add(it) }
                add("heartRateSamples", hrArray)
                addProperty("stepCount", record.stepCount)
                addProperty("location", record.location)
            }.toString()

            val request = PutDataMapRequest.create("${DataLayerPaths.WORKOUT_RESULT_PREFIX}${record.id}").apply {
                dataMap.putString("workout_json", json)
                // Distinct value so identical re-publishes still register as a change.
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (e: Exception) {
            Log.e("WearStretch", "Failed to publish workout result", e)
            false
        }
    }
}
