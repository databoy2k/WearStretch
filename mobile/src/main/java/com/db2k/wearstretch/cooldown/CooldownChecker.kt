package com.db2k.wearstretch.cooldown

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.db2k.wearstretch.MainActivity
import com.db2k.wearstretch.R
import com.db2k.wearstretch.data.RoutineRepository
import com.db2k.wearstretch.health.HealthConnectManager
import java.time.Duration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Looks in Health Connect for a workout another app just logged (e.g. openGym)
 * and, if nothing has been stretched since, suggests a matching cool-down routine
 * with a notification. "Start on watch" sends the routine to the watch as
 * suggested; the stretch itself is still only logged by Wear Stretch.
 */
object CooldownChecker {
    private const val TAG = "WearStretchCooldown"
    private const val PREFS = "cooldown_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HANDLED_UNTIL = "handled_until_millis"
    private val mutex = Mutex()
    /** Walks are often just the trip home; never let one hide the gym session. */
    private val LOW_PRIORITY_TYPES = setOf(ExerciseSessionRecord.EXERCISE_TYPE_WALKING)
    const val CHANNEL_ID = "cooldown_channel"

    /** Only suggest for workouts that ended this recently. */
    private val WINDOW: Duration = Duration.ofHours(3)
    /** Ignore very short sessions (accidental starts, warm-up walks). */
    private val MIN_LENGTH: Duration = Duration.ofMinutes(10)

    fun isEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ENABLED, enabled) }
        if (enabled) CooldownWorker.schedule(context) else CooldownWorker.cancel(context)
    }

    /** Runs one check. Safe to call from the foreground or from the worker. */
    suspend fun check(context: Context) {
        if (!isEnabled(context)) return
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return
        val hc = HealthConnectManager(context)
        if (!hc.hasReadPermission()) return

        // The worker and the on-open check can run at the same moment; without
        // this both would notify.
        mutex.withLock { checkLocked(context, hc) }
    }

    private suspend fun checkLocked(context: Context, hc: HealthConnectManager) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Everything that ended at or before this was already handled. A
        // timestamp (not a set of ids) so it never grows and never forgets.
        val handledUntil = Instant.ofEpochMilli(prefs.getLong(KEY_HANDLED_UNTIL, 0L))
        val windowStart = Instant.now().minus(WINDOW)

        val fresh = try {
            hc.readOtherWorkoutsEndedSince(maxOf(windowStart, handledUntil.plusMillis(1)))
                .filter { Duration.between(it.startTime, it.endTime) >= MIN_LENGTH }
        } catch (e: Exception) {
            // Background reads throw without the background-read permission.
            Log.w(TAG, "Could not read workouts: ${e.message}")
            return
        }
        // Several workouts in one window (e.g. a gym session plus the walk home)
        // get ONE suggestion. The training session wins over a walk, even if the
        // walk ended later; otherwise the most recent one.
        val workout = fresh.filter { it.exerciseType !in LOW_PRIORITY_TYPES }.maxByOrNull { it.endTime }
            ?: fresh.maxByOrNull { it.endTime }
            ?: return

        // Mark everything seen as handled first, so a crash below never causes
        // repeat notifications and the skipped walk isn't suggested later.
        prefs.edit { putLong(KEY_HANDLED_UNTIL, fresh.maxOf { it.endTime }.toEpochMilli()) }

        if (hc.hasStretchedSince(workout.endTime)) {
            Log.d(TAG, "Already stretched after ${workout.title}; no suggestion")
            return
        }

        val suggestion = CooldownMatcher.suggest(
            RoutineRepository(context).loadRoutines(),
            workout.title,
            workout.exerciseType
        ) ?: return

        notify(context, workout.title, suggestion.routine.id, suggestion.routine.name)
    }

    private fun notify(context: Context, workoutTitle: String?, routineId: String, routineName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Cool-down suggestions", NotificationManager.IMPORTANCE_DEFAULT)
        )

        val what = workoutTitle?.takeIf { it.isNotBlank() } ?: "your workout"
        val notificationId = routineId.hashCode()
        val reason = "after $what"

        val startIntent = Intent(context, CooldownActionReceiver::class.java).apply {
            action = CooldownActionReceiver.ACTION_START_ON_WATCH
            putExtra(CooldownActionReceiver.EXTRA_ROUTINE_ID, routineId)
            putExtra(CooldownActionReceiver.EXTRA_REASON, reason)
            putExtra(CooldownActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val startPending = PendingIntent.getBroadcast(
            context, notificationId, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Nice work on $what. Cool down with \"$routineName\"?"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to stretch")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openPending)
            .addAction(0, "Start on watch", startPending)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }
}
