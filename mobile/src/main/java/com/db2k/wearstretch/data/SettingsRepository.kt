package com.db2k.wearstretch.data

import android.content.Context

data class AppSettings(
    val trackHeartRate: Boolean = true,
    val trackLocation: Boolean = false,
    val trackSteps: Boolean = true,
    val showAnimations: Boolean = true,
    val startDelaySeconds: Int = 10
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putBoolean("track_hr", settings.trackHeartRate)
            putBoolean("track_location", settings.trackLocation)
            putBoolean("track_steps", settings.trackSteps)
            putBoolean("show_animations", settings.showAnimations)
            putInt("start_delay", settings.startDelaySeconds)
            apply()
        }
    }

    fun loadSettings(): AppSettings {
        return AppSettings(
            trackHeartRate = prefs.getBoolean("track_hr", true),
            trackLocation = prefs.getBoolean("track_location", false),
            trackSteps = prefs.getBoolean("track_steps", true),
            showAnimations = prefs.getBoolean("show_animations", true),
            startDelaySeconds = prefs.getInt("start_delay", 10)
        )
    }
}
