package com.trazo.app.notifications

import android.app.AlarmManager
import android.app.Notification
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
        prefs.edit { putStringSet("keys", keys.toList().takeLast(120).toSet()) }
    }
}

object ItemReminderScheduler {
    private const val PREFS = "trazo_item_alarm_ids"

    fun scheduleAll(context: Context, state: TrazoState = LocalStore(context).load()) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val settings = ReminderPreferences.load(context)
        val stored = prefs.getStringSet("ids", emptySet()).orEmpty()
        stored.forEach { raw -> cancelStored(context, manager, raw) }
        if (!settings.masterEnabled) {
            prefs.edit { putStringSet("ids", emptySet()) }
            return
        }
        val scheduled = mutableSetOf<String>()
        val now = LocalDateTime.now()
        if (settings.taskReminders) state.tasks.filter(Task::hasActiveReminder).forEach { task ->
            val eventAt = LocalDateTime.of(task.dueDate, LocalTime.of(task.reminderHour!!, task.reminderMinute))
            viableDeliveries(context, "task", task.id, eventAt, settings, now).forEach { delivery ->
                schedule(context, "task", task.id, eventAt, delivery, now)
                scheduled += token("task", task.id, delivery.stage)
            }
        }
        if (settings.habitReminders) state.habits.filter {
            !it.archived && it.deletedAt == null && it.reminderHour != null
        }.forEach { habit ->
            var date = LocalDate.now()
            repeat(15) {
                if (habit.hasActiveReminderOn(date)) {
                    val eventAt = LocalDateTime.of(date, LocalTime.of(habit.reminderHour!!, habit.reminderMinute))
                    val deliveries = viableDeliveries(context, "habit", habit.id, eventAt, settings, now)
                    if (deliveries.isNotEmpty()) {
                        deliveries.forEach { delivery ->
                            schedule(context, "habit", habit.id, eventAt, delivery, now)
                            scheduled += token("habit", habit.id, delivery.stage)
                        }
                        return@forEach
                    }
                }
                date = date.plusDays(1)
            }
        }
        stored.mapNotNull(::parseSnoozeToken)
            .filter { snooze -> snooze.triggerAtMillis > System.currentTimeMillis() }
            .filter { snooze -> snooze.isStillActive(state, settings) }
            .forEach { snooze ->
                AlarmDelivery.schedule(
                    context,
                    snooze.triggerAtMillis,
                    alarmIntent(
                        context,
                        snooze.kind,
                        snooze.id,
                        "snooze",
                        snooze.triggerAtMillis,
                        snooze.eventAtMillis
                    ),
                    exactPreferred = true
                )
                scheduled += snooze.token()
            }
        prefs.edit { putStringSet("ids", scheduled) }
    }

    private fun viableDeliveries(
        context: Context,
        kind: String,
        id: String,
        eventAt: LocalDateTime,
        settings: ReminderSettings,
        now: LocalDateTime
    ): List<AlarmNotificationPolicy.Delivery> {
        val deliveries = AlarmNotificationPolicy.deliveries(eventAt, settings.deliveryMode, settings.earlyMinutes)
            .filterNot { ReminderDeliveryStore.wasDelivered(context, deliveryKey(kind, id, it.at, it.stage)) }
        val future = deliveries.filter { it.at.isAfter(now) }
        val recovered = if (settings.recoverMissed) {
            deliveries.filter { ReminderSchedulePolicy.shouldRecover(it.at, now) }.maxByOrNull { it.at }
        } else null
        return future + listOfNotNull(recovered)
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

    fun snooze(context: Context, kind: String, id: String, minutes: Int, eventAtMillis: Long) {
        val trigger = LocalDateTime.now().plusMinutes(minutes.coerceIn(1, 180).toLong())
        val millis = trigger.toMillis()
        val targetEventMillis = eventAtMillis.takeIf { it > 0L } ?: millis
        AlarmDelivery.schedule(
            context,
            millis,
            alarmIntent(context, kind, id, "snooze", millis, targetEventMillis),
            exactPreferred = true
        )
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "$kind|$id|snooze|"
        val tokens = prefs.getStringSet("ids", emptySet()).orEmpty()
            .filterNot { it.startsWith(prefix) }
            .toMutableSet()
        tokens += SnoozeToken(kind, id, millis, targetEventMillis).token()
        prefs.edit { putStringSet("ids", tokens) }
    }

    private fun schedule(
        context: Context,
        kind: String,
        id: String,
        eventAt: LocalDateTime,
        delivery: AlarmNotificationPolicy.Delivery,
        now: LocalDateTime
    ) {
        val plannedMillis = delivery.at.toMillis()
        val triggerMillis = if (delivery.at.isAfter(now)) plannedMillis else now.plusSeconds(3).toMillis()
        AlarmDelivery.schedule(
            context,
            triggerMillis,
            alarmIntent(context, kind, id, delivery.stage, plannedMillis, eventAt.toMillis()),
            exactPreferred = true
        )
    }

    private fun cancelStored(context: Context, manager: AlarmManager, raw: String) {
        val modern = raw.split("|")
        if (modern.size >= 3) {
            manager.cancel(alarmIntent(context, modern[0], modern[1], modern[2], 0L, 0L))
            return
        }
        val legacy = raw.split(":", limit = 2)
        if (legacy.size == 2) {
            manager.cancel(PendingIntent.getBroadcast(
                context,
                "${legacy[0]}:${legacy[1]}".hashCode(),
                Intent(context, ItemReminderReceiver::class.java).setAction(ItemReminderReceiver.ACTION_ALARM),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
        }
    }

    private fun alarmIntent(
        context: Context,
        kind: String,
        id: String,
        stage: String,
        plannedAtMillis: Long,
        eventAtMillis: Long
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        "$kind:$id:$stage".hashCode(),
        Intent(context, ItemReminderReceiver::class.java)
            .setAction(ItemReminderReceiver.ACTION_ALARM)
            .putExtra(ItemReminderReceiver.EXTRA_KIND, kind)
            .putExtra(ItemReminderReceiver.EXTRA_ID, id)
            .putExtra(ItemReminderReceiver.EXTRA_STAGE, stage)
            .putExtra(ItemReminderReceiver.EXTRA_PLANNED_AT, plannedAtMillis)
            .putExtra(ItemReminderReceiver.EXTRA_EVENT_AT, eventAtMillis),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun deliveryKey(kind: String, id: String, at: LocalDateTime, stage: String): String =
        "$kind:$id:${at.toLocalDate()}:${at.toLocalTime()}:$stage"

    private data class SnoozeToken(
        val kind: String,
        val id: String,
        val triggerAtMillis: Long,
        val eventAtMillis: Long
    ) {
        fun token() = "$kind|$id|snooze|$triggerAtMillis|$eventAtMillis"

        fun isStillActive(state: TrazoState, settings: ReminderSettings): Boolean {
            val eventDate = java.time.Instant.ofEpochMilli(eventAtMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            return when (kind) {
                "task" -> settings.taskReminders && state.tasks.any { it.id == id && it.hasActiveReminder() }
                "habit" -> settings.habitReminders && state.habits.any { it.id == id && it.hasActiveReminderOn(eventDate) }
                else -> false
            }
        }
    }

    private fun parseSnoozeToken(raw: String): SnoozeToken? {
        val fields = raw.split("|")
        if (fields.size != 5 || fields[2] != "snooze") return null
        return SnoozeToken(
            kind = fields[0],
            id = fields[1],
            triggerAtMillis = fields[3].toLongOrNull() ?: return null,
            eventAtMillis = fields[4].toLongOrNull() ?: return null
        )
    }

    private fun token(kind: String, id: String, stage: String) = "$kind|$id|$stage"
    private fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

class ItemReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_COMPLETE -> complete(context, kind, id, intent.getLongExtra(EXTRA_EVENT_AT, -1L))
            ACTION_SNOOZE -> {
                ItemReminderScheduler.snooze(
                    context,
                    kind,
                    id,
                    intent.getIntExtra(EXTRA_MINUTES, 10),
                    intent.getLongExtra(EXTRA_EVENT_AT, -1L)
                )
                ItemReminderScheduler.cancelNotification(context, id)
            }
            else -> post(
                context,
                kind,
                id,
                intent.getStringExtra(EXTRA_STAGE) ?: "on_time",
                intent.getLongExtra(EXTRA_PLANNED_AT, -1L),
                intent.getLongExtra(EXTRA_EVENT_AT, -1L)
            )
        }
    }

    private fun post(
        context: Context,
        kind: String,
        id: String,
        stage: String,
        plannedAtMillis: Long,
        eventAtMillis: Long
    ) {
        NotificationCenter.createChannels(context)
        val settings = ReminderPreferences.load(context)
        if (!settings.masterEnabled || (kind == "task" && !settings.taskReminders) ||
            (kind == "habit" && !settings.habitReminders)
        ) return
        val fallbackEventAt = LocalDateTime.now()
        val eventAt = eventAtMillis.takeIf { it > 0L }?.toDateTime() ?: fallbackEventAt
        val state = LocalStore(context).load()
        val item = if (kind == "task") {
            state.tasks.firstOrNull { it.id == id && it.hasActiveReminder() }
        } else {
            state.habits.firstOrNull { it.id == id && it.hasActiveReminderOn(eventAt.toLocalDate()) }
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
        val alarmMode = when (stage) {
            "early" -> true
            "snooze" -> settings.deliveryMode.usesAlarm
            else -> settings.deliveryMode.usesAlarm
        }
        val heading = when (stage) {
            "early" -> "Alarma previa · faltan ${settings.earlyMinutes} min"
            "snooze" -> "Recordatorio pospuesto"
            else -> if (kind == "task") "Tarea para ahora" else "Tu hábito es ahora"
        }
        val time = eventAt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        val day = if (eventAt.toLocalDate() == LocalDate.now()) "Hoy" else eventAt.toLocalDate().format(
            DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-CL"))
        )
        val detail = "$day · $time · $title"
        val open = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SECTION, if (kind == "task") "TASKS" else "HABITS"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channel = if (alarmMode) NotificationCenter.ALARM_REMINDERS else NotificationCenter.ITEM_REMINDERS
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_handdrawn)
            .setContentTitle(heading)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(if (alarmMode) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setGroup(NotificationCenter.GROUP_REMINDERS)
            .addAction(0, "Hecho", actionIntent(context, ACTION_COMPLETE, kind, id, eventAtMillis = eventAt.toMillis()))
            .addAction(0, "+10 min", actionIntent(context, ACTION_SNOOZE, kind, id, 10, eventAt.toMillis()))
            .addAction(0, "+30 min", actionIntent(context, ACTION_SNOOZE, kind, id, 30, eventAt.toMillis()))
        if (alarmMode) builder.setTimeoutAfter(AlarmNotificationPolicy.timeoutMillis(settings.alarmDurationSeconds))
        val notification = builder.build().apply {
                if (alarmMode) flags = flags or Notification.FLAG_INSISTENT
            }
        if (NotificationCenter.post(context, ItemReminderScheduler.notificationId(id), notification)) {
            val plannedAt = plannedAtMillis.takeIf { it > 0L }?.toDateTime() ?: eventAt
            ReminderDeliveryStore.markDelivered(context, ItemReminderScheduler.deliveryKey(kind, id, plannedAt, stage))
            ReminderHistory.record(context, heading, detail)
        }
        val latest = LocalStore(context).load()
        val stillActive = if (kind == "task") {
            latest.tasks.any { it.id == id && it.hasActiveReminder() }
        } else {
            latest.habits.any { it.id == id && it.hasActiveReminderOn(eventAt.toLocalDate()) }
        }
        if (!stillActive) ItemReminderScheduler.cancelNotification(context, id)
        ItemReminderScheduler.scheduleAll(context, latest)
    }

    private fun complete(context: Context, kind: String, id: String, eventAtMillis: Long) {
        val store = LocalStore(context)
        val state = store.load()
        val updated = if (kind == "task") {
            state.copy(tasks = state.tasks.map {
                if (it.id == id) it.copy(completed = true, completedAt = System.currentTimeMillis()) else it
            })
        } else {
            val targetDate = eventAtMillis.takeIf { it > 0L }?.toDateTime()?.toLocalDate() ?: LocalDate.now()
            state.copy(habits = state.habits.map {
                if (it.id == id) HabitProgress.withAmount(it, targetDate, it.target) else it
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
        minutes: Int = 0,
        eventAtMillis: Long = -1L
    ) = PendingIntent.getBroadcast(
        context,
        "$action:$kind:$id:$minutes".hashCode(),
        Intent(context, ItemReminderReceiver::class.java).setAction(action)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_MINUTES, minutes)
            .putExtra(EXTRA_EVENT_AT, eventAtMillis),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun Long.toDateTime(): LocalDateTime =
        java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun LocalDateTime.toMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    companion object {
        const val ACTION_ALARM = "com.trazo.app.ITEM_REMINDER"
        const val ACTION_COMPLETE = "com.trazo.app.ITEM_COMPLETE"
        const val ACTION_SNOOZE = "com.trazo.app.ITEM_SNOOZE"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "item_id"
        const val EXTRA_MINUTES = "snooze_minutes"
        const val EXTRA_STAGE = "delivery_stage"
        const val EXTRA_PLANNED_AT = "planned_at"
        const val EXTRA_EVENT_AT = "event_at"
    }
}

internal fun Task.hasActiveReminder(): Boolean =
    !completed && !archived && deletedAt == null && dueDate != null && reminderHour != null

internal fun Habit.hasActiveReminderOn(date: LocalDate): Boolean =
    !archived && deletedAt == null && reminderHour != null &&
        HabitProgress.isScheduled(this, date) && !HabitProgress.isComplete(this, date)
