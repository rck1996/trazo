package com.trazo.app.notifications

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.view.WindowManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Explicit, opt-in alarm surface. It is never launched by ordinary reminders. */
class CriticalAlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val kind = intent.getStringExtra(ItemReminderReceiver.EXTRA_KIND).orEmpty()
        val id = intent.getStringExtra(ItemReminderReceiver.EXTRA_ID).orEmpty()
        val eventAt = intent.getLongExtra(ItemReminderReceiver.EXTRA_EVENT_AT, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 72, 48, 72)
            setBackgroundColor(Color.rgb(250, 247, 238))
            addView(TextView(context).apply { text = "ALARMA CRÍTICA"; textSize = 15f; setTextColor(Color.rgb(217, 83, 62)); gravity = Gravity.CENTER })
            addView(TextView(context).apply { text = title; textSize = 31f; setTextColor(Color.rgb(35, 32, 28)); gravity = Gravity.CENTER; setPadding(0, 32, 0, 48) })
            addView(actionButton("Hecho") { send(ItemReminderReceiver.ACTION_COMPLETE, kind, id, eventAt); finish() })
            addView(actionButton("Posponer 10 min") { send(ItemReminderReceiver.ACTION_SNOOZE, kind, id, eventAt, 10); finish() })
            addView(actionButton("Abrir Trazo") { startActivity(Intent(this@CriticalAlarmActivity, com.trazo.app.MainActivity::class.java)); finish() })
        }
        setContentView(layout)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 }
    }

    private fun send(action: String, kind: String, id: String, eventAt: Long, minutes: Int = 0) {
        sendBroadcast(Intent(this, ItemReminderReceiver::class.java).setAction(action)
            .putExtra(ItemReminderReceiver.EXTRA_KIND, kind)
            .putExtra(ItemReminderReceiver.EXTRA_ID, id)
            .putExtra(ItemReminderReceiver.EXTRA_EVENT_AT, eventAt)
            .putExtra(ItemReminderReceiver.EXTRA_MINUTES, minutes))
    }

    companion object { const val EXTRA_TITLE = "critical_title" }
}
