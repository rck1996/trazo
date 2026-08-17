package com.trazo.app.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
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

object ItemReminderScheduler {
    private const val PREFS = "trazo_item_alarm_ids"

    fun scheduleAll(context: Context, state: TrazoState = LocalStore(context).load()) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet("ids", emptySet()).orEmpty().forEach { raw ->
            val parts = raw.split(":", limit = 2)
            if (parts.size == 2) manager.cancel(alarmIntent(context, parts[0], parts[1]))
        }
        val scheduled = mutableSetOf<String>()
        val now = LocalDateTime.now()
        state.tasks.filter(Task::hasActiveReminder).forEach { task ->
            val at = LocalDateTime.of(task.dueDate, LocalTime.of(task.reminderHour!!, task.reminderMinute))
            if (at.isAfter(now)) {
                schedule(context, manager, "task", task.id, at)
                scheduled += "task:${task.id}"
            }
        }
        state.habits.filter {
            !it.archived && it.deletedAt == null && it.reminderHour != null
        }.forEach { habit ->
            var date = LocalDate.now()
            repeat(8) {
                val at = LocalDateTime.of(date, LocalTime.of(habit.reminderHour!!, habit.reminderMinute))
                if (habit.hasActiveReminderOn(date) && at.isAfter(now)) {
                    schedule(context, manager, "habit", habit.id, at)
                    scheduled += "habit:${habit.id}"
                    return@forEach
                }
                date = date.plusDays(1)
            }
        }
        prefs.edit().putStringSet("ids", scheduled).apply()
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

    fun snooze(context: Context, kind: String, id: String) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val trigger = System.currentTimeMillis() + 10 * 60_000L
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, alarmIntent(context, kind, id))
    }

    private fun schedule(
        context: Context,
        manager: AlarmManager,
        kind: String,
        id: String,
        at: LocalDateTime
    ) {
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            alarmIntent(context, kind, id)
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
}

class ItemReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_COMPLETE -> complete(context, kind, id)
            ACTION_SNOOZE -> {
                ItemReminderScheduler.snooze(context, kind, id)
                ItemReminderScheduler.cancelNotification(context, id)
            }
            else -> post(context, kind, id)
        }
    }

    private fun post(context: Context, kind: String, id: String) {
        NotificationCenter.createChannels(context)
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
        val open = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SECTION, if (kind == "task") "TASKS" else "HABITS"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val complete = actionIntent(context, ACTION_COMPLETE, kind, id)
        val snooze = actionIntent(context, ACTION_SNOOZE, kind, id)
        val notification = NotificationCompat.Builder(context, NotificationCenter.REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_handdrawn)
            .setContentTitle(if (kind == "task") "Tarea para ahora" else "Tu ritual te espera")
            .setContentText(title)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Hecho", complete)
            .addAction(0, "Posponer 10 min", snooze)
            .build()
        NotificationCenter.post(context, ItemReminderScheduler.notificationId(id), notification)
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

    private fun actionIntent(context: Context, action: String, kind: String, id: String) =
        PendingIntent.getBroadcast(
            context,
            "$action:$kind:$id".hashCode(),
            Intent(context, ItemReminderReceiver::class.java).setAction(action)
                .putExtra(EXTRA_KIND, kind).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val ACTION_ALARM = "com.trazo.app.ITEM_REMINDER"
        const val ACTION_COMPLETE = "com.trazo.app.ITEM_COMPLETE"
        const val ACTION_SNOOZE = "com.trazo.app.ITEM_SNOOZE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "item_id"
    }
}

internal fun Task.hasActiveReminder(): Boolean =
    !completed && !archived && deletedAt == null && dueDate != null && reminderHour != null

internal fun Habit.hasActiveReminderOn(date: LocalDate): Boolean =
    !archived && deletedAt == null && reminderHour != null &&
        HabitProgress.isScheduled(this, date) && !HabitProgress.isComplete(this, date)
