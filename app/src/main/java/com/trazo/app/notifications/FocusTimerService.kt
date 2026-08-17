package com.trazo.app.notifications

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trazo.app.R
import com.trazo.app.data.FocusStatsStore
import com.trazo.app.widget.FocusWidget

class FocusTimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var endAt = 0L
    private var taskTitle: String? = null
    private var phase = "FOCUS"
    private var totalSeconds = 0
    private val tick = object : Runnable {
        override fun run() {
            val left = ((endAt - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            if (left == 0) {
                if (phase == "FOCUS") FocusStatsStore.record(this@FocusTimerService, totalSeconds)
                FocusSessionStore.clear(this@FocusTimerService)
                FocusWidget.updateAll(this@FocusTimerService)
                if (NotificationCenter.canNotify(this@FocusTimerService)) {
                    val wasBreak = phase == "BREAK"
                    NotificationCenter.post(this@FocusTimerService,
                        7202, NotificationCompat.Builder(this@FocusTimerService, NotificationCenter.REMINDERS)
                            .setSmallIcon(R.drawable.ic_launcher_handdrawn)
                            .setContentTitle(if (wasBreak) "Descanso terminado" else "Pomodoro terminado")
                            .setContentText(if (wasBreak) "Cuando quieras, vuelve a tu siguiente trazo." else "Buen trabajo. Es momento de respirar.")
                            .setContentIntent(NotificationCenter.openAppIntent(this@FocusTimerService)).setAutoCancel(true).build()
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                updateNotification(left)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() { super.onCreate(); NotificationCenter.createChannels(this) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                handler.removeCallbacks(tick)
                FocusSessionStore.clear(this)
                FocusWidget.updateAll(this)
                if (intent.getBooleanExtra(EXTRA_RESET_UI, true)) publishStopped()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                endAt = intent?.getLongExtra(EXTRA_END_AT, 0L) ?: 0L
                taskTitle = intent?.getStringExtra(EXTRA_TASK)
                phase = intent?.getStringExtra(EXTRA_PHASE) ?: "FOCUS"
                val left = ((endAt - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(1)
                totalSeconds = left
                FocusSessionStore.save(this, endAt, taskTitle, phase, left)
                FocusWidget.updateAll(this)
                startForeground(7201, buildNotification(left))
                handler.removeCallbacks(tick); handler.post(tick)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(seconds: Int): android.app.Notification {
        val stop = android.app.PendingIntent.getService(
            this, 7203, Intent(this, FocusTimerService::class.java)
                .setAction(ACTION_STOP).putExtra(EXTRA_RESET_UI, true),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationCenter.FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_handdrawn)
            .setContentTitle("🍅 ${format(seconds)} · ${if (phase == "BREAK") "Descanso" else "Enfoque"}")
            .setContentText(if (phase == "BREAK") "Respira y vuelve con calma" else taskTitle ?: "Un trazo a la vez")
            .setContentIntent(NotificationCenter.openAppIntent(this))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .addAction(0, "Detener", stop).build()
    }
    private fun updateNotification(seconds: Int) = NotificationCenter.post(this, 7201, buildNotification(seconds))
    private fun format(s: Int) = "%02d:%02d".format(s / 60, s % 60)
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun publishStopped() {
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName).putExtra(EXTRA_RUNNING, false))
    }

    companion object {
        const val ACTION_STOP = "com.trazo.app.STOP_FOCUS"
        const val EXTRA_END_AT = "end_at"
        const val EXTRA_TASK = "task"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_RESET_UI = "reset_ui"
        const val ACTION_STATE_CHANGED = "com.trazo.app.FOCUS_STATE_CHANGED"
        const val EXTRA_RUNNING = "running"
    }
}
