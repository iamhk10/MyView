package com.x.myview.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
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
    val repsGoal: Int,
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
        val repExercises = exercises.filter { it.kind == KIND_REPS }
        val reps = repExercises.sumOf { exerciseTotal(logs, it.id, today) }
        return Snapshot(
            today = today,
            settings = c.settings.settings.value,
            planned = scheduledHabits(repo.habits.first(), today),
            doneByHabit = repo.habitDone.first(),
            waterMl = repo.waterTotalFor(today),
            reps = reps,
            repsGoal = repExercises.sumOf { it.dailyGoal },
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
        val habitProgress = if (snap.planned.isEmpty()) 0f else doneCount.toFloat() / snap.planned.size
        v.setImageViewBitmap(R.id.h_bar, barBitmap(habitProgress, accent(snap.settings)))

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
                v.setTextColor(names[i], if (done) 0x80FFFFFF.toInt() else 0xFFFFFFFF.toInt())
                v.setTextViewText(streaks[i], if (streak > 0) "\uD83D\uDD25 $streak" else "")
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
        v.setImageViewBitmap(R.id.w_ring, ringBitmap(progress, 0xFF3FA9FF.toInt(), 0xFF8E7CFF.toInt()))
        v.setTextViewText(R.id.w_percent, "${(progress * 100).toInt().coerceAtMost(999)}%")
        v.setTextViewText(R.id.w_amount, formatMl(snap.waterMl))
        v.setTextViewText(
            R.id.w_left,
            if (snap.waterMl >= goal) "Goal reached" else "${formatMl(goal - snap.waterMl)} to go of ${formatMl(goal)}",
        )
        v.setTextViewText(R.id.w_add, "+ ${snap.settings.glassMl} ml")
        v.setOnClickPendingIntent(R.id.w_add, Reminders.actionPending(ctx, Reminders.ACTION_ADD_WATER, 0))
        return v
    }

    // ---------- Summary ----------

    private fun summaryViews(ctx: Context, snap: Snapshot): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_summary)
        v.setOnClickPendingIntent(R.id.w_root, Reminders.openAppPending(ctx))
        val goalWater = snap.settings.waterGoalMl.coerceAtLeast(1)
        val habitsDone = snap.planned.count { snap.today in (snap.doneByHabit[it.id] ?: emptySet()) }

        if (snap.stepsSupported) {
            v.setViewVisibility(R.id.s_box_steps, android.view.View.VISIBLE)
            v.setImageViewBitmap(
                R.id.s_ring_steps,
                ringBitmap(snap.steps.toFloat() / snap.settings.stepGoal.coerceAtLeast(1), 0xFF2DD4BF.toInt(), 0xFF5EEAD4.toInt()),
            )
            v.setTextViewText(R.id.s_val_steps, compact(snap.steps))
        } else {
            v.setViewVisibility(R.id.s_box_steps, android.view.View.GONE)
        }
        v.setImageViewBitmap(R.id.s_ring_water, ringBitmap(snap.waterMl.toFloat() / goalWater, 0xFF3FA9FF.toInt(), 0xFF8E7CFF.toInt()))
        v.setTextViewText(R.id.s_val_water, if (snap.waterMl >= 1000) String.format(java.util.Locale.US, "%.1fL", snap.waterMl / 1000f) else "${snap.waterMl}")
        v.setImageViewBitmap(
            R.id.s_ring_reps,
            ringBitmap(if (snap.repsGoal > 0) snap.reps.toFloat() / snap.repsGoal else 0f, 0xFFFF8A3D.toInt(), 0xFFFFC857.toInt()),
        )
        v.setTextViewText(R.id.s_val_reps, compact(snap.reps))
        val accentColor = accent(snap.settings)
        v.setImageViewBitmap(
            R.id.s_ring_habits,
            ringBitmap(if (snap.planned.isEmpty()) 0f else habitsDone.toFloat() / snap.planned.size, accentColor, 0xFFFF6FB5.toInt()),
        )
        v.setTextViewText(R.id.s_val_habits, "$habitsDone/${snap.planned.size}")
        return v
    }

    /** 8200 -> "8.2k", 12400 -> "12k", small numbers stay as they are. */
    private fun compact(n: Int): String = when {
        n >= 10_000 -> "${n / 1000}k"
        n >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", n / 1000f)
        else -> n.toString()
    }

    /** A glowing gradient ring. [progress] 0..1 (more than 1 is shown as a full ring). */
    private fun ringBitmap(progress: Float, colorStart: Int, colorEnd: Int): Bitmap {
        val size = 240
        val stroke = 26f
        val pad = stroke / 2 + 14f
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rect = RectF(pad, pad, size - pad, size - pad)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = 0x24FFFFFF
        }
        canvas.drawArc(rect, 0f, 360f, false, paint)
        val p = progress.coerceIn(0f, 1f)
        if (p > 0.001f) {
            val shader = SweepGradient(size / 2f, size / 2f, intArrayOf(colorStart, colorEnd), floatArrayOf(0f, p.coerceAtLeast(0.02f)))
            val m = Matrix()
            m.postRotate(-90f, size / 2f, size / 2f)
            shader.setLocalMatrix(m)
            paint.shader = shader
            // Soft glow under the bright arc.
            paint.maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
            paint.alpha = 150
            canvas.drawArc(rect, -90f, 360f * p, false, paint)
            paint.maskFilter = null
            paint.alpha = 255
            canvas.drawArc(rect, -90f, 360f * p, false, paint)
        }
        return bmp
    }

    /** A rounded progress bar with a gradient fill, drawn wide so it stays sharp when stretched. */
    private fun barBitmap(progress: Float, color: Int): Bitmap {
        val w = 720
        val h = 20
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x24FFFFFF
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), h / 2f, h / 2f, paint)
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            val fillW = (w * p).coerceAtLeast(h.toFloat())
            paint.shader = android.graphics.LinearGradient(0f, 0f, fillW, 0f, color, 0xFFFF6FB5.toInt(), android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(0f, 0f, fillW, h.toFloat()), h / 2f, h / 2f, paint)
        }
        return bmp
    }
}
