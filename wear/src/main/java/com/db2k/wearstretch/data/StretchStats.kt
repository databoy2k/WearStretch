package com.db2k.wearstretch.data

import com.db2k.wearstretch.model.WorkoutRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Streak and today's totals from the watch's own workout history. */
data class StretchStats(
    val streakDays: Int,
    val minutesToday: Int,
    val lastRoutineName: String?
) {
    companion object {
        fun from(
            history: List<WorkoutRecord>,
            today: LocalDate = LocalDate.now(),
            zone: ZoneId = ZoneId.systemDefault()
        ): StretchStats {
            fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

            val days = history.map { dayOf(it.startTimeMillis) }.toSet()
            // A streak survives until the end of today: count back from today if
            // stretched today, otherwise from yesterday.
            var day = if (today in days) today else today.minusDays(1)
            var streak = 0
            while (day in days) {
                streak++
                day = day.minusDays(1)
            }

            val minutesToday = history
                .filter { dayOf(it.startTimeMillis) == today }
                .sumOf { (it.endTimeMillis - it.startTimeMillis).coerceAtLeast(0) } / 60_000

            val last = history.maxByOrNull { it.startTimeMillis }?.routineName
            return StretchStats(streak, minutesToday.toInt(), last)
        }
    }
}
