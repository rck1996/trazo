package com.trazo.app.ui

import com.trazo.app.model.Task
import java.time.LocalDate

internal data class FreeWindow(val startMinute: Int, val endMinute: Int) {
    val durationMinutes: Int get() = endMinute - startMinute
}

internal object CalendarInsights {
    fun workloadMinutes(tasks: List<Task>, date: LocalDate): Int = tasks.asSequence()
        .filter { !it.completed && it.dueDate == date }
        .sumOf { it.durationMinutes.coerceAtLeast(0) }

    fun plannedMinutes(tasks: List<Task>, date: LocalDate): Int = tasks.asSequence()
        .filter { !it.completed && it.dueDate == date && it.reminderHour != null }
        .sumOf { it.durationMinutes.coerceAtLeast(0) }

    fun conflictCount(tasks: List<Task>, date: LocalDate): Int {
        val intervals = intervals(tasks, date)
        return intervals.indices.sumOf { left ->
            (left + 1 until intervals.size).count { right ->
                intervals[left].first < intervals[right].second && intervals[right].first < intervals[left].second
            }
        }
    }

    fun firstConflictStart(tasks: List<Task>, date: LocalDate): Int? {
        val intervals = taskIntervals(tasks, date)
        return intervals.indices.firstNotNullOfOrNull { left ->
            (left + 1 until intervals.size).firstOrNull { right ->
                intervals[left].second.first < intervals[right].second.second &&
                    intervals[right].second.first < intervals[left].second.second
            }?.let { minOf(intervals[left].second.first, intervals[it].second.first) }
        }
    }

    fun conflictingTaskIds(tasks: List<Task>, date: LocalDate): Set<String> {
        val intervals = taskIntervals(tasks, date)
        return buildSet {
            intervals.indices.forEach { left ->
                (left + 1 until intervals.size).forEach { right ->
                    if (intervals[left].second.first < intervals[right].second.second &&
                        intervals[right].second.first < intervals[left].second.second
                    ) {
                        add(intervals[left].first)
                        add(intervals[right].first)
                    }
                }
            }
        }
    }

    fun freeWindows(tasks: List<Task>, date: LocalDate, startHour: Int = 8, endHour: Int = 20): List<FreeWindow> {
        val start = startHour * 60
        val end = endHour * 60
        val busy = intervals(tasks, date).map { maxOf(start, it.first) to minOf(end, it.second) }
            .filter { it.first < it.second }.sortedBy { it.first }
        val merged = mutableListOf<Pair<Int, Int>>()
        busy.forEach { interval ->
            val last = merged.lastOrNull()
            if (last == null || interval.first > last.second) merged += interval
            else merged[merged.lastIndex] = last.first to maxOf(last.second, interval.second)
        }
        var cursor = start
        return buildList {
            merged.forEach { interval ->
                if (interval.first > cursor) add(FreeWindow(cursor, interval.first))
                cursor = maxOf(cursor, interval.second)
            }
            if (cursor < end) add(FreeWindow(cursor, end))
        }
    }

    private fun intervals(tasks: List<Task>, date: LocalDate): List<Pair<Int, Int>> = tasks.mapNotNull { task ->
        val hour = task.reminderHour
        if (task.completed || task.dueDate != date || hour == null) null
        else {
            val start = hour * 60 + task.reminderMinute
            start to (start + task.durationMinutes.coerceAtLeast(5))
        }
    }.sortedBy { it.first }

    private fun taskIntervals(tasks: List<Task>, date: LocalDate): List<Pair<String, Pair<Int, Int>>> = tasks.mapNotNull { task ->
        val hour = task.reminderHour
        if (task.completed || task.dueDate != date || hour == null) null
        else {
            val start = hour * 60 + task.reminderMinute
            task.id to (start to (start + task.durationMinutes.coerceAtLeast(5)))
        }
    }.sortedBy { it.second.first }
}
