package com.x.myview.steps

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.x.myview.MyViewApp
import com.x.myview.R
import com.x.myview.reminders.Reminders
import com.x.myview.util.formatSteps
import com.x.myview.util.todayEpochDay
import com.x.myview.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps the step sensor listened to while the app is closed, so the step count, the small
 * notification and the Summary widget move within a few seconds of every walk instead of every
 * ~15 minutes. It shows one quiet, ongoing notification (Android requires that) and can be turned
 * off in Settings.
 */
class StepService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var shown = -1
    private var shownDay = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val c = (application as MyViewApp).container
        Reminders.ensureChannels(this)
        try {
            val note = buildNotification(c.stepTracker.stepsFor(todayEpochDay()), c.settings.settings.value.stepGoal)
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            } else {
                startForeground(NOTIFICATION_ID, note)
            }
        } catch (_: Exception) {
            // Android refused (for example the permission was just taken away): nothing to run.
            stopSelf()
            return
        }
        c.stepTracker.startLive(StepTracker.OWNER_SERVICE)
        job = scope.launch {
            launch {
                c.stepTracker.steps.collect {
                    refresh(false)
                    delay(2000) // at most one refresh every two seconds while walking
                }
            }
            while (true) {
                delay(60_000)
                refresh(false) // also moves everything over midnight
            }
        }
    }

    private suspend fun refresh(force: Boolean) {
        val c = (application as MyViewApp).container
        val day = todayEpochDay()
        val n = c.stepTracker.stepsFor(day)
        if (!force && n == shown && day == shownDay) return
        shown = n
        shownDay = day
        try {
            val note = buildNotification(n, c.settings.settings.value.stepGoal)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, note)
        } catch (_: SecurityException) {
            // Notifications are switched off for the app; counting carries on.
        }
        try {
            WidgetUpdater.updateAll(applicationContext, syncSteps = false)
        } catch (_: Exception) {
            // The widget keeps its old numbers until the next refresh.
        }
    }

    private fun buildNotification(steps: Int, goal: Int): Notification {
        val g = goal.coerceAtLeast(1)
        return NotificationCompat.Builder(this, Reminders.CH_STEPS)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("${formatSteps(steps)} steps today")
            .setContentText(if (steps >= g) "Goal reached. Well done!" else "${formatSteps(g - steps)} to your goal of ${formatSteps(g)}")
            .setContentIntent(Reminders.openAppPending(this))
            .setProgress(g, steps.coerceAtMost(g), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        (application as MyViewApp).container.stepTracker.stopLive(StepTracker.OWNER_SERVICE)
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 7001

        /** Starts the service when steps are supported, allowed and switched on. Never throws. */
        fun startIfEnabled(ctx: Context) {
            val app = ctx.applicationContext
            try {
                val c = (app as MyViewApp).container
                if (!c.stepTracker.isSupported || !c.stepTracker.hasPermission()) return
                if (!c.settings.settings.value.liveSteps) return
                ContextCompat.startForegroundService(app, Intent(app, StepService::class.java))
            } catch (_: Exception) {
                // Android may refuse to start it from the background; the 15 minute worker still runs.
            }
        }

        fun stop(ctx: Context) {
            val app = ctx.applicationContext
            try {
                app.stopService(Intent(app, StepService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
