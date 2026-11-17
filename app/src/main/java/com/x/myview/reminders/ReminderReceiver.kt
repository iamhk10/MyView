package com.x.myview.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.x.myview.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives alarms, notification buttons and widget taps (not exported; only this app can send to it). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.handle(app, intent)
            } catch (_: Exception) {
                // A failed reminder must never crash the app in the background.
            } finally {
                pending.finish()
            }
        }
    }
}

/** Phone restarted or app updated: alarms are gone, set them again and refresh widgets. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        Reminders.scheduleAllAsync(app)
        WidgetUpdater.updateAllAsync(app)
    }
}
