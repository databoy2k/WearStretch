package com.db2k.wearstretch.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.db2k.wearstretch.R
import com.db2k.wearstretch.health.HealthConnectManager
import com.db2k.wearstretch.model.DataLayerPaths
import com.db2k.wearstretch.data.SettingsRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class MobileDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var healthConnectManager: HealthConnectManager
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val channelId = "workout_sync_channel"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Snapshot before the buffer is released at the end of this callback.
        val changed = dataEvents.filter { it.type == DataEvent.TYPE_CHANGED }
            .map { it.dataItem.freeze() }

        for (item in changed) {
            val path = item.uri.path ?: continue
            when {
                path == DataLayerPaths.SETTINGS -> applySettings(item)
                path.startsWith(DataLayerPaths.WORKOUT_RESULT_PREFIX) -> scope.launch { consumeWorkout(item) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        healthConnectManager = HealthConnectManager(this)
        createNotificationChannel()
    }

    private fun applySettings(item: DataItem) {
        try {
            val dataMap = DataMapItem.fromDataItem(item).dataMap
            val trackHr = dataMap.getBoolean("track_hr")
            val trackLocation = dataMap.getBoolean("track_location")
            val trackSteps = dataMap.getBoolean("track_steps")
            val showAnimations = dataMap.getBoolean("show_animations")
            val startDelay = dataMap.getInt("start_delay", 10)

            val repository = SettingsRepository(this)
            val updated = repository.loadSettings().copy(
                trackHeartRate = trackHr,
                trackLocation = trackLocation,
                trackSteps = trackSteps,
                showAnimations = showAnimations,
                startDelaySeconds = startDelay
            )
            repository.saveSettings(updated)
            Log.d("StretchWear", "Saved settings from watch: hr=$trackHr, steps=$trackSteps, location=$trackLocation")
        } catch (e: Exception) {
            Log.e("StretchWear", "Failed to update phone settings from watch: ${e.message}")
        }
    }

    /**
     * Writes a workout DataItem received from the watch to Health Connect, then
     * deletes the item to acknowledge it. If the write fails the item is left in
     * place so the phone retries on the next data event, and the user is told.
     */
    private suspend fun consumeWorkout(item: DataItem) {
        val json = try {
            val dataMap = DataMapItem.fromDataItem(item).dataMap
            dataMap.getString("workout_json") ?: return
        } catch (e: Exception) {
            Log.e("StretchWear", "Failed to read workout DataItem: ${e.message}", e)
            return
        }

        val obj: JsonObject = try {
            JsonParser.parseString(json).asJsonObject
        } catch (e: Exception) {
            Log.e("StretchWear", "Malformed workout json: ${e.message}", e)
            // Unparseable payload will never succeed — delete it so it stops recurring.
            runCatching { dataClient.deleteDataItems(item.uri).await() }
            return
        }

        fun str(key: String, default: String) =
            if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asString else default
        fun strOrNull(key: String) =
            if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asString else null
        fun long(key: String) =
            if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asLong else System.currentTimeMillis()
        fun int(key: String) =
            if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asInt else 0

        val routineName = str("name", "Stretching Session")
        val recordId = str("id", System.currentTimeMillis().toString())
        val startTimeMillis = long("startTimeMillis")
        val endTimeMillis = long("endTimeMillis")
        val stepCount = int("stepCount")
        val location = strOrNull("location")

        val hrSamples = mutableListOf<Int>()
        if (obj.has("heartRateSamples") && !obj.get("heartRateSamples").isJsonNull) {
            obj.get("heartRateSamples").asJsonArray.forEach { hrSamples.add(it.asInt) }
        }

        try {
            healthConnectManager.writeWorkout(
                id = recordId,
                startTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault()),
                endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault()),
                title = routineName,
                heartRateSamples = hrSamples,
                stepCount = stepCount,
                location = location
            )
            // Success — acknowledge by deleting the item so it isn't reprocessed.
            dataClient.deleteDataItems(item.uri).await()
            showCompletionNotification(routineName)
        } catch (e: Exception) {
            // Leave the DataItem in place; the phone will retry on the next event.
            Log.e("StretchWear", "Failed to write workout to Health Connect: ${e.message}", e)
            showFailureNotification(routineName)
        }
    }

    private fun showCompletionNotification(routineName: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Stretch Saved")
            .setContentText("Your $routineName workout was logged to Health Connect.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Permission check handled on app startup, but technically required here for lint
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showFailureNotification(routineName: String) {
        val text = "Your $routineName workout couldn't be logged. Open the app to grant Health Connect access."
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Stretch Not Saved")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Workout Sync",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
