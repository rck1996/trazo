package com.trazo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trazo.app.MainActivity
import com.trazo.app.R
import com.trazo.app.VoiceCaptureActivity

/** One-tap voice capture that sends the phrase through SmartCaptureParser. */
class SmartCaptureWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.smart_capture_widget)
            views.setOnClickPendingIntent(
                R.id.smart_capture_voice,
                PendingIntent.getActivity(
                    context, id,
                    Intent(context, VoiceCaptureActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setOnClickPendingIntent(
                R.id.smart_capture_root,
                PendingIntent.getActivity(
                    context, id + 20_000,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            manager.updateAppWidget(id, views)
        }
    }
}
