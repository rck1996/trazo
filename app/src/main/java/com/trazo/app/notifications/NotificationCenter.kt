package com.trazo.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.TaskSchedule
import com.trazo.app.model.TrazoState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class ReminderSettings(
    val masterEnabled: Boolean = true,
    val taskReminders: Boolean = true,
    val habitReminders: Boolean = true,
    val morningEnabled: Boolean = false,
    val morningHour: Int = 9,
    val morningMinute: Int = 0,
    val eveningEnabled: Boolean = false,
    val eveningHour: Int = 20,
    val eveningMinute: Int = 0,
    val recoverMissed: Boolean = true
)

object ReminderPreferences {
    private const val FILE = "trazo_reminders"

    fun load(context: Context): ReminderSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val legacyDailyEnabled = prefs.getBoolean("enabled", false)
        val legacyDailyHour = prefs.getInt("hour", 9)
        return ReminderSettings(
            masterEnabled = prefs.getBoolean("master_enabled", true),
            taskReminders = prefs.getBoolean("task_reminders", true),
            habitReminders = prefs.getBoolean("habit_reminders", true),
            morningEnabled = prefs.getBoolean("morning_enabled", legacyDailyEnabled),
            morningHour = prefs.getInt("morning_hour", legacyDailyHour).coerceIn(0, 23),
            morningMinute = prefs.getInt("morning_minute", 0).coerceIn(0, 59),
            eveningEnabled = prefs.getBoolean("evening_enabled", false),
            eveningHour = prefs.getInt("evening_hour", 20).coerceIn(0, 23),
            eveningMinute = prefs.getInt("evening_minute", 0).coerceIn(0, 59),
            recoverMissed = prefs.getBoolean("recover_missed", true)
        )
    }

    fun save(context: Context, settings: ReminderSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putBoolean("master_enabled", settings.masterEnabled)
            putBoolean("task_reminders", settings.taskReminders)
            putBoolean("habit_reminders", settings.habitReminders)
            putBoolean("morning_enabled", settings.morningEnabled)
            putInt("morning_hour", settings.morningHour)
            putInt("morning_minute", settings.morningMinute)
            putBoolean("evening_enabled", settings.eveningEnabled)
            putInt("evening_hour", settings.eveningHour)
            putInt("evening_minute", settings.eveningMinute)
            putBoolean("recover_missed", settings.recoverMissed)
        }
        ReminderScheduler.schedule(context)
        ItemReminderScheduler.scheduleAll(context)
        val state = LocalStore(context).load()
        if (!settings.masterEnabled || !settings.taskReminders) {
            state.tasks.forEach { ItemReminderScheduler.cancelNotification(context, it.id) }
        }
        if (!settings.masterEnabled || !settings.habitReminders) {
            state.habits.forEach { ItemReminderScheduler.cancelNotification(context, it.id) }
        }
        if (!settings.masterEnabled) {
            context.getSystemService(NotificationManager::class.java).apply {
                cancel(7102)
                cancel(7104)
                cancel(7199)
            }
        }
    }
}

data class ReminderReceipt(val title: String, val body: String, val atMillis: Long)

object ReminderHistory {
    private const val FILE = "trazo_reminder_history"

    fun latest(context: Context): ReminderReceipt? {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val at = prefs.getLong("last_at", -1L)
        if (at < 0L) return null
        return ReminderReceipt(
            title = prefs.getString("last_title", "Aviso").orEmpty(),
            body = prefs.getString("last_body", "").orEmpty(),
            atMillis = at
        )
    }

    fun record(context: Context, title: String, body: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString("last_title", title)
            putString("last_body", body)
            putLong("last_at", System.currentTimeMillis())
        }
    }
}

object NotificationCenter {
    const val ITEM_REMINDERS = "trazo_item_reminders_v2"
    const val PLANNING = "trazo_planning_v2"
    const val FOCUS = "trazo_focus"
    const val GROUP_REMINDERS = "trazo_reminder_group"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannelGroup(NotificationChannelGroup(GROUP_REMINDERS, "Avisos de Trazo"))
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(ITEM_REMINDERS, "Tareas y hábitos a su hora", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos puntuales, acciones para completar y posponer"
                    group = GROUP_REMINDERS
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(PLANNING, "Resumen y cierre del día", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Agenda matinal y revisión de pendientes al final del día"
                    group = GROUP_REMINDERS
                },
                NotificationChannel(FOCUS, "Temporizador de enfoque", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Pomodoro activo"
                    setSound(null, null)
                }
            )
        )
    }

    fun openAppIntent(context: Context, section: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            section?.let { putExtra(MainActivity.EXTRA_SECTION, it) }
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canNotify(context: Context): Boolean {
        val runtimeGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun itemChannelEnabled(context: Context): Boolean {
        if (!canNotify(context)) return false
        createChannels(context)
        return context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(ITEM_REMINDERS)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM").setData(Uri.parse("package:${context.packageName}"))

    fun post(context: Context, id: Int, notification: android.app.Notification): Boolean {
        if (!canNotify(context)) return false
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(notification.channelId)?.importance == NotificationManager.IMPORTANCE_NONE
        ) return false
        return runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        }.getOrDefault(false)
    }

    fun postTest(context: Context): Boolean {
        createChannels(context)
        val title = "Trazo sí puede avisarte"
        val body = "Esta es una prueba. Tus recordatorios aparecerán con sonido, vibración y acciones rápidas."
        val notification = NotificationCompat.Builder(context, ITEM_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_handdrawn)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(context, "TODAY", 7198))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val posted = post(context, 7199, notification)
        if (posted) ReminderHistory.record(context, title, body)
        return posted
    }
}

