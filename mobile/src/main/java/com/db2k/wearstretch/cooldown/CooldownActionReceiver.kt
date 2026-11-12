package com.db2k.wearstretch.cooldown

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.db2k.wearstretch.wear.MobileToWearSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Handles "Start on watch" from the cool-down notification. */
class CooldownActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_START_ON_WATCH) return
        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: return
        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val app = context.applicationContext

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sent = withTimeoutOrNull(9_000) {
                    MobileToWearSender(app).suggestRoutine(routineId, reason)
                } ?: false
                withContext(Dispatchers.Main) {
                    if (sent) {
                        app.getSystemService(NotificationManager::class.java).cancel(notificationId)
                    } else {
                        Toast.makeText(app, "Watch not reachable", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_START_ON_WATCH = "com.db2k.wearstretch.COOLDOWN_START_ON_WATCH"
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_REASON = "reason"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
