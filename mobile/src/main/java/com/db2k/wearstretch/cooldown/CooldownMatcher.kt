package com.db2k.wearstretch.cooldown

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.db2k.wearstretch.model.Routine

/**
 * Picks the stretching routine that best fits a workout another app logged
 * (e.g. an openGym "Leg day" strength session).
 *
 * Health Connect only gives us the workout's title and exercise type, not the
 * muscles trained, so the match is keyword based. Pure Kotlin so it can be unit
 * tested without Android.
 */
object CooldownMatcher {

    const val THIGHS = "Thighs (Quads & Hamstrings)"
    const val HIPS = "Hips & Glutes"
    const val CALVES = "Calves & Feet"
    const val LOWER_BACK = "Lower Back & Core"
    const val CHEST = "Chest & Upper Back"
    const val NECK = "Neck & Shoulders"
    const val ARMS = "Arms & Wrists"

    private val LOWER = setOf(THIGHS, HIPS, CALVES, LOWER_BACK)
    private val UPPER = setOf(CHEST, NECK, ARMS)

    /** Keyword → body areas. Checked as whole words / word prefixes in the title. */
    private val KEYWORDS: List<Pair<List<String>, Set<String>>> = listOf(
        listOf("leg", "lower", "squat", "lunge", "quad", "hamstring", "calf", "calves",
            "glute", "hip", "run", "jog", "walk", "hike", "bike", "cycl", "spin",
            "stair", "step") to LOWER,
        listOf("deadlift", "pull", "back", "row", "lat") to setOf(CHEST, LOWER_BACK, ARMS, HIPS),
        listOf("push", "chest", "bench", "press", "shoulder", "upper", "arm",
            "tricep", "bicep", "curl", "dip") to UPPER,
        listOf("core", "abs", "ab ", "plank") to setOf(LOWER_BACK, HIPS),
        listOf("full", "total", "whole", "body", "crossfit", "hiit", "circuit") to (LOWER + UPPER),
    )

    /** Cardio exercise types (Health Connect constants) that work mostly the legs. */
    private val LEG_TYPES = setOf(
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
    )
    private const val ROWING_MACHINE = ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE

    data class Suggestion(val routine: Routine, val areas: Set<String>)

    /** Body areas the workout most likely trained. Never empty: falls back to full body. */
    fun areasFor(title: String?, exerciseType: Int): Set<String> {
        val text = " " + (title ?: "").lowercase() + " "
        val areas = mutableSetOf<String>()
        for ((words, hit) in KEYWORDS) {
            if (words.any { w -> Regex("[^a-z]" + Regex.escape(w.trim())).containsMatchIn(text) }) {
                areas += hit
            }
        }
        if (areas.isEmpty()) {
            when (exerciseType) {
                in LEG_TYPES -> areas += LOWER
                ROWING_MACHINE -> areas += setOf(LOWER_BACK, CHEST, ARMS, THIGHS)
            }
        }
        return areas.ifEmpty { LOWER + UPPER }
    }

    /**
     * Best routine for [areas]: the one whose stretches mostly target those areas.
     * Ties go to routines named like a post-workout/cool-down routine, then to
     * the longer routine. Returns null only when there are no usable routines.
     */
    fun pick(routines: List<Routine>, areas: Set<String>): Routine? {
        val usable = routines.filter { it.stretches.size >= 3 }
        if (usable.isEmpty()) return null
        return usable.maxWithOrNull(
            compareBy<Routine> { score(it, areas) }
                .thenBy { if (looksLikeCooldown(it.name)) 1 else 0 }
                .thenBy { it.stretches.size }
        )
    }

    fun suggest(routines: List<Routine>, title: String?, exerciseType: Int): Suggestion? {
        val areas = areasFor(title, exerciseType)
        val routine = pick(routines, areas) ?: return null
        return Suggestion(routine, areas)
    }

    /**
     * Fraction of the routine's stretches that hit [areas], times how many of
     * [areas] it covers. Rewards routines that are both on-target and broad.
     */
    internal fun score(routine: Routine, areas: Set<String>): Double {
        if (routine.stretches.isEmpty()) return 0.0
        val onTarget = routine.stretches.count { it.category in areas }.toDouble() / routine.stretches.size
        val covered = routine.stretches.map { it.category }.toSet().intersect(areas).size.toDouble() / areas.size
        return onTarget * covered
    }

    private fun looksLikeCooldown(name: String): Boolean {
        val n = name.lowercase()
        return "post" in n || "cool" in n || "after" in n || "recovery" in n
    }
}
