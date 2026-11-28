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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import com.x.myview.domain.quoteFor
import com.x.myview.domain.scheduledHabits
import com.x.myview.reminders.Reminders
import com.x.myview.run.RunRecord
import com.x.myview.run.RunStore
import com.x.myview.run.distLabel
import com.x.myview.util.formatDuration
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
class StreakWidget : BaseWidget()
class WorkoutWidget : BaseWidget()
class QuoteWidget : BaseWidget()
class RingsWidget : BaseWidget()
class TodayWidget : BaseWidget()
class RunWeekWidget : BaseWidget()
class StepsWeekWidget : BaseWidget()

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
    val streak: Int,
    val weekActive: List<Boolean>,
    val goalsDone: Int,
    val exerciseCount: Int,
    /** Run and walk metres per day for the last 7 days (today last). */
    val runWeekM: List<Float>,
    val runWeekCount: Int,
    val lastRun: RunRecord?,
    val stepsWeek: List<Int>,
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
        val streakIds = mgr.getAppWidgetIds(ComponentName(app, StreakWidget::class.java))
        val workoutIds = mgr.getAppWidgetIds(ComponentName(app, WorkoutWidget::class.java))
        val quoteIds = mgr.getAppWidgetIds(ComponentName(app, QuoteWidget::class.java))
        val ringsIds = mgr.getAppWidgetIds(ComponentName(app, RingsWidget::class.java))
        val todayIds = mgr.getAppWidgetIds(ComponentName(app, TodayWidget::class.java))
        val runWeekIds = mgr.getAppWidgetIds(ComponentName(app, RunWeekWidget::class.java))
        val stepsWeekIds = mgr.getAppWidgetIds(ComponentName(app, StepsWeekWidget::class.java))
        if (habitIds.isEmpty() && waterIds.isEmpty() && summaryIds.isEmpty() && stepsIds.isEmpty() &&
            streakIds.isEmpty() && workoutIds.isEmpty() && quoteIds.isEmpty() &&
            ringsIds.isEmpty() && todayIds.isEmpty() && runWeekIds.isEmpty() && stepsWeekIds.isEmpty()
        ) return

        if (syncSteps && (summaryIds.isNotEmpty() || stepsIds.isNotEmpty() || ringsIds.isNotEmpty() || todayIds.isNotEmpty() || stepsWeekIds.isNotEmpty())) {
            val tracker = (app as MyViewApp).container.stepTracker
            if (tracker.isSupported && !tracker.isLive) tracker.readOnce(1500)
        }
        val snap = load(app)
        if (habitIds.isNotEmpty()) mgr.updateAppWidget(habitIds, habitsViews(app, snap))
        if (waterIds.isNotEmpty()) mgr.updateAppWidget(waterIds, waterViews(app, snap))
        if (summaryIds.isNotEmpty()) mgr.updateAppWidget(summaryIds, summaryViews(app, snap))
        if (stepsIds.isNotEmpty()) mgr.updateAppWidget(stepsIds, stepsViews(app, snap))
        if (streakIds.isNotEmpty()) mgr.updateAppWidget(streakIds, cardViews(app, streakBitmap(snap)))
        if (workoutIds.isNotEmpty()) mgr.updateAppWidget(workoutIds, cardViews(app, workoutBitmap(snap)))
        if (quoteIds.isNotEmpty()) mgr.updateAppWidget(quoteIds, cardViews(app, quoteBitmap(snap)))
        if (ringsIds.isNotEmpty()) mgr.updateAppWidget(ringsIds, cardViews(app, ringsBitmap(snap)))
        if (todayIds.isNotEmpty()) mgr.updateAppWidget(todayIds, cardViews(app, todayBitmap(snap)))
        if (runWeekIds.isNotEmpty()) mgr.updateAppWidget(runWeekIds, cardViews(app, runWeekBitmap(snap)))
        if (stepsWeekIds.isNotEmpty()) mgr.updateAppWidget(stepsWeekIds, cardViews(app, stepsWeekBitmap(snap)))
    }

    private suspend fun load(ctx: Context): Snapshot {
        val c = (ctx.applicationContext as MyViewApp).container
        val repo = c.repository
        val today = todayEpochDay()
        val exercises = repo.exercises.first()
        val logs = repo.exerciseLogs.first()
        val repExercises = exercises.filter { it.kind == KIND_REPS }
        val reps = repExercises.sumOf { exerciseTotal(logs, it.id, today) }
        val doneMap = repo.habitDone.first()
        // A day counts when anything was done: a habit ticked or an exercise saved.
        val active = HashSet<Long>()
        doneMap.values.forEach { active.addAll(it) }
        logs.forEach { active.add(it.day) }
        var streak = 0
        var d = if (today in active) today else today - 1
        while (d in active) {
            streak++
            d--
        }
        val runs = RunStore.all(ctx)
        val zone = java.time.ZoneId.systemDefault()
        val perDay = HashMap<Long, Float>()
        runs.forEach { r ->
            val day = java.time.Instant.ofEpochMilli(r.startedAt).atZone(zone).toLocalDate().toEpochDay()
            perDay[day] = (perDay[day] ?: 0f) + r.distanceM
        }
        val runWeek = (today - 6..today).map { perDay[it] ?: 0f }
        val runsThisWeek = runs.count {
            java.time.Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate().toEpochDay() in (today - 6..today)
        }
        return Snapshot(
            today = today,
            settings = c.settings.settings.value,
            planned = scheduledHabits(repo.habits.first(), today),
            doneByHabit = doneMap,
            waterMl = repo.waterTotalFor(today),
            reps = reps,
            repsGoal = repExercises.sumOf { it.dailyGoal },
            stepsSupported = c.stepTracker.isSupported,
            steps = c.stepTracker.stepsFor(today),
            streak = streak,
            weekActive = (today - 6..today).map { it in active },
            goalsDone = exercises.count { it.dailyGoal > 0 && exerciseTotal(logs, it.id, today) >= it.dailyGoal },
            exerciseCount = exercises.size,
            runWeekM = runWeek,
            runWeekCount = runsThisWeek,
            lastRun = runs.firstOrNull(),
            stepsWeek = (today - 6..today).map { c.stepTracker.stepsFor(it) },
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
        v.setImageViewBitmap(R.id.st_card, stepsCardBitmap(snap.steps, goal, snap.stepsSupported, pal(snap)))
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
    private fun stepsCardBitmap(steps: Int, goal: Int, supported: Boolean, pal: Pal): Bitmap {
        val w = 480
        val h = 480
        val blue = pal.accent
        val ink = pal.ink
        val grey = pal.sub
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val panelW = w * 0.41f

        // Card shape: rounded square, white, with the blue panel clipped to its left side.
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 76f, 76f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        paint.color = pal.card
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.color = pal.panel
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
            color = pal.track
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
        paint.color = pal.track
        canvas.drawCircle(cx, cy, 30f, paint)
        paint.color = pal.card
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

    // ---------- Picture-style widgets: palette, streak, workout, quote ----------

    private class Pal(
        val card: Int,
        val panel: Int,
        val ink: Int,
        val sub: Int,
        val track: Int,
        val accent: Int,
    )

    private fun shade(c: Int, f: Float): Int {
        val r = (((c shr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
        val g = (((c shr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) * f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Colours for the picture widgets. Style 0 = light, 1 = dark, 2 = accent colour (set in Settings). */
    private fun pal(snap: Snapshot): Pal {
        val acc = accent(snap.settings)
        return when (snap.settings.widgetStyle) {
            1 -> Pal(0xFF15181E.toInt(), acc, 0xFFFFFFFF.toInt(), 0xFF9AA0AB.toInt(), 0xFF2A2F38.toInt(), acc)
            2 -> Pal(acc, shade(acc, 0.72f), 0xFFFFFFFF.toInt(), 0xCCFFFFFF.toInt(), 0x40FFFFFF, 0xFFFFFFFF.toInt())
            else -> Pal(0xFFFFFFFF.toInt(), 0xFF4A6CFF.toInt(), 0xFF3A3A3F.toInt(), 0xFF9A9AA2.toInt(), 0xFFEFEFF3.toInt(), 0xFF4A6CFF.toInt())
        }
    }

    private fun cardViews(ctx: Context, bmp: Bitmap): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_card)
        v.setOnClickPendingIntent(R.id.w_root, Reminders.openAppPending(ctx))
        v.setImageViewBitmap(R.id.card_img, bmp)
        return v
    }

    private fun roundedCard(w: Int, h: Int, color: Int, radius: Float): Pair<Bitmap, Canvas> {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, paint)
        return Pair(bmp, canvas)
    }

    /** Day streak: a flame, the number of days, and the last 7 days as dots. */
    private fun streakBitmap(snap: Snapshot): Bitmap {
        val p = pal(snap)
        val w = 480
        val h = 480
        val (bmp, canvas) = roundedCard(w, h, p.card, 76f)
        val cx = w / 2f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.sub
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = 26f
            letterSpacing = 0.2f
        }
        canvas.drawText("STREAK", cx, h * 0.15f, label)
        val flame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 84f
        }
        canvas.drawText("\uD83D\uDD25", cx, h * 0.34f, flame)
        val num = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.ink
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 150f
        }
        canvas.drawText(snap.streak.toString(), cx, h * 0.62f, num)
        label.letterSpacing = 0f
        label.textSize = 28f
        label.color = p.ink
        canvas.drawText(if (snap.streak == 1) "day in a row" else "days in a row", cx, h * 0.72f, label)
        // Last 7 days, oldest on the left, today on the right (ringed).
        val dot = Paint(Paint.ANTI_ALIAS_FLAG)
        val gap = 54f
        val startX = cx - gap * 3
        for (i in 0 until 7) {
            val x = startX + gap * i
            val on = snap.weekActive.getOrElse(i) { false }
            dot.style = Paint.Style.FILL
            dot.color = if (on) p.accent else p.track
            canvas.drawCircle(x, h * 0.86f, 15f, dot)
            if (i == 6) {
                dot.style = Paint.Style.STROKE
                dot.strokeWidth = 4f
                dot.color = p.accent
                canvas.drawCircle(x, h * 0.86f, 23f, dot)
            }
        }
        return bmp
    }

    /** Reps today as a ring, with how many exercise goals are done. */
    private fun workoutBitmap(snap: Snapshot): Bitmap {
        val p = pal(snap)
        val w = 480
        val h = 480
        val (bmp, canvas) = roundedCard(w, h, p.card, 76f)
        val cx = w / 2f
        val cy = h * 0.5f
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.sub
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            textSize = 26f
            letterSpacing = 0.2f
        }
        canvas.drawText("WORKOUT", cx, h * 0.13f, label)
        val r = 118f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 26f
            strokeCap = Paint.Cap.ROUND
            color = p.track
        }
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rect, 0f, 360f, false, ring)
        val progress = if (snap.repsGoal > 0) (snap.reps.toFloat() / snap.repsGoal).coerceIn(0f, 1f) else 0f
        if (progress > 0.001f) {
            ring.color = p.accent
            canvas.drawArc(rect, -90f, 360f * progress, false, ring)
        }
        val num = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.ink
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 96f
        }
        canvas.drawText(compact(snap.reps), cx, cy + 22f, num)
        label.letterSpacing = 0.1f
        label.textSize = 24f
        canvas.drawText("REPS", cx, cy + 60f, label)
        label.letterSpacing = 0f
        label.textSize = 28f
        label.color = p.ink
        canvas.drawText("${snap.goalsDone} of ${snap.exerciseCount} goals done", cx, h * 0.93f, label)
        return bmp
    }

    /** The quote of the day, wrapped to fit a wide card. */
    private fun quoteBitmap(snap: Snapshot): Bitmap {
        val p = pal(snap)
        val w = 800
        val h = 360
        val (bmp, canvas) = roundedCard(w, h, p.card, 64f)
        val pad = 48f
        val tag = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.accent
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = 24f
            letterSpacing = 0.2f
        }
        canvas.drawText("TODAY", pad, 70f, tag)
        val text = quoteFor(snap.today)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.ink
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        val maxW = (w - 2 * pad).toInt()
        val maxH = h - 170
        fun build(sz: Float): StaticLayout {
            tp.textSize = sz
            return StaticLayout.Builder.obtain(text, 0, text.length, tp, maxW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .build()
        }
        var size = 46f
        var layout = build(size)
        while (layout.height > maxH && size > 24f) {
            size -= 2f
            layout = build(size)
        }
        canvas.save()
        canvas.translate(pad, 96f)
        layout.draw(canvas)
        canvas.restore()
        val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.sub
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("MyView", w - pad, h - 36f, foot)
        return bmp
    }

    // ---------- Premium widgets (glow + gradient cards) ----------

    private fun wcolors(snap: Snapshot): WColors {
        val p = pal(snap)
        return WColors(p.card, p.ink, p.sub, p.track, p.accent)
    }

    private fun dayLetters(): List<String> {
        val today = java.time.LocalDate.now()
        return (6 downTo 0).map {
            today.minusDays(it.toLong()).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())
        }
    }

    /** Three nested rings: steps (or habits when the phone has no step sensor), water, reps. */
    private fun ringsBitmap(snap: Snapshot): Bitmap {
        val goalWater = snap.settings.waterGoalMl.coerceAtLeast(1)
        val habitsDone = snap.planned.count { snap.today in (snap.doneByHabit[it.id] ?: emptySet()) }
        val first = if (snap.stepsSupported) {
            RingSpec(snap.steps.toFloat() / snap.settings.stepGoal.coerceAtLeast(1), 0xFF2DD4BF.toInt(), 0xFF5EEAD4.toInt(), compact(snap.steps), "STEPS")
        } else {
            RingSpec(if (snap.planned.isEmpty()) 0f else habitsDone.toFloat() / snap.planned.size, 0xFFFF6FB5.toInt(), 0xFFFFA3D1.toInt(), "$habitsDone/${snap.planned.size}", "HABITS")
        }
        val water = RingSpec(
            snap.waterMl.toFloat() / goalWater, 0xFF3FA9FF.toInt(), 0xFF8E7CFF.toInt(),
            if (snap.waterMl >= 1000) String.format(java.util.Locale.US, "%.1fL", snap.waterMl / 1000f) else "${snap.waterMl}ml", "WATER",
        )
        val reps = RingSpec(
            if (snap.repsGoal > 0) snap.reps.toFloat() / snap.repsGoal else 0f, 0xFFFF8A3D.toInt(), 0xFFFFC857.toInt(), compact(snap.reps), "REPS",
        )
        return PremiumCards.rings(wcolors(snap), listOf(first, water, reps))
    }

    private fun todayBitmap(snap: Snapshot): Bitmap {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 5 -> "Good night"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            hour < 21 -> "Good evening"
            else -> "Good night"
        }
        val date = java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date())
        val goalWater = snap.settings.waterGoalMl.coerceAtLeast(1)
        val habitsDone = snap.planned.count { snap.today in (snap.doneByHabit[it.id] ?: emptySet()) }
        val stats = ArrayList<StatSpec>()
        if (snap.stepsSupported) {
            stats.add(StatSpec("STEPS", compact(snap.steps), snap.steps.toFloat() / snap.settings.stepGoal.coerceAtLeast(1), 0xFF2DD4BF.toInt()))
        }
        stats.add(
            StatSpec(
                "WATER",
                if (snap.waterMl >= 1000) String.format(java.util.Locale.US, "%.1fL", snap.waterMl / 1000f) else "${snap.waterMl}ml",
                snap.waterMl.toFloat() / goalWater, 0xFF3FA9FF.toInt(),
            ),
        )
        stats.add(StatSpec("REPS", compact(snap.reps), if (snap.repsGoal > 0) snap.reps.toFloat() / snap.repsGoal else 0f, 0xFFFF8A3D.toInt()))
        stats.add(StatSpec("HABITS", "$habitsDone/${snap.planned.size}", if (snap.planned.isEmpty()) 0f else habitsDone.toFloat() / snap.planned.size, 0xFFFF6FB5.toInt()))
        return PremiumCards.today(wcolors(snap), greeting, date, snap.streak, stats)
    }

    private fun runWeekBitmap(snap: Snapshot): Bitmap {
        val total = snap.runWeekM.sum()
        val last = snap.lastRun
        val note = if (last == null) "No runs yet" else "Last: ${distLabel(last.distanceM)} in ${formatDuration(last.durationSec)}"
        val totalText = if (total < 1000f) "${total.toInt()} m" else String.format(java.util.Locale.US, "%.1f km", total / 1000f)
        return PremiumCards.weekBars(
            wcolors(snap), "RUN AND WALK  \u2022  7 DAYS", totalText, note,
            snap.runWeekM, dayLetters(), null,
        )
    }

    private fun stepsWeekBitmap(snap: Snapshot): Bitmap {
        val total = snap.stepsWeek.sum()
        val goal = snap.settings.stepGoal
        val note = if (snap.stepsSupported) "Goal ${String.format(java.util.Locale.US, "%,d", goal)} a day" else "No step sensor"
        return PremiumCards.weekBars(
            wcolors(snap), "STEPS  \u2022  7 DAYS", if (snap.stepsSupported) "${compact(total)} steps" else "--", note,
            snap.stepsWeek.map { it.toFloat() }, dayLetters(), if (snap.stepsSupported) goal.toFloat() else null,
        )
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
