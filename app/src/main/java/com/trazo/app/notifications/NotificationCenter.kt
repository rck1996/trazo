package com.trazo.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.data.LocalStore
import com.trazo.app.model.HabitProgress
import com.trazo.app.model.TaskSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderPreferences {
    private const val FILE = "trazo_reminders"
    fun enabled(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("enabled", false)
    fun hour(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt("hour", 9)
    fun set(context: Context, enabled: Boolean, hour: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putBoolean("enabled", enabled); putInt("hour", hour) }
        if (enabled) ReminderScheduler.schedule(context) else ReminderScheduler.cancel(context)
    }
}

object NotificationCenter {
    const val REMINDERS = "trazo_reminders"
    const val FOCUS = "trazo_focus"
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(REMINDERS, "Agenda y hábitos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Resumen diario de tareas y hábitos"
            },
            NotificationChannel(FOCUS, "Temporizador de enfoque", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Pomodoro activo"
                setSound(null, null)
            }
        ))
    }

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun canNotify(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

object ReminderScheduler {
    private fun intent(context: Context) = PendingIntent.getBroadcast(
        context, 7101, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    fun schedule(context: Context) {
        if (!ReminderPreferences.enabled(context)) return
        val hour = ReminderPreferences.hour(context)
        var next = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, 0))
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent(context))
    }
    fun cancel(context: Context) = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(intent(context))
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationCenter.createChannels(context)
        val today = LocalDate.now()
        val state = LocalStore(context).load()
        val tasks = TaskSchedule.actionable(state.tasks, today).count { !it.completed }
        val habits = state.habits.count { !it.archived && it.deletedAt == null && HabitProgress.isScheduled(it, today) && !HabitProgress.isComplete(it, today) }
        if (NotificationCenter.canNotify(context) && (tasks + habits > 0)) {
            val text = when {
                tasks > 0 && habits > 0 -> "$tasks tareas y $habits hábitos te esperan hoy"
                tasks > 0 -> "$tasks tareas te esperan hoy"
                else -> "$habits hábitos te esperan hoy"
            }
            val notification = NotificationCompat.Builder(context, NotificationCenter.REMINDERS)
                .setSmallIcon(R.drawable.ic_launcher_handdrawn).setContentTitle("Tu página de hoy")
                .setContentText(text).setContentIntent(NotificationCenter.openAppIntent(context))
                .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
            NotificationCenter.post(context, 7102, notification)
        }
        ReminderScheduler.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)) {
            ReminderScheduler.schedule(context)
            ItemReminderScheduler.scheduleAll(context)
        }
    }
}
