package com.db2k.wearstretch

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.db2k.wearstretch.cooldown.CooldownMatcher
import com.db2k.wearstretch.data.PresetData
import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.ui.routine.mergeBuiltInRoutines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetRoutinesTest {
    private val strength = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    private val presets = PresetData.presetRoutines

    private fun seconds(r: Routine) = r.stretches.sumOf {
        it.durationSeconds + it.breakDurationSeconds + if (it.isSplit) it.splitBreakDurationSeconds else 0
    }

    @Test fun staticHoldsAreAtLeast20sPerSide() {
        // Dynamic moves (rolls, circles, fan-outs, chin tucks) are allowed shorter.
        val dynamic = setOf("2", "25", "28", "12", "54")
        PresetData.presetStretches.filter { it.id !in dynamic }.forEach {
            val perSide = if (it.isSplit) it.durationSeconds / 2 else it.durationSeconds
            assertTrue("${it.name} holds only ${perSide}s", perSide >= 20)
        }
    }

    @Test fun deskResetIsAboutFiveMinutes() {
        val s = seconds(presets.first { it.id == "preset_desk_worker" })
        assertTrue("desk reset is ${s}s", s in 240..390)
    }

    @Test fun preBedEndsLyingDown() {
        val last = presets.first { it.id == "preset_pre_bed" }.stretches.last()
        assertEquals("Knees-to-Chest", last.name)
        assertTrue(presets.first { it.id == "preset_pre_bed" }.stretches.none { it.name == "Cobra Stretch" })
    }

    @Test fun cooldownsMatchTrainingDays() {
        assertEquals("Upper-Body Cool-Down", CooldownMatcher.suggest(presets, "Push day", strength)!!.routine.name)
        assertEquals("Lower-Body Cool-Down", CooldownMatcher.suggest(presets, "Leg day", strength)!!.routine.name)
        assertEquals("Lower-Body Cool-Down",
            CooldownMatcher.suggest(presets, null, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING)!!.routine.name)
    }

    @Test fun updateBuiltInsKeepsCustomRoutines() {
        val mine = Routine("custom_1", "My thing", presets[0].stretches.take(3))
        val oldMorning = presets[0].copy(name = "Old morning", stretches = presets[0].stretches.take(1))
        val merged = mergeBuiltInRoutines(listOf(mine, oldMorning), presets)
        assertEquals("custom routine kept first", mine, merged[0])
        assertEquals("built-in replaced in place", presets[0], merged[1])
        assertEquals("new built-ins appended", presets.size + 1, merged.size)
    }
}
