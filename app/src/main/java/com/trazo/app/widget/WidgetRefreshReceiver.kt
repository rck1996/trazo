package com.trazo.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Refreshes launcher surfaces after an APK update, avoiding stale One UI placeholders. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) TrazoWidget.updateAll(context)
    }
}
