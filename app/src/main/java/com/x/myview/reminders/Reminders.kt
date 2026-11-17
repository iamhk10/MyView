package com.x.myview.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.x.myview.MainActivity
import com.x.myview.MyViewApp
import com.x.myview.R
import com.x.myview.data.db.HabitEntity
import com.x.myview.di.AppContainer
import com.x.myview.util.ALL_DAYS
import com.x.myview.util.formatMl
import com.x.myview.util.isScheduled
import com.x.myview.util.todayEpochDay
import com.x.myview.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Habit, water and workout reminders.
 *
 * Every reminder is a single alarm for the NEXT time it should ring. When it rings we show the
 * notification (unless there is nothing left to remind about) and set the alarm for the one after.
 * Alarms are "inexact": Android may deliver them a few minutes late to save battery, which is fine
 * for reminders and needs no special permission.
 */
object Reminders {
    const val CH_HABITS = "habits"
    const val CH_WATER = "water"
    const val CH_WORKOUT = "workout"

    const val ACTION_FIRE = "com.x.myview.action.FIRE"
    const val ACTION_HABIT_DONE = "com.x.myview.action.HABIT_DONE"
    const val ACTION_TOGGLE_HABIT = "com.x.myview.action.TOGGLE_HABIT"
    const val ACTION_ADD_WATER = "com.x.myview.action.ADD_WATER"

    const val EXTRA_TYPE = "type"
    const val EXTRA_ID = "id"

    const val TYPE_HABIT = "habit"
    const val TYPE_WATER = "water"
    const val TYPE_WORKOUT = "workout"

    private const val WATER_FIRST_HOUR = 8
    private const val WATER_LAST_HOUR = 22

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun container(ctx: Context): AppContainer = (ctx.applicationContext as MyViewApp).container