object ReminderSchedulePolicy {
    fun nextDaily(hour: Int, minute: Int, now: LocalDateTime): LocalDateTime {
        var next = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next
    }

    fun shouldRecover(scheduledAt: LocalDateTime, now: LocalDateTime, windowHours: Long = 6): Boolean =
        !scheduledAt.isAfter(now) && !scheduledAt.isBefore(now.minusHours(windowHours))
}

internal object AlarmDelivery {
    fun schedule(context: Context, atMillis: Long, intent: PendingIntent, exactPreferred: Boolean) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (exactPreferred && NotificationCenter.canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        }
    }
}

object ReminderScheduler {
    private const val MORNING = "morning"
    private const val EVENING = "evening"

    private fun intent(context: Context, kind: String) = PendingIntent.getBroadcast(
        context,
        if (kind == MORNING) 7101 else 7103,
        Intent(context, ReminderReceiver::class.java)
            .setAction("com.trazo.app.DAILY_${kind.uppercase()}")
            .putExtra(ReminderReceiver.EXTRA_KIND, kind),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(context: Context) {
        cancel(context)
        val settings = ReminderPreferences.load(context)
        if (!settings.masterEnabled) return
        val now = LocalDateTime.now()
        if (settings.morningEnabled) {
            val next = ReminderSchedulePolicy.nextDaily(settings.morningHour, settings.morningMinute, now)
            AlarmDelivery.schedule(context, next.toMillis(), intent(context, MORNING), exactPreferred = false)
        }
        if (settings.eveningEnabled) {
            val next = ReminderSchedulePolicy.nextDaily(settings.eveningHour, settings.eveningMinute, now)
            AlarmDelivery.schedule(context, next.toMillis(), intent(context, EVENING), exactPreferred = false)
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(intent(context, MORNING))
        manager.cancel(intent(context, EVENING))
    }

    private fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationCenter.createChannels(context)
        val settings = ReminderPreferences.load(context)
        if (!settings.masterEnabled) return
        val state = LocalStore(context).load()
        val today = LocalDate.now()
        val tasks = TaskSchedule.actionable(state.tasks, today).count { !it.completed }
        val habits = state.habits.count {
            !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, today) && !HabitProgress.isComplete(it, today)
        }
        val kind = intent?.getStringExtra(EXTRA_KIND) ?: MORNING_KIND
        if (tasks + habits > 0) {
            val morning = kind == MORNING_KIND
            val title = if (morning) "Tu página de hoy" else "Antes de cerrar el día"
            val body = summaryText(tasks, habits, morning)
            val notification = NotificationCompat.Builder(context, NotificationCenter.PLANNING)
                .setSmallIcon(R.drawable.ic_launcher_handdrawn)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(NotificationCenter.openAppIntent(context, "TODAY", if (morning) 7102 else 7104))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(NotificationCenter.GROUP_REMINDERS)
                .build()
            val id = if (morning) 7102 else 7104
            if (NotificationCenter.post(context, id, notification)) ReminderHistory.record(context, title, body)
        }
        ReminderScheduler.schedule(context)
    }

    private fun summaryText(tasks: Int, habits: Int, morning: Boolean): String {
        val items = buildList {
            if (tasks > 0) add("$tasks ${if (tasks == 1) "tarea pendiente" else "tareas pendientes"}")
            if (habits > 0) add("$habits ${if (habits == 1) "hábito por completar" else "hábitos por completar"}")
        }.joinToString(" y ")
        return if (morning) "$items. Elige tu primer trazo." else "$items. Aún puedes cerrar algo pequeño."
    }

    companion object {
        const val EXTRA_KIND = "summary_kind"
        private const val MORNING_KIND = "morning"
    }
}

object ReminderStatus {
    fun nextScheduled(context: Context, state: TrazoState): LocalDateTime? {
        val settings = ReminderPreferences.load(context)
        if (!settings.masterEnabled) return null
        val now = LocalDateTime.now()
        val candidates = mutableListOf<LocalDateTime>()
        if (settings.morningEnabled) candidates += ReminderSchedulePolicy.nextDaily(settings.morningHour, settings.morningMinute, now)
        if (settings.eveningEnabled) candidates += ReminderSchedulePolicy.nextDaily(settings.eveningHour, settings.eveningMinute, now)
        if (settings.taskReminders) state.tasks.filter { it.hasActiveReminder() }.forEach {
            val at = LocalDateTime.of(it.dueDate, LocalTime.of(it.reminderHour!!, it.reminderMinute))
            if (at.isAfter(now)) candidates += at
        }
        if (settings.habitReminders) state.habits.filter { !it.archived && it.deletedAt == null && it.reminderHour != null }.forEach { habit ->
            var date = LocalDate.now()
            repeat(15) {
                val at = LocalDateTime.of(date, LocalTime.of(habit.reminderHour!!, habit.reminderMinute))
                if (habit.hasActiveReminderOn(date) && at.isAfter(now)) {
                    candidates += at
                    return@forEach
                }
                date = date.plusDays(1)
            }
        }
        return candidates.minOrNull()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            )
        ) {
            NotificationCenter.createChannels(context)
            ReminderScheduler.schedule(context)
            ItemReminderScheduler.scheduleAll(context)
        }
    }
}
