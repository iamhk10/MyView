package com.x.myview.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.x.myview.MyViewApp
import com.x.myview.R
import com.x.myview.data.AppSettings
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.KIND_REPS
import com.x.myview.domain.currentStreak
import com.x.myview.domain.exerciseTotal
import com.x.myview.domain.scheduledHabits
import com.x.myview.reminders.Reminders
import com.x.myview.ui.theme.ACCENTS
import com.x.myview.util.formatMl
import com.x.myview.util.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat

/** Base class: every widget refreshes all widgets from the database when Android asks for an update. */
abstract class BaseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdater.updateAll(app)
            } catch (_: Exception) {
                // Ignore: the widget keeps its old content until the next update.
            } finally {
                pending.finish()
            }
        }
    }
}

class HabitsWidget : BaseWidget()
class WaterWidget : BaseWidget()
class SummaryWidget : BaseWidget()

private class Snapshot(
    val today: Long,
    val settings: AppSettings,
    val planned: List<HabitEntity>,
    val doneByHabit: Map<Long, Set<Long>>,
    val waterMl: Int,
    val reps: Int,
    val stepsSupported: Boolean,
    val steps: Int,
)

object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateAllAsync(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch { updateAll(app) }
    }

    /**
     * [syncSteps] reads the step sensor once first, so the widget never shows an old step count even
     * if nothing has been listening. The live step service passes false (it is already listening).
     */
    suspend fun updateAll(ctx: Context, syncSteps: Boolean = true) {
        val app = ctx.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        val habitIds = mgr.getAppWidgetIds(ComponentName(app, HabitsWidget::class.java))
        val waterIds = mgr.getAppWidgetIds(ComponentName(app, WaterWidget::class.java))
        val summaryIds = mgr.getAppWidgetIds(ComponentName(app, SummaryWidget::class.java))
        if (habitIds.isEmpty() && waterIds.isEmpty() && summaryIds.isEmpty()) return

        if (syncSteps && summaryIds.isNotEmpty()) {
            val tracker = (app as MyViewApp).container.stepTracker
            if (tracker.isSupported && !tracker.isLive) tracker.readOnce(1500)
        }
        val snap = load(app)
        if (habitIds.isNotEmpty()) mgr.updateAppWidget(habitIds, habitsViews(app, snap))
        if (waterIds.isNotEmpty()) mgr.updateAppWidget(waterIds, waterViews(app, snap))
        if (summaryIds.isNotEmpty()) mgr.updateAppWidget(summaryIds, summaryViews(app, snap))
    }

    private suspend fun load(ctx: Context): Snapshot {
        val c = (ctx.applicationContext as MyViewApp).container
        val repo = c.repository
        val today = todayEpochDay()
        val exercises = repo.exercises.first()
        val logs = repo.exerciseLogs.first()
        val reps = exercises.filter { it.kind == KIND_REPS }.sumOf { exerciseTotal(logs, it.id, today) }
        return Snapshot(
            today = today,
            settings = c.settings.settings.value,
            planned = scheduledHabits(repo.habits.first(), today),
            doneByHabit = repo.habitDone.first(),
            waterMl = repo.waterTotalFor(today),
            reps = reps,
            stepsSupported = c.stepTracker.isSupported,
            steps = c.stepTracker.stepsFor(today),
        )
    }

    private fun accent(s: AppSettings): Int = ACCENTS[s.accentIndex.coerceIn(0, ACCENTS.lastIndex)].color.toArgb()

    // ---------- Habits ----------

    private fun habitsViews(ctx: Context, snap: Snapshot): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_habits)
        val open = Reminders.openAppPending(ctx)
        v.setOnClickPendingIntent(R.id.w_root, open)
        v.setOnClickPendingIntent(R.id.h_header, open)

        fun isDone(h: HabitEntity) = snap.today in (snap.doneByHabit[h.id] ?: emptySet())
        val doneCount = snap.planned.count { isDone(it) }
        v.setTextViewText(R.id.h_count, "$doneCount / ${snap.planned.size}")

        val rows = intArrayOf(R.id.h_row1, R.id.h_row2, R.id.h_row3, R.id.h_row4)
        val checks = intArrayOf(R.id.h_check1, R.id.h_check2, R.id.h_check3, R.id.h_check4)
        val names = intArrayOf(R.id.h_name1, R.id.h_name2, R.id.h_name3, R.id.h_name4)
        val streaks = intArrayOf(R.id.h_streak1, R.id.h_streak2, R.id.h_streak3, R.id.h_streak4)

        // Habits still to do come first, so the widget always shows what is left.
        val shown = snap.planned.sortedBy { isDone(it) }.take(rows.size)
        for (i in rows.indices) {
            if (i < shown.size) {
                val h = shown[i]
                val done = isDone(h)
                val streak = currentStreak(h.days, snap.doneByHabit[h.id] ?: emptySet(), snap.today)
                v.setViewVisibility(rows[i], android.view.View.VISIBLE)
                v.setImageViewResource(checks[i], if (done) R.drawable.ic_widget_done else R.drawable.ic_widget_todo)
                v.setTextViewText(names[i], h.name)
                v.setTextViewText(streaks[i], if (streak > 0) "🔥 $streak" else "")
                v.setOnClickPendingIntent(rows[i], Reminders.actionPending(ctx, Reminders.ACTION_TOGGLE_HABIT, h.id))
            } else {
                v.setViewVisibility(rows[i], android.view.View.GONE)
            }
        }

        val more = snap.planned.size - shown.size
        if (more > 0) {
            v.setViewVisibility(R.id.h_more, android.view.View.VISIBLE)
            v.setTextViewText(R.id.h_more, "+$more more in the app")
        } else {
            v.setViewVisibility(R.id.h_more, android.view.View.GONE)
        }
        if (snap.planned.isEmpty()) {
            v.setViewVisibility(R.id.h_empty, android.view.View.VISIBLE)
            v.setTextViewText(R.id.h_empty, "Nothing planned today. Tap to open MyView.")
        } else {
            v.setViewVisibility(R.id.h_empty, android.view.View.GONE)
        }
        return v
    }

    // ---------- Water ----------

    private fun waterViews(ctx: Context, snap: Snapshot): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_water)
        val goal = snap.settings.waterGoalMl.coerceAtLeast(1)
        val progress = snap.waterMl.toFloat() / goal
        v.setOnClickPendingIntent(R.id.w_root, Reminders.openAppPending(ctx))
        v.setImageViewBitmap(R.id.w_ring, ringBitmap(progress, 0xFF4DB8FF.toInt()))
        v.setTextViewText(R.id.w_percent, "${(progress * 100).toInt().coerceAtMost(999)}%")
        v.setTextViewText(R.id.w_amount, "${formatMl(snap.waterMl)} / ${formatMl(goal)}")
        v.setTextViewText(R.id.w_add, "+ ${snap.settings.glassMl} ml")
        v.setOnClickPendingIntent(R.id.w_add, Reminders.actionPending(ctx, Reminders.ACTION_ADD_WATER, 0))
        return v
    }

    // ---------- Summary ----------

    private fun summaryViews(ctx: Context, snap: Snapshot): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_summary)
        v.setOnClickPendingIntent(R.id.w_root, Reminders.openAppPending(ctx))
        val nf = NumberFormat.getIntegerInstance()
        if (snap.stepsSupported) {
            v.setViewVisibility(R.id.s_box_steps, android.view.View.VISIBLE)
            v.setTextViewText(R.id.s_val_steps, nf.format(snap.steps))
        } else {
            v.setViewVisibility(R.id.s_box_steps, android.view.View.GONE)
        }
        v.setTextViewText(R.id.s_val_water, formatMl(snap.waterMl))
        v.setTextViewText(R.id.s_val_reps, nf.format(snap.reps))
        val done = snap.planned.count { snap.today in (snap.doneByHabit[it.id] ?: emptySet()) }
        v.setTextViewText(R.id.s_val_habits, "$done/${snap.planned.size}")
        return v
    }

    private fun ringBitmap(progress: Float, color: Int): Bitmap {
        val size = 200
        val stroke = 24f
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        val rect = RectF(stroke / 2, stroke / 2, size - stroke / 2, size - stroke / 2)
        paint.color = 0x33FFFFFF
        canvas.drawArc(rect, 0f, 360f, false, paint)
        if (progress > 0f) {
            paint.color = color
            canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0f, 1f), false, paint)
        }
        return bmp
    }
}
