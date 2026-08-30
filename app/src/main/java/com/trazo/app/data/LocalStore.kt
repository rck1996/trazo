package com.trazo.app.data

import android.content.Context
import androidx.core.content.edit
import com.trazo.app.model.Habit
import com.trazo.app.model.CategoryCatalog
import com.trazo.app.model.CategoryDefinition
import com.trazo.app.model.HabitCategory
import com.trazo.app.model.HabitUnit
import com.trazo.app.model.ItemReminderMode
import com.trazo.app.model.Task
import com.trazo.app.model.TaskPriority
import com.trazo.app.model.TaskRecurrence
import com.trazo.app.model.TaskSubtask
import com.trazo.app.model.TaskTemplate
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
        val previous = load()
        preferences.edit { putString(KEY_STATE, encode(state).toString()) }
        ItemReminderScheduler.syncAfterSave(context, previous, state)
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
        put("version", 7)
        put("categories", JSONArray(state.categories.map { category -> JSONObject().apply {
            put("id", category.id); put("name", category.name); put("symbol", category.symbol); put("colorArgb", category.colorArgb)
        } }))
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
                    put("durationMinutes", task.durationMinutes.coerceIn(5, 480))
                    put("recurrence", task.recurrence.name)
                    put("categoryId", task.categoryId)
                    put("subtasks", JSONArray(task.subtasks.map { JSONObject().apply { put("id", it.id); put("title", it.title); put("completed", it.completed); it.dependsOnId?.let { dependency -> put("dependsOnId", dependency) } } }))
                    task.reminderHour?.let { put("reminderHour", it) }
                    put("reminderMinute", task.reminderMinute)
                    task.reminderMode?.let { put("reminderMode", it.name) }
                    put("criticalAlarm", task.criticalAlarm)
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
                    put("categoryId", habit.categoryId)
                    put("activeDays", JSONArray(habit.activeDays.map { it.value }))
                    put("repeatEveryWeeks", habit.repeatEveryWeeks)
                    put("skippedDates", JSONArray(habit.skippedDates.map { it.toString() }))
                    put("completions", JSONArray(habit.completions.map { it.toString() }))
                    put("progress", JSONObject().apply {
                        habit.progress.forEach { (date, amount) -> put(date.toString(), amount) }
                    })
                    put("target", habit.target)
                    put("unit", habit.unit.name)
                    habit.reminderHour?.let { put("reminderHour", it) }
                    put("reminderMinute", habit.reminderMinute)
                    habit.reminderMode?.let { put("reminderMode", it.name) }
                    put("criticalAlarm", habit.criticalAlarm)
                    put("tags", JSONArray(habit.tags.toList()))
                    put("archived", habit.archived)
                    habit.deletedAt?.let { put("deletedAt", it) }
                    put("createdOn", habit.createdOn.toString())
                })
            }
        })
        put("taskTemplates", JSONArray(state.taskTemplates.map { template -> JSONObject().apply {
            put("id", template.id); put("name", template.name); put("title", template.title); put("note", template.note)
            put("priority", template.priority.name); put("durationMinutes", template.durationMinutes)
            put("recurrence", template.recurrence.name); put("categoryId", template.categoryId)
            put("subtasks", JSONArray(template.subtasks.map { sub -> JSONObject().apply { put("id", sub.id); put("title", sub.title); put("completed", sub.completed); sub.dependsOnId?.let { dependency -> put("dependsOnId", dependency) } } }))
            template.reminderHour?.let { put("reminderHour", it) }; put("reminderMinute", template.reminderMinute)
            template.reminderMode?.let { put("reminderMode", it.name) }; put("tags", JSONArray(template.tags.toList()))
        } }))
    }

    private fun decode(root: JSONObject): TrazoState {
        val categories = buildList {
            val array = root.optJSONArray("categories") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.optString("name").trim()
                if (name.isNotEmpty()) add(CategoryDefinition(
                    id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    name = name,
                    symbol = item.optString("symbol", "✦").ifBlank { "✦" }.take(2),
                    colorArgb = item.optLong("colorArgb", 0xFF5B7F67)
                ))
            }
        }.ifEmpty { CategoryCatalog.defaults }
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
                    durationMinutes = item.optInt("durationMinutes", 25).coerceIn(5, 480),
                    recurrence = runCatching { TaskRecurrence.valueOf(item.optString("recurrence")) }
                        .getOrDefault(TaskRecurrence.NONE),
                    categoryId = item.optString("categoryId", "general").takeIf { saved -> categories.any { it.id == saved } } ?: "general",
                    subtasks = buildList {
                        val items = item.optJSONArray("subtasks") ?: JSONArray()
                        for (subIndex in 0 until items.length()) {
                            val sub = items.optJSONObject(subIndex) ?: continue
                            add(TaskSubtask(sub.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }, sub.optString("title"), sub.optBoolean("completed")))
                        }
                    }.filter { it.title.isNotBlank() },
                    reminderHour = item.optInt("reminderHour", -1).takeIf { it >= 0 },
                    reminderMinute = item.optInt("reminderMinute", 0),
                    reminderMode = item.optString("reminderMode").takeIf { it.isNotBlank() }
                        ?.let { saved -> runCatching { ItemReminderMode.valueOf(saved) }.getOrNull() },
                    criticalAlarm = item.optBoolean("criticalAlarm", false),
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
                val skippedDates = item.optJSONArray("skippedDates").toDateSet()
                add(Habit(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    emoji = item.optString("emoji", "✦"),
                    category = runCatching {
                        HabitCategory.valueOf(item.optString("category"))
                    }.getOrElse { HabitCategory.infer(item.getString("title")) },
                    categoryId = item.optString("categoryId").takeIf { saved -> categories.any { it.id == saved } }
                        ?: runCatching { HabitCategory.valueOf(item.optString("category")) }.getOrElse { HabitCategory.infer(item.getString("title")) }.let(CategoryCatalog::legacyId),
                    activeDays = days,
                    repeatEveryWeeks = item.optInt("repeatEveryWeeks", 1).coerceIn(1, 12),
                    skippedDates = skippedDates,
                    completions = completions,
                    progress = progress,
                    target = item.optInt("target", 1).coerceAtLeast(1),
                    unit = runCatching { HabitUnit.valueOf(item.optString("unit")) }
                        .getOrDefault(HabitUnit.CHECK),
                    reminderHour = item.optInt("reminderHour", -1).takeIf { it >= 0 },
                    reminderMinute = item.optInt("reminderMinute", 0),
                    reminderMode = item.optString("reminderMode").takeIf { it.isNotBlank() }
                        ?.let { saved -> runCatching { ItemReminderMode.valueOf(saved) }.getOrNull() },
                    criticalAlarm = item.optBoolean("criticalAlarm", false),
                    tags = item.optJSONArray("tags").toStringSet(),
                    archived = item.optBoolean("archived"),
                    deletedAt = item.optLong("deletedAt", -1L).takeIf { it >= 0L },
                    createdOn = LocalDate.parse(item.getString("createdOn"))
                ))
            }
        }
        val templatesJson = root.optJSONArray("taskTemplates") ?: JSONArray()
        val templates = buildList {
            for (index in 0 until templatesJson.length()) {
                val item = templatesJson.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                if (title.isEmpty()) continue
                add(TaskTemplate(
                    id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    name = item.optString("name", title).ifBlank { title }, title = title,
                    note = item.optString("note"),
                    priority = runCatching { TaskPriority.valueOf(item.optString("priority")) }.getOrDefault(TaskPriority.CALM),
                    durationMinutes = item.optInt("durationMinutes", 25).coerceIn(5, 480),
                    recurrence = runCatching { TaskRecurrence.valueOf(item.optString("recurrence")) }.getOrDefault(TaskRecurrence.NONE),
                    categoryId = item.optString("categoryId", "general").takeIf { saved -> categories.any { it.id == saved } } ?: "general",
                    subtasks = item.optJSONArray("subtasks").toSubtasks(),
                    reminderHour = item.optInt("reminderHour", -1).takeIf { it >= 0 },
                    reminderMinute = item.optInt("reminderMinute", 0),
                    reminderMode = item.optString("reminderMode").takeIf { it.isNotBlank() }
                        ?.let { value -> runCatching { ItemReminderMode.valueOf(value) }.getOrNull() },
                    tags = item.optJSONArray("tags").toStringSet()
                ))
            }
        }
        return TrazoState(tasks = tasks, habits = habits, categories = categories, taskTemplates = templates)
    }

    private companion object { const val KEY_STATE = "state_v1" }

    private fun JSONArray?.toStringSet(): Set<String> = buildSet {
        if (this@toStringSet != null) {
            for (index in 0 until length()) add(getString(index))
        }
    }

    private fun JSONArray?.toDateSet(): Set<LocalDate> = buildSet {
        if (this@toDateSet != null) {
            for (index in 0 until length()) {
                runCatching { LocalDate.parse(getString(index)) }.getOrNull()?.let(::add)
            }
        }
    }

    private fun JSONArray?.toSubtasks(): List<TaskSubtask> = buildList {
        val source = this@toSubtasks ?: return@buildList
        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            if (title.isNotEmpty()) add(TaskSubtask(
                id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                title = title,
                completed = item.optBoolean("completed"),
                dependsOnId = item.optString("dependsOnId").takeIf { it.isNotBlank() }
            ))
        }
    }
}
