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
    private var autoAdvance = false
    private var focusSeconds = 25 * 60
    private var shortBreakSeconds = 5 * 60
    private var longBreakSeconds = 15 * 60
    private var cyclesBeforeLong = 4
    private var completedFocusSessions = 0
    private val tick = object : Runnable {
        override fun run() {
            val left = ((endAt - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
            if (left == 0) {
                if (phase == "FOCUS") {
                    FocusStatsStore.record(this@FocusTimerService, totalSeconds)
                    completedFocusSessions++
                }
                if (autoAdvance) {
                    phase = if (phase == "FOCUS") "BREAK" else "FOCUS"
                    totalSeconds = when {
                        phase == "FOCUS" -> focusSeconds
                        completedFocusSessions > 0 && completedFocusSessions % cyclesBeforeLong == 0 -> longBreakSeconds
                        else -> shortBreakSeconds
                    }
                    endAt = System.currentTimeMillis() + totalSeconds * 1000L
                    FocusSessionStore.save(this@FocusTimerService, endAt, taskTitle, phase, totalSeconds)
                    FocusWidget.updateAll(this@FocusTimerService)
                    publishState(true)
                    updateNotification(totalSeconds)
                    handler.postDelayed(this, 1000)
                    return
                }
                FocusSessionStore.clear(this@FocusTimerService)
                FocusWidget.updateAll(this@FocusTimerService)
                if (NotificationCenter.canNotify(this@FocusTimerService)) {
                    val wasBreak = phase == "BREAK"
                    NotificationCenter.post(this@FocusTimerService,
                        7202, NotificationCompat.Builder(this@FocusTimerService, NotificationCenter.PLANNING)
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
                autoAdvance = intent?.getBooleanExtra(EXTRA_AUTO_ADVANCE, false) ?: false
                focusSeconds = intent?.getIntExtra(EXTRA_FOCUS_SECONDS, 25 * 60) ?: 25 * 60
                shortBreakSeconds = intent?.getIntExtra(EXTRA_SHORT_BREAK_SECONDS, 5 * 60) ?: 5 * 60
                longBreakSeconds = intent?.getIntExtra(EXTRA_LONG_BREAK_SECONDS, 15 * 60) ?: 15 * 60
                cyclesBeforeLong = (intent?.getIntExtra(EXTRA_CYCLES_BEFORE_LONG, 4) ?: 4).coerceIn(2, 8)
                completedFocusSessions = intent?.getIntExtra(EXTRA_COMPLETED_SESSIONS, 0) ?: 0
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
    private fun publishStopped() = publishState(false)

    private fun publishState(running: Boolean) {
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName)
            .putExtra(EXTRA_RUNNING, running)
            .putExtra(EXTRA_PHASE, phase)
            .putExtra(EXTRA_END_AT, endAt)
            .putExtra(EXTRA_TOTAL_SECONDS, totalSeconds))
    }

    companion object {
        const val ACTION_STOP = "com.trazo.app.STOP_FOCUS"
        const val EXTRA_END_AT = "end_at"
        const val EXTRA_TASK = "task"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_RESET_UI = "reset_ui"
        const val EXTRA_AUTO_ADVANCE = "auto_advance"
        const val EXTRA_FOCUS_SECONDS = "focus_seconds"
        const val EXTRA_SHORT_BREAK_SECONDS = "short_break_seconds"
        const val EXTRA_LONG_BREAK_SECONDS = "long_break_seconds"
        const val EXTRA_CYCLES_BEFORE_LONG = "cycles_before_long"
        const val EXTRA_COMPLETED_SESSIONS = "completed_sessions"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val ACTION_STATE_CHANGED = "com.trazo.app.FOCUS_STATE_CHANGED"
        const val EXTRA_RUNNING = "running"
    }
}
