package com.db2k.wearstretch.presentation

import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch

/**
 * Seconds left in the whole routine, mirroring WorkoutService's phase order:
 * get ready → stretch (side 1 → switch → side 2) → break (uses the NEXT
 * stretch's break length) → next stretch …
 */
internal fun remainingRoutineSeconds(
    routine: Routine,
    index: Int,
    timeLeft: Int,
    isStarting: Boolean,
    isBreak: Boolean,
    isSwitchingSides: Boolean,
    isSplitSecondHalf: Boolean
): Int {
    val st = routine.stretches
    fun body(s: Stretch) = s.durationSeconds + if (s.isSplit) s.splitBreakDurationSeconds else 0
    fun rest(from: Int) = (from until st.size).sumOf { st[it].breakDurationSeconds + body(st[it]) }
    val current = st.getOrNull(index) ?: return timeLeft
    return timeLeft + when {
        isStarting -> body(current) + rest(index + 1)
        isBreak -> (st.getOrNull(index + 1)?.let { body(it) } ?: 0) + rest(index + 2)
        isSwitchingSides -> current.durationSeconds / 2 + rest(index + 1)
        current.isSplit && !isSplitSecondHalf ->
            current.splitBreakDurationSeconds + current.durationSeconds / 2 + rest(index + 1)
        else -> rest(index + 1)
    }
}

/** 1-based number of the stretch on screen; during a break that's the upcoming one. */
internal fun displayedStretchNumber(index: Int, isBreak: Boolean, count: Int) =
    (if (isBreak) index + 2 else index + 1).coerceIn(1, count.coerceAtLeast(1))

/** Lines for the finish screen, e.g. "6m 12s", "13 of 14 stretches", "Avg ♥ 82 · max 97", "4-day streak". */
internal fun finishSummary(
    routine: Routine?,
    startMillis: Long,
    endMillis: Long,
    skipped: Int,
    heartRates: List<Int>,
    streakDays: Int
): List<String> {
    val lines = mutableListOf<String>()
    if (startMillis > 0 && endMillis >= startMillis) lines += formatDuration((endMillis - startMillis) / 1000)
    routine?.let {
        val total = it.stretches.size
        val done = (total - skipped).coerceIn(0, total)
        lines += if (skipped == 0) "All $total stretches" else "$done of $total stretches"
    }
    if (heartRates.isNotEmpty()) lines += "Avg ♥ ${heartRates.average().toInt()} · max ${heartRates.max()}"
    if (streakDays > 0) lines += if (streakDays == 1) "1-day streak" else "$streakDays-day streak"
    return lines
}
