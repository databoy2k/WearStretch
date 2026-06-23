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
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class MobileDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var healthConnectManager: HealthConnectManager
    private val channelId = "workout_sync_channel"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            if (path == DataLayerPaths.SETTINGS) {
                if (event.type == DataEvent.TYPE_CHANGED) {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val trackHr = dataMap.getBoolean("track_hr")
                        val trackLocation = dataMap.getBoolean("track_location")
                        val trackSteps = dataMap.getBoolean("track_steps")
                        val showAnimations = dataMap.getBoolean("show_animations")
                        val startDelay = dataMap.getInt("start_delay", 10)
                        
                        val repository = SettingsRepository(this)
                        val current = repository.loadSettings()
                        val updated = current.copy(
                            trackHeartRate = trackHr,
                            trackLocation = trackLocation,
                            trackSteps = trackSteps,
                            showAnimations = showAnimations,
                            startDelaySeconds = startDelay
                        )
                        repository.saveSettings(updated)
                        Log.d("StretchWear", "Saved settings from watch to phone repository: hr=$trackHr, steps=$trackSteps, location=$trackLocation")
                    } catch (e: Exception) {
                        Log.e("StretchWear", "Failed to update phone settings from watch: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        healthConnectManager = HealthConnectManager(this)
        createNotificationChannel()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == DataLayerPaths.WORKOUT_COMPLETED) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(this, "Receiving workout from watch...", android.widget.Toast.LENGTH_SHORT).show()
            }
            try {
                val jsonString = String(messageEvent.data)
                val jsonObject = JsonParser.parseString(jsonString).asJsonObject
                
                val routineName = if (jsonObject.has("name") && !jsonObject.get("name").isJsonNull) jsonObject.get("name").asString else "Stretching Session"
                val recordId = if (jsonObject.has("id") && !jsonObject.get("id").isJsonNull) jsonObject.get("id").asString else System.currentTimeMillis().toString()
                val startTimeMillis = if (jsonObject.has("startTimeMillis") && !jsonObject.get("startTimeMillis").isJsonNull) jsonObject.get("startTimeMillis").asLong else System.currentTimeMillis()
                val endTimeMillis = if (jsonObject.has("endTimeMillis") && !jsonObject.get("endTimeMillis").isJsonNull) jsonObject.get("endTimeMillis").asLong else System.currentTimeMillis()
                val stepCount = if (jsonObject.has("stepCount") && !jsonObject.get("stepCount").isJsonNull) jsonObject.get("stepCount").asInt else 0
                val location = if (jsonObject.has("location") && !jsonObject.get("location").isJsonNull) jsonObject.get("location").asString else null
                
                val hrSamples = mutableListOf<Int>()
                if (jsonObject.has("heartRateSamples") && !jsonObject.get("heartRateSamples").isJsonNull) {
                    jsonObject.get("heartRateSamples").asJsonArray.forEach { 
                        hrSamples.add(it.asInt)
                    }
                }
                
                scope.launch {
                    try {
                        val startTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault())
                        val endTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis), ZoneId.systemDefault())
                        
                        healthConnectManager.writeWorkout(
                            id = recordId,
                            startTime = startTime,
                            endTime = endTime,
                            title = routineName,
                            heartRateSamples = hrSamples,
                            stepCount = stepCount,
                            location = location
                        )
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(this@MobileDataListenerService, "Saved $routineName to history!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showCompletionNotification(routineName)
                    } catch (e: Exception) {
                        Log.e("StretchWear", "Failed to write workout to Health Connect: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("StretchWear", "Failed to parse workout completion message: ${e.message}", e)
            }
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
