package com.db2k.wearstretch

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.db2k.wearstretch.health.HealthConnectManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

/**
 * Runs against the real Health Connect on a device/emulator. Needs the app's
 * Health Connect permissions granted first, e.g.:
 *   adb shell pm grant com.db2k.wearstretch android.permission.health.READ_EXERCISE (and WRITE_*, *_HEART_RATE, *_STEPS)
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val hc = HealthConnectManager(context)
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    @Test
    fun writtenStretchShowsInMeTabAndDeleteRemovesHrAndSteps() = runBlocking {
        assumeTrue(HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE)
        assumeTrue(hc.hasAllPermissions())

        val id = "test-" + System.currentTimeMillis()
        val end = ZonedDateTime.now().minusMinutes(1)
        val start = end.minusMinutes(5)
        hc.writeWorkout(id, start, end, "Instrumented stretch", listOf(80, 82, 85), stepCount = 12)

        val mine = hc.readRecentStretchingWorkouts().firstOrNull { it.metadata.clientRecordId == id }
        assertTrue("written stretch should appear in the Me tab list", mine != null)
        assertTrue(hc.hasStretchedSince(start.toInstant().minusSeconds(1)))

        hc.deleteWorkout(mine!!)

        val range = TimeRangeFilter.between(start.toInstant(), end.toInstant())
        val hr = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, range)).records
            .filter { it.metadata.clientRecordId == "${id}_hr" }
        val steps = client.readRecords(ReadRecordsRequest(StepsRecord::class, range)).records
            .filter { it.metadata.clientRecordId == "${id}_steps" }
        assertEquals("heart rate should be deleted with the stretch", 0, hr.size)
        assertEquals("steps should be deleted with the stretch", 0, steps.size)
        assertTrue(hc.readRecentStretchingWorkouts().none { it.metadata.clientRecordId == id })
    }
}
