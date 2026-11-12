package com.db2k.wearstretch.presentation

import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun duration() {
        assertEquals("9s", formatDuration(9))
        assertEquals("6m 12s", formatDuration(372))
        assertEquals("0s", formatDuration(-5))
    }

    @Test fun routineMinutes() {
        val st = Stretch("1", "A", "", durationSeconds = 40, breakDurationSeconds = 8, isSplit = true, splitBreakDurationSeconds = 5)
        // 4 x (40 + 8 + 5) = 212 s -> ~4 min
        assertEquals("~4 min", routineMinutesLabel(Routine("r", "R", List(4) { st })))
        assertEquals("~1 min", routineMinutesLabel(Routine("r", "R", emptyList())))
    }
}
