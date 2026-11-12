package com.db2k.wearstretch.presentation

import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMathTest {
    private val plain = Stretch("p", "Plain", "", durationSeconds = 30, breakDurationSeconds = 5)
    private val split = Stretch("s", "Split", "", durationSeconds = 40, breakDurationSeconds = 8, isSplit = true, splitBreakDurationSeconds = 4)
    private val routine = Routine("r", "R", listOf(plain, split, plain))

    @Test fun remainingAtStart() {
        // ready 10 + plain 30 + (8 + 40 + 4) + (5 + 30) = 127
        assertEquals(127, remainingRoutineSeconds(routine, 0, 10, true, false, false, false))
    }

    @Test fun remainingMidSplitFirstSide() {
        // 7 left of side 1 + switch 4 + side 2 20 + (5 + 30) = 66
        assertEquals(66, remainingRoutineSeconds(routine, 1, 7, false, false, false, false))
    }

    @Test fun remainingDuringBreak() {
        // break before split: 3 + split body 44 + (5 + 30) = 82
        assertEquals(82, remainingRoutineSeconds(routine, 0, 3, false, true, false, false))
    }

    @Test fun remainingLastStretch() {
        assertEquals(12, remainingRoutineSeconds(routine, 2, 12, false, false, false, false))
    }

    @Test fun numberShowsUpcomingDuringBreak() {
        assertEquals(1, displayedStretchNumber(0, false, 3))
        assertEquals(2, displayedStretchNumber(0, true, 3))
        assertEquals(3, displayedStretchNumber(2, true, 3))
    }

    @Test fun summary() {
        val lines = finishSummary(routine, 0L + 1, 372_001L, 1, listOf(80, 90), 4)
        assertEquals(listOf("6m 12s", "2 of 3 stretches", "Avg ♥ 85 · max 90", "4-day streak"), lines)
        assertTrue(finishSummary(routine, 1, 2, 0, emptyList(), 0).contains("All 3 stretches"))
    }

    @Test fun clock() {
        assertEquals("4:05", clockLabel(245))
        assertEquals("0:00", clockLabel(-3))
    }
}
