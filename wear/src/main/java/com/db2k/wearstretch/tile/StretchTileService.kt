package com.db2k.wearstretch.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.db2k.wearstretch.data.RoutineStore
import com.db2k.wearstretch.data.StretchStats
import com.db2k.wearstretch.data.WorkoutRepository
import com.db2k.wearstretch.presentation.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Tile: stretch streak, minutes today, and one button that opens the app with
 * a routine pinned at the top (a fresh cool-down suggestion if there is one,
 * otherwise the last routine done).
 */
class StretchTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val stats = StretchStats.from(WorkoutRepository(this).getHistory())
        val suggestion = RoutineStore.currentSuggestion()
        val suggestedName = suggestion?.let { s ->
            s.routineName ?: RoutineStore.receivedRoutines.firstOrNull { s.matches(it) }?.name
        }

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(stats, suggestedName)))
            .build()
        return Futures.immediateFuture(tile)
    }

    @Deprecated("Required by TileService on this tiles version")
    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())

    private fun layout(stats: StretchStats, suggestedName: String?): LayoutElementBuilders.LayoutElement {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text("Wear Stretch", 13f, GRAY))
            .addContent(spacer(4f))
            .addContent(text(streakLabel(stats.streakDays), 22f, ACCENT, bold = true))
            .addContent(text("${stats.minutesToday} min today", 14f, WHITE))
            .addContent(spacer(10f))

        val (label, routineName) = when {
            suggestedName != null -> "Cool-down: $suggestedName" to suggestedName
            stats.lastRoutineName != null -> "Again: ${stats.lastRoutineName}" to stats.lastRoutineName
            else -> "Pick a routine" to null
        }
        column.addContent(button(label, routineName))

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setPadding(ModifiersBuilders.Padding.Builder().setStart(dp(20f)).setEnd(dp(20f)).build())
                    .build()
            )
            .addContent(column.build())
            .build()
    }

    private fun button(label: String, routineName: String?): LayoutElementBuilders.LayoutElement {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName(MainActivity::class.java.name)
        if (routineName != null) {
            activity.addKeyToExtraMapping(
                MainActivity.EXTRA_PIN_ROUTINE_NAME,
                ActionBuilders.AndroidStringExtra.Builder().setValue(routineName).build()
            )
        }
        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build())
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(BUTTON))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(24f)).build())
                            .build()
                    )
                    .setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(10f)).build())
                    .build()
            )
            .addContent(text(label, 14f, WHITE, bold = true, maxLines = 2))
            .build()
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        maxLines: Int = 1
    ): LayoutElementBuilders.LayoutElement {
        val style = LayoutElementBuilders.FontStyle.Builder()
            .setSize(sp(size))
            .setColor(argb(color))
        if (bold) style.setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
        return LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(maxLines)
            .setFontStyle(style.build())
            .build()
    }

    private fun spacer(height: Float) =
        LayoutElementBuilders.Spacer.Builder().setHeight(dp(height)).build()

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val GRAY = 0xFFBDBDBD.toInt()
        private const val ACCENT = 0xFFFFC857.toInt()
        private const val BUTTON = 0xFF2E5E8C.toInt()

        fun streakLabel(days: Int) = when (days) {
            0 -> "No streak yet"
            1 -> "1-day streak"
            else -> "$days-day streak"
        }

        /** Ask the system to redraw the tile, e.g. after a workout finishes. */
        fun requestUpdate(context: Context) {
            getUpdater(context).requestUpdate(StretchTileService::class.java)
        }
    }
}