    // ---------- Channels ----------

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH_HABITS, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_WATER, "Water reminders", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_WORKOUT, "Workout reminder", NotificationManager.IMPORTANCE_DEFAULT))
    }

    // ---------- Scheduling ----------

    fun scheduleAllAsync(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch { scheduleAll(app) }
    }

    suspend fun scheduleAll(ctx: Context) {
        val c = container(ctx)
        val s = c.settings.settings.value
        c.repository.habits.first().forEach { h ->
            if (h.reminderMin >= 0) scheduleHabit(ctx, h) else cancel(ctx, TYPE_HABIT, h.id)
        }
        if (s.waterReminder) scheduleWater(ctx, s.waterIntervalH) else cancel(ctx, TYPE_WATER, 0)
        if (s.workoutReminder) scheduleWorkout(ctx, s.workoutReminderMin) else cancel(ctx, TYPE_WORKOUT, 0)
    }

    fun cancelHabit(ctx: Context, id: Long) = cancel(ctx.applicationContext, TYPE_HABIT, id)

    private fun scheduleHabit(ctx: Context, h: HabitEntity) {
        val at = nextDaily(h.reminderMin, h.days) ?: return
        setAlarm(ctx, TYPE_HABIT, h.id, at)
    }

    private fun scheduleWorkout(ctx: Context, minute: Int) {
        val at = nextDaily(minute, ALL_DAYS) ?: return
        setAlarm(ctx, TYPE_WORKOUT, 0, at)
    }

    private fun scheduleWater(ctx: Context, intervalH: Int) {
        setAlarm(ctx, TYPE_WATER, 0, nextWater(intervalH))
    }

    private fun setAlarm(ctx: Context, type: String, id: Long, atMillis: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, firePending(ctx, type, id))
    }

    private fun cancel(ctx: Context, type: String, id: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePending(ctx, type, id))
    }

    /** Next time (millis) at [minuteOfDay] on a day allowed by [daysMask], at least 20 s from now. */
    internal fun nextDaily(minuteOfDay: Int, daysMask: Int): Long? {
        val now = ZonedDateTime.now()
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val at = date.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(now.zone)
            if (at.isAfter(now.plusSeconds(20)) && isScheduled(daysMask, date.toEpochDay())) {
                return at.toInstant().toEpochMilli()
            }
        }
        return null
    }

    internal fun nextWater(intervalH: Int): Long {
        val step = intervalH.coerceAtLeast(1)
        val now = ZonedDateTime.now()
        for (offset in 0..1) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            var h = WATER_FIRST_HOUR
            while (h <= WATER_LAST_HOUR) {
                val at = date.atTime(h, 0).atZone(now.zone)
                if (at.isAfter(now.plusSeconds(20))) return at.toInstant().toEpochMilli()
                h += step
            }
        }
        return now.plusHours(step.toLong()).toInstant().toEpochMilli()
    }

    // ---------- PendingIntents ----------

    fun firePending(ctx: Context, type: String, id: Long): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("myview://fire/$type/$id"))
            .putExtra(EXTRA_TYPE, type)
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** A broadcast that runs [action] in [ReminderReceiver] (notification buttons and widget taps). */
    fun actionPending(ctx: Context, action: String, id: Long): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("myview://action/$action/$id"))
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun openAppPending(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // ---------- Receiving ----------

    suspend fun handle(ctx: Context, intent: Intent) {
        val repo = container(ctx).repository
        val settings = container(ctx).settings.settings.value
        val today = todayEpochDay()
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        when (intent.action) {
            ACTION_FIRE -> when (intent.getStringExtra(EXTRA_TYPE)) {
                TYPE_HABIT -> {
                    val habit = repo.habitById(id) ?: return
                    if (habit.reminderMin < 0) return
                    scheduleHabit(ctx, habit)
                    if (isScheduled(habit.days, today) && !repo.isHabitDone(id, today)) {
                        notifyHabit(ctx, habit)
                    }
                }
                TYPE_WATER -> {
                    if (!settings.waterReminder) return
                    scheduleWater(ctx, settings.waterIntervalH)
                    val total = repo.waterTotalFor(today)
                    if (total < settings.waterGoalMl) notifyWater(ctx, total, settings.waterGoalMl, settings.glassMl)
                }
                TYPE_WORKOUT -> {
                    if (!settings.workoutReminder) return
                    scheduleWorkout(ctx, settings.workoutReminderMin)
                    if (!repo.hasExerciseLogs(today)) notifyWorkout(ctx)
                }
            }
            ACTION_HABIT_DONE -> {
                repo.setHabitDone(id, today, true)
                NotificationManagerCompat.from(ctx).cancel(habitNotificationId(id))
                WidgetUpdater.updateAll(ctx)
            }
            ACTION_TOGGLE_HABIT -> {
                val done = repo.isHabitDone(id, today)
                repo.setHabitDone(id, today, !done)
                WidgetUpdater.updateAll(ctx)
            }
            ACTION_ADD_WATER -> {
                repo.addWater(today, settings.glassMl)
                NotificationManagerCompat.from(ctx).cancel(WATER_NOTIFICATION_ID)
                WidgetUpdater.updateAll(ctx)
            }
        }
    }

    // ---------- Notifications ----------

    private const val WATER_NOTIFICATION_ID = 1
    private const val WORKOUT_NOTIFICATION_ID = 2
    private fun habitNotificationId(id: Long) = 2000 + id.toInt()

    @SuppressLint("MissingPermission")
    private fun show(ctx: Context, notificationId: Int, builder: NotificationCompat.Builder) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        nm.notify(notificationId, builder.build())
    }

    private fun base(ctx: Context, channel: String, title: String, text: String) =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPending(ctx))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    private fun notifyHabit(ctx: Context, h: HabitEntity) {
        val b = base(ctx, CH_HABITS, h.name, "Time for your habit. Keep the streak going!")
            .addAction(0, "Mark done", actionPending(ctx, ACTION_HABIT_DONE, h.id))
        show(ctx, habitNotificationId(h.id), b)
    }

    private fun notifyWater(ctx: Context, total: Int, goal: Int, glass: Int) {
        val b = base(ctx, CH_WATER, "Time to drink water", "${formatMl(total)} of ${formatMl(goal)} so far today.")
            .addAction(0, "+$glass ml", actionPending(ctx, ACTION_ADD_WATER, 0))
        show(ctx, WATER_NOTIFICATION_ID, b)
    }

    private fun notifyWorkout(ctx: Context) {
        show(ctx, WORKOUT_NOTIFICATION_ID, base(ctx, CH_WORKOUT, "Workout time", "You have not logged any exercise today."))
    }
}
