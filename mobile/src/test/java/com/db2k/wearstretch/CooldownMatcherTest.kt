package com.db2k.wearstretch

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.db2k.wearstretch.cooldown.CooldownMatcher
import com.db2k.wearstretch.data.PresetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CooldownMatcherTest {
    private val strength = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING

    @Test
    fun legDayMapsToLowerBody() {
        val areas = CooldownMatcher.areasFor("Leg Day", strength)
        assertTrue(CooldownMatcher.THIGHS in areas)
        assertTrue(CooldownMatcher.HIPS in areas)
        assertTrue(CooldownMatcher.ARMS !in areas)
    }

    @Test
    fun pushDayMapsToUpperBody() {
        val areas = CooldownMatcher.areasFor("Push A", strength)
        assertEquals(setOf(CooldownMatcher.CHEST, CooldownMatcher.NECK, CooldownMatcher.ARMS), areas)
    }

    @Test
    fun unknownTitleFallsBackToFullBody() {
        assertEquals(7, CooldownMatcher.areasFor("Workout 12", strength).size)
        assertEquals(7, CooldownMatcher.areasFor(null, strength).size)
    }

    @Test
    fun runningWithoutTitleIsLegs() {
        val areas = CooldownMatcher.areasFor(null, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING)
        assertTrue(CooldownMatcher.CALVES in areas)
        assertTrue(CooldownMatcher.CHEST !in areas)
    }

    @Test
    fun keywordNeedsWordStart() {
        // Keywords only match at the start of a word: "lat" must not match "plate".
        val areas = CooldownMatcher.areasFor("Plate work", strength)
        assertEquals(7, areas.size)
    }

    @Test
    fun picksARoutineFromPresets() {
        val s = CooldownMatcher.suggest(PresetData.presetRoutines, "Full body", strength)
        assertNotNull(s)
    }
}
