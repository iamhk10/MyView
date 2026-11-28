package com.x.myview.widget

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface

/** Colours for the premium widgets (they follow the widget style chosen in Settings). */
internal class WColors(val card: Int, val ink: Int, val sub: Int, val track: Int, val accent: Int)

/** One ring of the activity rings widget. */
internal class RingSpec(val progress: Float, val c1: Int, val c2: Int, val value: String, val label: String)

/** One column of the "today" widget. */
internal class StatSpec(val label: String, val value: String, val progress: Float, val color: Int)

/** Picture widgets with a glow, a sheen and gradient shapes. Plain data in, bitmap out. */
internal object PremiumCards {

    private fun alpha(c: Int, a: Int): Int = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    /** Rounded card with a soft accent glow in the top corner, a light sheen and a hairline edge. */
    private fun base(w: Int, h: Int, c: WColors, radius: Float): Pair<Bitmap, Canvas> {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = c.card
        canvas.drawRect(rect, p)
        p.shader = RadialGradient(w * 0.9f, 0f, w * 0.75f, alpha(c.accent, 70), alpha(c.accent, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(rect, p)
        p.shader = RadialGradient(0f, h.toFloat(), w * 0.6f, alpha(c.accent, 34), alpha(c.accent, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(rect, p)
        p.shader = LinearGradient(0f, 0f, 0f, h * 0.5f, alpha(0xFFFFFFFF.toInt(), 22), 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRect(rect, p)
        canvas.restore()
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = alpha(c.ink, 34)
        }
        canvas.drawRoundRect(RectF(1f, 1f, w - 1f, h - 1f), radius, radius, edge)
        return Pair(bmp, canvas)
    }

    private fun text(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, spacing: Float = 0f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            letterSpacing = spacing
            typeface = if (bold) Typeface.create("sans-serif-condensed", Typeface.BOLD) else Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

    private fun glowArc(canvas: Canvas, rect: RectF, cx: Float, cy: Float, progress: Float, c1: Int, c2: Int, stroke: Float) {
        val p = progress.coerceIn(0f, 1f)
        if (p <= 0.001f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        val shader = SweepGradient(cx, cy, intArrayOf(c1, c2), floatArrayOf(0f, p.coerceAtLeast(0.02f)))
        val m = Matrix()
        m.postRotate(-90f, cx, cy)
        shader.setLocalMatrix(m)
        paint.shader = shader
        paint.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        paint.alpha = 140
        canvas.drawArc(rect, -90f, 360f * p, false, paint)
        paint.maskFilter = null
        paint.alpha = 255
        canvas.drawArc(rect, -90f, 360f * p, false, paint)
    }

    /** Three nested rings with the average in the middle and the three values underneath. */
    fun rings(c: WColors, rings: List<RingSpec>): Bitmap {
        val w = 480
        val h = 480
        val (bmp, canvas) = base(w, h, c, 76f)
        val cx = w / 2f
        val cy = 188f
        val stroke = 26f
        val radii = floatArrayOf(128f, 96f, 64f)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = c.track
        }
        rings.take(3).forEachIndexed { i, r ->
            val rad = radii[i]
            val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
            canvas.drawArc(rect, 0f, 360f, false, track)
            glowArc(canvas, rect, cx, cy, r.progress, r.c1, r.c2, stroke)
        }
        val avg = if (rings.isEmpty()) 0 else (rings.map { it.progress.coerceIn(0f, 1f) }.average() * 100).toInt()
        canvas.drawText("$avg%", cx, cy + 16f, text(c.ink, 46f, bold = true, align = Paint.Align.CENTER))
        val colW = w / 3f
        rings.take(3).forEachIndexed { i, r ->
            val x = colW * i + colW / 2f
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = r.c1 }
            canvas.drawCircle(x, 372f, 8f, dot)
            canvas.drawText(r.value, x, 418f, text(c.ink, 40f, bold = true, align = Paint.Align.CENTER))
            canvas.drawText(r.label, x, 450f, text(c.sub, 22f, align = Paint.Align.CENTER, spacing = 0.12f))
        }
        return bmp
    }

    /** Wide card: greeting, date, streak and four progress columns. */
    fun today(c: WColors, greeting: String, date: String, streak: Int, stats: List<StatSpec>): Bitmap {
        val w = 800
        val h = 360
        val (bmp, canvas) = base(w, h, c, 64f)
        val pad = 48f
        canvas.drawText(greeting.uppercase(), pad, 78f, text(c.sub, 24f, spacing = 0.2f))
        canvas.drawText(date, pad, 150f, text(c.ink, 66f, bold = true))
        if (streak > 0) {
            val pill = RectF(w - pad - 156f, 44f, w - pad, 108f)
            val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = alpha(c.accent, 56) }
            canvas.drawRoundRect(pill, 32f, 32f, pp)
            canvas.drawText("🔥 $streak", pill.centerX(), pill.centerY() + 14f, text(c.ink, 40f, bold = true, align = Paint.Align.CENTER))
        }
        val cols = stats.take(4)
        if (cols.isNotEmpty()) {
            val colW = (w - 2 * pad) / cols.size
            cols.forEachIndexed { i, s ->
                val x = pad + colW * i
                canvas.drawText(s.label, x, 232f, text(c.sub, 21f, spacing = 0.14f))
                canvas.drawText(s.value, x, 282f, text(c.ink, 44f, bold = true))
                val barW = colW - 26f
                val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.track }
                canvas.drawRoundRect(RectF(x, 304f, x + barW, 318f), 7f, 7f, bar)
                val p = s.progress.coerceIn(0f, 1f)
                if (p > 0.001f) {
                    bar.color = s.color
                    canvas.drawRoundRect(RectF(x, 304f, x + (barW * p).coerceAtLeast(14f), 318f), 7f, 7f, bar)
                }
            }
        }
        return bmp
    }

    /** Wide card with seven rounded bars, a big total and an optional dashed goal line. */
    fun weekBars(
        c: WColors,
        tag: String,
        total: String,
        note: String,
        values: List<Float>,
        labels: List<String>,
        goal: Float?,
    ): Bitmap {
        val w = 800
        val h = 360
        val (bmp, canvas) = base(w, h, c, 64f)
        val pad = 48f
        canvas.drawText(tag, pad, 70f, text(c.accent, 22f, spacing = 0.2f))
        canvas.drawText(total, pad, 138f, text(c.ink, 60f, bold = true))
        canvas.drawText(note, w - pad, 134f, text(c.sub, 24f, align = Paint.Align.RIGHT))

        val top = 168f
        val bottom = 292f
        val n = values.size.coerceAtLeast(1)
        val gap = 16f
        val bw = (w - 2 * pad - gap * (n - 1)) / n
        val maxV = maxOf(values.maxOrNull() ?: 0f, goal ?: 0f, 0.0001f)
        val trackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.track }
        values.forEachIndexed { i, v ->
            val x = pad + i * (bw + gap)
            canvas.drawRoundRect(RectF(x, top, x + bw, bottom), 14f, 14f, trackP)
            if (v > 0f) {
                val bh = ((bottom - top) * (v / maxV)).coerceAtLeast(18f)
                val last = i == values.lastIndex
                val barP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, bottom - bh, 0f, bottom,
                        if (last) c.accent else alpha(c.accent, 190), if (last) alpha(c.accent, 150) else alpha(c.accent, 110),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRoundRect(RectF(x, bottom - bh, x + bw, bottom), 14f, 14f, barP)
            }
            val lbl = labels.getOrElse(i) { "" }
            canvas.drawText(lbl, x + bw / 2f, 332f, text(if (i == values.lastIndex) c.ink else c.sub, 24f, bold = i == values.lastIndex, align = Paint.Align.CENTER))
        }
        if (goal != null && goal > 0f) {
            val y = bottom - (bottom - top) * (goal / maxV)
            val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = alpha(c.ink, 150)
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }
            canvas.drawLine(pad, y, w - pad, y, dash)
        }
        return bmp
    }
}
