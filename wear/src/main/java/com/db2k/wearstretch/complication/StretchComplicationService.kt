package com.db2k.wearstretch.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.db2k.wearstretch.data.StretchStats
import com.db2k.wearstretch.data.WorkoutRepository
import com.db2k.wearstretch.presentation.MainActivity

/** Watch-face complication: stretch streak in days. Tap opens the app. */
class StretchComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        if (type == ComplicationType.SHORT_TEXT) build(streak = 5, minutesToday = 12, tap = null) else null

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null
        val stats = StretchStats.from(WorkoutRepository(this).getHistory())
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return build(stats.streakDays, stats.minutesToday, tap)
    }

    private fun build(streak: Int, minutesToday: Int, tap: PendingIntent?): ComplicationData {
        val description = "Stretch streak $streak days, $minutesToday minutes today"
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("${streak}d").build(),
            contentDescription = PlainComplicationText.Builder(description).build()
        )
            .setTitle(PlainComplicationText.Builder("Stretch").build())
            .apply { if (tap != null) setTapAction(tap) }
            .build()
    }

    companion object {
        fun requestUpdate(context: Context) {
            ComplicationDataSourceUpdateRequester.create(
                context, ComponentName(context, StretchComplicationService::class.java)
            ).requestUpdateAll()
        }
    }
}
