package com.trazo.app.data

import android.content.Context
import androidx.core.content.edit
import com.trazo.app.model.Habit
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitUnit
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import com.trazo.app.model.TrazoState
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import com.trazo.app.widget.TrazoWidget
import com.trazo.app.notifications.ItemReminderScheduler

/** Small, versioned local store. No network or account is involved. */
class LocalStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("trazo_local_store", Context.MODE_PRIVATE)

    fun load(): TrazoState = runCatching {
        val raw = preferences.getString(KEY_STATE, null) ?: return TrazoState()
        decode(JSONObject(raw))
    }.getOrDefault(TrazoState())

    fun save(state: TrazoState) {
        preferences.edit { putString(KEY_STATE, encode(state).toString()) }
        ItemReminderScheduler.scheduleAll(context, state)
        TrazoWidget.updateAll(context)
    }

    /** Creates a portable, human-readable backup without touching the current data. */
    fun exportJson(): String = encode(load()).toString(2)

    /** Replaces local data only after the whole backup has been parsed successfully. */
    fun importJson(raw: String): Result<TrazoState> = runCatching {
        val root = JSONObject(raw)
        require(root.has("tasks") && root.has("habits")) { "La copia no pertenece a Trazo" }
        val restored = decode(root)
        save(restored)
        restored
    }

    private fun encode(state: TrazoState) = JSONObject().apply {
        put("version", 4)
        put("tasks", JSONArray().apply {
            state.tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("note", task.note)
                    put("priority", task.priority.name)
                    put("completed", task.completed)
                    task.completedAt?.let { put("completedAt", it) }
                    put("createdOn", task.createdOn.toString())
                    task.dueDate?.let { put("dueDate", it.toString()) }
                    task.reminderHour?.let { put("reminderHour", it) }
                    put("reminderMinute", task.reminderMinute)
                    put("tags", JSONArray(task.tags.toList()))
                    put("archived", task.archived)
                    task.deletedAt?.let { put("deletedAt", it) }
                })
            }
        })
        put("habits", JSONArray().apply {
            state.habits.forEach { habit ->
                put(JSONObject().apply {
                    put("id", habit.id)
                    put("title", habit.title)
                    put("emoji", habit.emoji)
                    put("category", habit.category.name)
                    put("activeDays", JSONArray(habit.activeDays.map { it.value }))
                    put("completions", JSONArray(habit.completions.map { it.toString() }))
                    put("progress", JSONObject().apply {
                        habit.progress.forEach { (date, amount) -> put(date.toString(), amount) }
                    })
                    put("target", habit.target)
                    put("unit", habit.unit.name)
                    habit.reminderHour?.let { put("reminderHour", it) }
                    put("reminderMinute", habit.reminderMinute)
                    put("tags", JSONArray(habit.tags.toList()))
                    put("archived", habit.archived)
                    habit.deletedAt?.let { put("deletedAt", it) }
                    put("createdOn", habit.createdOn.toString())
                })
            }
        })
    }

    private fun decode(root: JSONObject): TrazoState {
        val tasksJson = root.optJSONArray("tasks") ?: JSONArray()
        val tasks = buildList {
            for (index in 0 until tasksJson.length()) {
                val item = tasksJson.getJSONObject(index)
                add(Task(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    note = item.optString("note"),
                    priority = runCatching { TaskPriority.valueOf(item.optString("priority")) }
                        .getOrDefault(TaskPriority.CALM),
                    completed = item.optBoolean("completed"),
                    completedAt = item.optLong("completedAt", -1L).takeIf { it >= 0L },
                    createdOn = LocalDate.parse(item.getString("createdOn")),
                    dueDate = item.optString("dueDate").takeIf { it.isNotBlank() }
                        ?.let(LocalDate::parse),
                    reminderHour = item.optInt("reminderHour", -1).takeIf { it >= 0 },
                    reminderMinute = item.optInt("reminderMinute", 0),
                    tags = item.optJSONArray("tags").toStringSet(),
                    archived = item.optBoolean("archived"),
                    deletedAt = item.optLong("deletedAt", -1L).takeIf { it >= 0L }
                ))
            }
        }
        val habitsJson = root.optJSONArray("habits") ?: JSONArray()
        val habits = buildList {
            for (index in 0 until habitsJson.length()) {
                val item = habitsJson.getJSONObject(index)
                val daysJson = item.optJSONArray("activeDays") ?: JSONArray()
                val days = buildSet {
                    for (dayIndex in 0 until daysJson.length()) add(DayOfWeek.of(daysJson.getInt(dayIndex)))
                }
                val completionJson = item.optJSONArray("completions") ?: JSONArray()
                val completions = buildSet {
                    for (dateIndex in 0 until completionJson.length()) add(LocalDate.parse(completionJson.getString(dateIndex)))
                }
                val progressJson = item.optJSONObject("progress")
                val progress = buildMap {
                    progressJson?.keys()?.forEach { date ->
                        put(LocalDate.parse(date), progressJson.getInt(date))
                    }
                }
                add(Habit(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    emoji = item.optString("emoji", "✦"),
                    category = runCatching {
                        HabitCategory.valueOf(item.optString("category"))
                    }.getOrElse { HabitCategory.infer(item.getString("title")) },
                    activeDays = days,
                    completions = completions,
                    progress = progress,
                    target = item.optInt("target", 1).coerceAtLeast(1),
                    unit = runCatching { HabitUnit.valueOf(item.optString("unit")) }
                        .getOrDefault(HabitUnit.CHECK),
                    reminderHour = item.optInt("reminderHour", -1).takeIf { it >= 0 },
                    reminderMinute = item.optInt("reminderMinute", 0),
                    tags = item.optJSONArray("tags").toStringSet(),
                    archived = item.optBoolean("archived"),
                    deletedAt = item.optLong("deletedAt", -1L).takeIf { it >= 0L },
                    createdOn = LocalDate.parse(item.getString("createdOn"))
                ))
            }
        }
        return TrazoState(tasks, habits)
    }

    private companion object { const val KEY_STATE = "state_v1" }

    private fun JSONArray?.toStringSet(): Set<String> = buildSet {
        if (this@toStringSet != null) {
            for (index in 0 until length()) add(getString(index))
        }
    }
}
