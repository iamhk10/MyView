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
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
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
class StepsWidget : BaseWidget()

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
        val stepsIds = mgr.getAppWidgetIds(ComponentName(app, StepsWidget::class.java))
        if (habitIds.isEmpty() && waterIds.isEmpty() && summaryIds.isEmpty() && stepsIds.isEmpty()) return

        if (syncSteps && (summaryIds.isNotEmpty() || stepsIds.isNotEmpty())) {
            val tracker = (app as MyViewApp).container.stepTracker
            if (tracker.isSupported && !tracker.isLive) tracker.readOnce(1500)
        }
        val snap = load(app)
        if (habitIds.isNotEmpty()) mgr.updateAppWidget(habitIds, habitsViews(app, snap))
        if (waterIds.isNotEmpty()) mgr.updateAppWidget(waterIds, waterViews(app, snap))
        if (summaryIds.isNotEmpty()) mgr.updateAppWidget(summaryIds, summaryViews(app, snap))
        if (stepsIds.isNotEmpty()) mgr.updateAppWidget(stepsIds, stepsViews(app, snap))
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

    // ---------- Steps only ----------

    private fun stepsViews(ctx: Context, snap: Snapshot): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_steps)
        v.setOnClickPendingIntent(R.id.w_root, Reminders.openAppPending(ctx))
        val goal = snap.settings.stepGoal.coerceAtLeast(1)
        v.setImageViewBitmap(R.id.st_card, stepsCardBitmap(snap.steps, goal, snap.stepsSupported))
        v.setContentDescription(
            R.id.st_card,
            if (snap.stepsSupported) "${snap.steps} steps today, goal $goal" else "No step sensor on this phone",
        )
        return v
    }

    /**
     * The whole "steps only" card as one picture: a blue panel with the step count stacked in two
     * lines, and a white panel with a dial whose hand and arc show progress towards the goal.
     */
    private fun stepsCardBitmap(steps: Int, goal: Int, supported: Boolean): Bitmap {
        val w = 480
        val h = 480
        val blue = 0xFF4A6CFF.toInt()
        val ink = 0xFF3A3A3F.toInt()
        val grey = 0xFF9A9AA2.toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val panelW = w * 0.41f

        // Card shape: rounded square, white, with the blue panel clipped to its left side.
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 76f, 76f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.color = blue
        canvas.drawRect(0f, 0f, panelW, h.toFloat(), paint)
        canvas.restore()

        // Stacked digits (like a clock's hour over minute): 8240 -> 82 / 40, 523 -> 05 / 23, 12345 -> 123 / 45.
        val digits = if (supported) steps.coerceAtLeast(0).toString().padStart(4, '0') else "----"
        val top = digits.substring(0, (digits.length + 1) / 2)
        val bottom = digits.substring((digits.length + 1) / 2)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = Typeface.create("sans-serif-condensed-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        var size = 190f
        val maxW = panelW - 44f
        text.textSize = size
        while (size > 40f && maxOf(text.measureText(top), text.measureText(bottom)) > maxW) {
            size -= 4f
            text.textSize = size
        }
        val b = Rect()
        text.getTextBounds("0", 0, 1, b)
        val lineH = b.height().toFloat()
        val gap = h * 0.07f
        val startY = (h - (2 * lineH + gap)) / 2f + lineH
        canvas.drawText(top, panelW / 2f, startY, text)
        canvas.drawText(bottom, panelW / 2f, startY + lineH + gap, text)

        // Dial on the white panel.
        val cx = panelW + (w - panelW) / 2f
        val cy = h * 0.5f
        val radius = 92f
        val progress = if (supported) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 16f
            strokeCap = Paint.Cap.ROUND
            color = 0xFFEFEFF3.toInt()
        }
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, 0f, 360f, false, ring)
        if (progress > 0.001f) {
            ring.color = blue
            canvas.drawArc(rect, -90f, 360f * progress, false, ring)
        }

        // Hand points at the progress angle (12 o'clock = 0, full circle = goal reached).
        val ang = Math.toRadians((-90.0 + 360.0 * progress))
        val dx = Math.cos(ang).toFloat()
        val dy = Math.sin(ang).toFloat()
        val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = ink
            strokeWidth = 7f
        }
        canvas.drawLine(cx, cy, cx + dx * (radius - 34f), cy + dy * (radius - 34f), hand)
        // Short thick blue marker on the ring end, like the reference's hour hand.
        hand.color = blue
        hand.strokeWidth = 20f
        canvas.drawLine(cx + dx * (radius - 30f), cy + dy * (radius - 30f), cx + dx * (radius - 8f), cy + dy * (radius - 8f), hand)
        // Hub.
        paint.color = 0xFFEFEFF3.toInt()
        canvas.drawCircle(cx, cy, 30f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(cx, cy, 10f, paint)

        // Labels.
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = grey
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = 26f
            letterSpacing = 0.2f
        }
        canvas.drawText("STEPS", cx, h * 0.15f, label)
        label.letterSpacing = 0f
        label.textSize = 28f
        label.color = ink
        val foot = if (supported) "${(steps.toLong() * 100 / goal).coerceAtMost(999)}% of ${String.format(java.util.Locale.US, "%,d", goal)}" else "No step sensor"
        canvas.drawText(foot, cx, h * 0.89f, label)
        return bmp
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
