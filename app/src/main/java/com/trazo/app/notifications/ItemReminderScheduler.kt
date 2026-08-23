package com.trazo.app.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.model.Habit
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.Task
import com.trazo.app.model.TrazoState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private object ReminderDeliveryStore {
    private const val FILE = "trazo_delivered_reminders"

    fun wasDelivered(context: Context, key: String): Boolean =
        key in context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getStringSet("keys", emptySet()).orEmpty()

    fun markDelivered(context: Context, key: String) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val keys = prefs.getStringSet("keys", emptySet()).orEmpty().toMutableSet()
        keys += key
        prefs.edit { putStringSet("keys", keys.toList().takeLast(80).toSet()) }
    }
}

object ItemReminderScheduler {
    private const val PREFS = "trazo_item_alarm_ids"

    fun scheduleAll(context: Context, state: TrazoState = LocalStore(context).load()) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet("ids", emptySet()).orEmpty().forEach { raw ->
            val parts = raw.split(":", limit = 2)
            if (parts.size == 2) manager.cancel(alarmIntent(context, parts[0], parts[1]))
        }
        val settings = ReminderPreferences.load(context)
        if (!settings.masterEnabled) {
            prefs.edit { putStringSet("ids", emptySet()) }
            return
        }
        val scheduled = mutableSetOf<String>()
        val now = LocalDateTime.now()
        if (settings.taskReminders) state.tasks.filter(Task::hasActiveReminder).forEach { task ->
            val at = LocalDateTime.of(task.dueDate, LocalTime.of(task.reminderHour!!, task.reminderMinute))
            val key = deliveryKey("task", task.id, at)
            val trigger = when {
                at.isAfter(now) -> at
                settings.recoverMissed && ReminderSchedulePolicy.shouldRecover(at, now) &&
                    !ReminderDeliveryStore.wasDelivered(context, key) -> now.plusSeconds(3)
                else -> null
            }
            if (trigger != null) {
                schedule(context, "task", task.id, trigger)
                scheduled += "task:${task.id}"
            }
        }
        if (settings.habitReminders) state.habits.filter {
            !it.archived && it.deletedAt == null && it.reminderHour != null
        }.forEach { habit ->
            var date = LocalDate.now()
            repeat(15) {
                val at = LocalDateTime.of(date, LocalTime.of(habit.reminderHour!!, habit.reminderMinute))
                val key = deliveryKey("habit", habit.id, at)
                val recover = date == LocalDate.now() && settings.recoverMissed &&
                    ReminderSchedulePolicy.shouldRecover(at, now) && !ReminderDeliveryStore.wasDelivered(context, key)
                if (habit.hasActiveReminderOn(date) && (at.isAfter(now) || recover)) {
                    schedule(context, "habit", habit.id, if (recover) now.plusSeconds(4) else at)
                    scheduled += "habit:${habit.id}"
                    return@forEach
                }
                date = date.plusDays(1)
            }
        }
        prefs.edit { putStringSet("ids", scheduled) }
    }

    /** Alarm cancellation alone does not dismiss a notification posted earlier. */
    fun syncAfterSave(context: Context, previous: TrazoState, current: TrazoState) {
        scheduleAll(context, current)
        val currentTaskIds = current.tasks.mapTo(mutableSetOf()) { it.id }
        val currentHabitIds = current.habits.mapTo(mutableSetOf()) { it.id }
        val removedIds = buildSet {
            previous.tasks.filterNot { it.id in currentTaskIds }.forEach { add(it.id) }
            previous.habits.filterNot { it.id in currentHabitIds }.forEach { add(it.id) }
        }
        val inactiveIds = buildSet {
            current.tasks.filterNot(Task::hasActiveReminder).forEach { add(it.id) }
            current.habits.filterNot { it.hasActiveReminderOn(LocalDate.now()) }.forEach { add(it.id) }
        }
        (removedIds + inactiveIds).forEach { cancelNotification(context, it) }
    }

    fun cancelNotification(context: Context, id: String) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(id))
    }

    internal fun notificationId(id: String) = 7400 + id.hashCode().and(0x3fff)

    fun snooze(context: Context, kind: String, id: String, minutes: Int) {
        val trigger = System.currentTimeMillis() + minutes.coerceIn(1, 180) * 60_000L
        AlarmDelivery.schedule(context, trigger, alarmIntent(context, kind, id), exactPreferred = true)
    }

    private fun schedule(context: Context, kind: String, id: String, at: LocalDateTime) {
        AlarmDelivery.schedule(
            context,
            at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            alarmIntent(context, kind, id),
            exactPreferred = true
        )
    }

    private fun alarmIntent(context: Context, kind: String, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            "$kind:$id".hashCode(),
            Intent(context, ItemReminderReceiver::class.java)
                .setAction(ItemReminderReceiver.ACTION_ALARM)
                .putExtra(ItemReminderReceiver.EXTRA_KIND, kind)
                .putExtra(ItemReminderReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun deliveryKey(kind: String, id: String, at: LocalDateTime): String =
        "$kind:$id:${at.toLocalDate()}:${at.toLocalTime()}"
}

class ItemReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_COMPLETE -> complete(context, kind, id)
            ACTION_SNOOZE -> {
                ItemReminderScheduler.snooze(context, kind, id, intent.getIntExtra(EXTRA_MINUTES, 10))
                ItemReminderScheduler.cancelNotification(context, id)
            }
            else -> post(context, kind, id)
        }
    }

    private fun post(context: Context, kind: String, id: String) {
        NotificationCenter.createChannels(context)
        val settings = ReminderPreferences.load(context)
        if (!settings.masterEnabled || (kind == "task" && !settings.taskReminders) ||
            (kind == "habit" && !settings.habitReminders)
        ) return
        val state = LocalStore(context).load()
        val item = if (kind == "task") {
            state.tasks.firstOrNull { it.id == id && it.hasActiveReminder() }
        } else {
            state.habits.firstOrNull { it.id == id && it.hasActiveReminderOn(LocalDate.now()) }
        }
        val title = when (item) {
            is Task -> item.title
            is Habit -> item.title
            else -> {
                ItemReminderScheduler.cancelNotification(context, id)
                ItemReminderScheduler.scheduleAll(context, state)
                return
            }
        }
        val eventAt = when (item) {
            is Task -> LocalDateTime.of(item.dueDate, LocalTime.of(item.reminderHour!!, item.reminderMinute))
            is Habit -> LocalDateTime.of(LocalDate.now(), LocalTime.of(item.reminderHour!!, item.reminderMinute))
            else -> return
        }
        val heading = if (kind == "task") "Tarea para ahora" else "Tu hábito te espera"
        val detail = if (item is Task) {
            val day = if (item.dueDate == LocalDate.now()) "Hoy" else item.dueDate?.format(
                DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-CL"))
            )
            "$day · ${eventAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))} · $title"
        } else {
            "Ahora · $title"
        }
        val open = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SECTION, if (kind == "task") "TASKS" else "HABITS"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationCenter.ITEM_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_handdrawn)
            .setContentTitle(heading)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(NotificationCenter.GROUP_REMINDERS)
            .addAction(0, "Hecho", actionIntent(context, ACTION_COMPLETE, kind, id))
            .addAction(0, "+10 min", actionIntent(context, ACTION_SNOOZE, kind, id, 10))
            .addAction(0, "+30 min", actionIntent(context, ACTION_SNOOZE, kind, id, 30))
            .build()
        if (NotificationCenter.post(context, ItemReminderScheduler.notificationId(id), notification)) {
            ReminderDeliveryStore.markDelivered(context, ItemReminderScheduler.deliveryKey(kind, id, eventAt))
            ReminderHistory.record(context, heading, detail)
        }
        val latest = LocalStore(context).load()
        val stillActive = if (kind == "task") {
            latest.tasks.any { it.id == id && it.hasActiveReminder() }
        } else {
            latest.habits.any { it.id == id && it.hasActiveReminderOn(LocalDate.now()) }
        }
        if (!stillActive) ItemReminderScheduler.cancelNotification(context, id)
        ItemReminderScheduler.scheduleAll(context, latest)
    }

    private fun complete(context: Context, kind: String, id: String) {
        val store = LocalStore(context)
        val state = store.load()
        val updated = if (kind == "task") {
            state.copy(tasks = state.tasks.map {
                if (it.id == id) it.copy(completed = true, completedAt = System.currentTimeMillis()) else it
            })
        } else {
            val today = LocalDate.now()
            state.copy(habits = state.habits.map {
                if (it.id == id) HabitProgress.withAmount(it, today, it.target) else it
            })
        }
        store.save(updated)
        ItemReminderScheduler.cancelNotification(context, id)
    }

    private fun actionIntent(
        context: Context,
        action: String,
        kind: String,
        id: String,
        minutes: Int = 0
    ) = PendingIntent.getBroadcast(
        context,
        "$action:$kind:$id:$minutes".hashCode(),
        Intent(context, ItemReminderReceiver::class.java).setAction(action)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_MINUTES, minutes),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val ACTION_ALARM = "com.trazo.app.ITEM_REMINDER"
        const val ACTION_COMPLETE = "com.trazo.app.ITEM_COMPLETE"
        const val ACTION_SNOOZE = "com.trazo.app.ITEM_SNOOZE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "item_id"
        const val EXTRA_MINUTES = "snooze_minutes"
    }
}

internal fun Task.hasActiveReminder(): Boolean =
    !completed && !archived && deletedAt == null && dueDate != null && reminderHour != null

internal fun Habit.hasActiveReminderOn(date: LocalDate): Boolean =
    !archived && deletedAt == null && reminderHour != null &&
        HabitProgress.isScheduled(this, date) && !HabitProgress.isComplete(this, date)
