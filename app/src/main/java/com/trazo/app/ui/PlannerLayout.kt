package com.trazo.app.ui

import java.time.LocalDate
import kotlin.math.roundToInt

internal const val PLANNER_SNAP_MINUTES = 15

internal data class PlannerDrop(
    val date: LocalDate,
    val hour: Int,
    val minute: Int,
    val dayDelta: Int,
    val minuteDelta: Int
)

/** Pure planner geometry shared by the gesture and unit tests. */
internal fun plannerDrop(
    originalDate: LocalDate,
    originalHour: Int,
    originalMinute: Int,
    horizontalPixels: Float,
    verticalPixels: Float,
    pixelsPerDay: Float,
    pixelsPerMinute: Float
): PlannerDrop {
    val dayDelta = if (pixelsPerDay > 0f) {
        (horizontalPixels / pixelsPerDay).roundToInt().coerceIn(-7, 7)
    } else 0
    val rawMinuteDelta = if (pixelsPerMinute > 0f) {
        (verticalPixels / pixelsPerMinute).roundToInt()
    } else 0
    val originalTotal = originalHour.coerceIn(0, 23) * 60 + originalMinute.coerceIn(0, 59)
    val snappedTotal = (((originalTotal + rawMinuteDelta).toFloat() / PLANNER_SNAP_MINUTES).roundToInt() * PLANNER_SNAP_MINUTES)
        .coerceIn(0, 23 * 60 + 45)
    return PlannerDrop(
        date = originalDate.plusDays(dayDelta.toLong()),
        hour = snappedTotal / 60,
        minute = snappedTotal % 60,
        dayDelta = dayDelta,
        minuteDelta = snappedTotal - originalTotal
    )
}

/** Duration is visible while short/very long blocks remain usable on a phone. */
internal fun plannerBlockHeightDp(durationMinutes: Int): Int =
    (72f + durationMinutes.coerceIn(5, 180) * 1.2f).roundToInt().coerceIn(92, 288)
