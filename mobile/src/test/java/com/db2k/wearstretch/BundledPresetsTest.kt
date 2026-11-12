package com.db2k.wearstretch

import com.db2k.wearstretch.data.PresetData
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The watch ships a copy of the preset routines (wear/src/main/assets) so it
 * works without the phone. This fails when PresetData changes and the copy
 * wasn't refreshed. To refresh: set UPDATE_BUNDLED_PRESETS=1 and run :mobile:testDebugUnitTest.
 */
class BundledPresetsTest {
    @Test
    fun watchPresetsMatchPhonePresets() {
        val asset = File("../wear/src/main/assets/preset_routines.json")
        val expected = Gson().toJson(PresetData.presetRoutines)
        if (System.getenv("UPDATE_BUNDLED_PRESETS") == "1") asset.writeText(expected)
        assertEquals(
            "Watch presets are stale: rerun with env UPDATE_BUNDLED_PRESETS=1",
            JsonParser.parseString(expected),
            JsonParser.parseString(asset.readText())
        )
    }
}
