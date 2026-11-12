package com.db2k.wearstretch

import com.db2k.wearstretch.data.RoutineSuggestion
import com.db2k.wearstretch.data.StretchStats
import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.WorkoutRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StretchStatsTest {
    private val zone = ZoneId.of("America/Edmonton")
    private val today = LocalDate.of(2026, 11, 12)

    private fun rec(day: LocalDate, hour: Int, minutes: Int, name: String = "Morning") : WorkoutRecord {
        val start = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return WorkoutRecord(id = "$day$hour", routineName = name, startTimeMillis = start, endTimeMillis = start + minutes * 60_000L)
    }

    @Test fun emptyHistory() {
        val s = StretchStats.from(emptyList(), today, zone)
        assertEquals(0, s.streakDays); assertEquals(0, s.minutesToday); assertNull(s.lastRoutineName)
    }

    @Test fun streakCountsTodayAndPreviousDays() {
        val h = listOf(rec(today, 8, 10), rec(today.minusDays(1), 8, 5), rec(today.minusDays(2), 8, 5), rec(today.minusDays(4), 8, 5))
        assertEquals(3, StretchStats.from(h, today, zone).streakDays)
    }

    @Test fun streakSurvivesUntilEndOfDay() {
        // Not stretched yet today: yesterday's streak still counts.
        val h = listOf(rec(today.minusDays(1), 8, 5), rec(today.minusDays(2), 8, 5))
        assertEquals(2, StretchStats.from(h, today, zone).streakDays)
    }

    @Test fun streakBrokenAfterMissedDay() {
        val h = listOf(rec(today.minusDays(2), 8, 5))
        assertEquals(0, StretchStats.from(h, today, zone).streakDays)
    }

    @Test fun minutesTodayAndLastRoutine() {
        val h = listOf(rec(today, 7, 10, "Morning"), rec(today, 19, 12, "Pre-Bed"), rec(today.minusDays(1), 8, 30))
        val s = StretchStats.from(h, today, zone)
        assertEquals(22, s.minutesToday)
        assertEquals("Pre-Bed", s.lastRoutineName)
    }

    @Test fun suggestionMatchesByIdOrName() {
        val r = Routine(id = "preset_post_workout", name = "Full-Body Post-Workout", stretches = emptyList())
        assertTrue(RoutineSuggestion("preset_post_workout", "", 0).matches(r))
        assertTrue(RoutineSuggestion("", "", 0, routineName = "Full-Body Post-Workout").matches(r))
        assertFalse(RoutineSuggestion("other", "", 0).matches(r))
    }
}
