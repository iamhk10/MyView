package com.x.myview.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

// ---------------------------------------------------------------------------------------------
//  Exercise guide: offline animated figure + steps (English / Hinglish) + online "Watch video".
//  The animation is drawn on a Canvas from ExerciseAnimData (no files, no network).
//  "Watch video" only opens the browser / YouTube through an Intent, so the app still needs
//  no INTERNET permission.
// ---------------------------------------------------------------------------------------------

class ExGuide(
    val muscles: String,
    val stepsEn: List<String>,
    val stepsHi: List<String>,
    val mistakesEn: List<String>,
    val mistakesHi: List<String>,
    val query: String,
)

/** Maps an exercise name (also custom ones) to an animation key, or null when we have none. */
fun exerciseKeyFor(name: String): String? {
    val n = name.lowercase().replace('-', ' ').replace('_', ' ')
    return when {
        "wall" in n && "sit" in n -> "wallsit"
        "push" in n -> "pushup"
        "pull" in n || "chin up" in n || "chinup" in n -> "pullup"
        "squat" in n -> "squat"
        "sit up" in n || "situp" in n -> "situp"
        "crunch" in n -> "crunch"
        "lunge" in n -> "lunge"
        "burpee" in n -> "burpee"
        "jack" in n -> "jacks"
        "dip" in n -> "dips"
        "plank" in n -> "plank"
        "skip" in n || "rope" in n -> "skipping"
        else -> null
    }
}

private val GUIDES: Map<String, ExGuide> = mapOf(
    "pushup" to ExGuide(
        "Chest • Shoulders • Triceps • Core",
        listOf(
            "Hands just wider than your shoulders, body in one straight line from head to heels.",
            "Squeeze your core and glutes. Keep elbows about 45° from your body.",
            "Lower slowly until your chest almost touches the floor.",
            "Push the floor away until your arms are straight. Breathe in going down, out coming up.",
        ),
        listOf(
            "Haath kandhon se thoda chaude rakho, sar se edi tak body ek seedhi line me.",
            "Pet (core) aur glutes tight rakho. Kohni body se lagbhag 45° par rakho.",
            "Dheere neeche jao jab tak chest zameen ke paas na aa jaye.",
            "Zameen ko dhakka do jab tak haath seedhe na ho jaye. Neeche jate waqt saans lo, upar aate waqt chhodo.",
        ),
        listOf("Hips sagging or sticking up", "Elbows flared out to 90°", "Half reps, neck dropping forward"),
        listOf("Kamar neeche latakna ya upar uthana", "Kohni bahar 90° par phailana", "Aadhi rep karna, gardan aage jhukana"),
        "how to do push ups with proper form",
    ),
    "pullup" to ExGuide(
        "Back • Biceps • Shoulders • Core",
        listOf(
            "Hold the bar a bit wider than your shoulders, arms fully straight, shoulders pulled down.",
            "Squeeze your core, cross or bend your legs slightly behind you.",
            "Pull your elbows down until your chin goes above the bar.",
            "Lower slowly (2-3 seconds) until arms are straight again. No swinging.",
        ),
        listOf(
            "Bar ko kandhon se thoda chauda pakdo, haath poore seedhe, kandhe neeche khinche hue.",
            "Core tight rakho, pair thode peeche mod lo.",
            "Kohni ko neeche kheencho jab tak thodi bar ke upar na aa jaye.",
            "2-3 second me dheere neeche aao jab tak haath seedhe ho jaye. Jhoolna nahi hai.",
        ),
        listOf("Swinging or kicking for momentum", "Not going all the way down", "Shrugging shoulders to the ears"),
        listOf("Jhatka ya kick maarke upar jana", "Poora neeche na aana", "Kandhe kaano tak uthana"),
        "how to do pull ups correctly for beginners",
    ),
    "squat" to ExGuide(
        "Thighs • Glutes • Core",
        listOf(
            "Feet shoulder-width apart, toes slightly out, chest up.",
            "Push hips back and bend knees as if sitting on a chair. Arms go forward for balance.",
            "Go down until thighs are about parallel to the floor. Knees follow the toes.",
            "Press through your heels to stand up and squeeze your glutes at the top.",
        ),
        listOf(
            "Pair kandhon jitne chaude, panje thode bahar, seena upar.",
            "Kulhe peeche karo aur ghutne modo, jaise kursi par baith rahe ho. Haath balance ke liye aage.",
            "Jab tak jaanghe zameen ke barabar na ho jaye neeche jao. Ghutne panjon ki direction me rahein.",
            "Edi se dabaake khade ho jao aur upar glutes tight karo.",
        ),
        listOf("Knees caving inwards", "Heels lifting off the floor", "Rounding the lower back"),
        listOf("Ghutne andar ki taraf jhukna", "Edi upar uthna", "Kamar ko gol karna"),
        "how to do bodyweight squats with proper form",
    ),
    "situp" to ExGuide(
        "Abs • Hip flexors",
        listOf(
            "Lie on your back, knees bent, feet flat on the floor.",
            "Cross your arms on your chest or lightly touch your head.",
            "Curl up using your abs until your chest comes near your knees.",
            "Lower back down slowly with control. Breathe out going up.",
        ),
        listOf(
            "Peeth ke bal let jao, ghutne mude hue, pair zameen par.",
            "Haath chest par cross karo ya sar ko halka chhuo.",
            "Pet ki taakat se upar aao jab tak chest ghutno ke paas na aa jaye.",
            "Dheere control ke saath wapas let jao. Upar jate waqt saans bahar chhodo.",
        ),
        listOf("Pulling on the neck with your hands", "Using momentum and jerking", "Feet lifting off the floor"),
        listOf("Haath se gardan kheenchna", "Jhatke se upar aana", "Pair zameen se uthna"),
        "how to do sit ups correctly",
    ),
    "crunch" to ExGuide(
        "Upper abs",
        listOf(
            "Lie on your back, knees bent, feet flat. Hands lightly behind your head.",
            "Lift only your head and shoulder blades off the floor, squeezing your abs.",
            "Hold for a second at the top. Keep your lower back on the floor.",
            "Lower slowly and repeat. Breathe out as you crunch.",
        ),
        listOf(
            "Peeth ke bal let jao, ghutne mude hue. Haath sar ke peeche halke se.",
            "Sirf sar aur kandhe zameen se uthao aur pet ko dabao.",
            "Upar 1 second ruko. Kamar zameen par hi rahe.",
            "Dheere neeche aao aur phir se karo. Upar jate waqt saans bahar.",
        ),
        listOf("Yanking your neck forward", "Lifting the whole back off the floor", "Going too fast"),
        listOf("Gardan ko aage kheenchna", "Poori peeth upar uthana", "Bahut tez karna"),
        "how to do crunches correctly",
    ),
    "lunge" to ExGuide(
        "Thighs • Glutes • Balance",
        listOf(
            "Stand tall, then take a big step forward with one foot.",
            "Lower your hips straight down until both knees are about 90°.",
            "Front knee stays above the ankle, back knee hovers just above the floor.",
            "Push through the front heel to come back up. Switch legs.",
        ),
        listOf(
            "Seedhe khade ho, phir ek pair se bada kadam aage lo.",
            "Kulhe seedhe neeche karo jab tak dono ghutne lagbhag 90° par na ho jaye.",
            "Aage wala ghutna takhne ke upar, peeche wala ghutna zameen se thoda upar.",
            "Aage ki edi se dabaake wapas upar aao. Phir pair badlo.",
        ),
        listOf("Front knee going past the toes", "Leaning the body forward", "Steps too short"),
        listOf("Aage ka ghutna panje se aage jana", "Body ko aage jhukana", "Kadam bahut chhota rakhna"),
        "how to do lunges with proper form",
    ),
    "burpee" to ExGuide(
        "Full body • Cardio",
        listOf(
            "Stand, then squat down and place your hands on the floor.",
            "Jump or step your feet back into a plank. Add a push-up if you want.",
            "Jump your feet back in, next to your hands.",
            "Explode upward into a jump with your arms overhead. Land softly and repeat.",
        ),
        listOf(
            "Khade ho, phir baith ke haath zameen par rakho.",
            "Pair peeche kudake plank me jao. Chaho to ek push-up bhi karo.",
            "Pair kudake wapas haathon ke paas laao.",
            "Haath upar karke zor se uchho. Narmi se utro aur phir se shuru.",
        ),
        listOf("Sagging lower back in the plank", "Landing on stiff legs", "Rushing and losing form"),
        listOf("Plank me kamar latakna", "Akdi hui taangon par utarna", "Jaldbaazi me form bigadna"),
        "how to do burpees step by step",
    ),
    "jacks" to ExGuide(
        "Full body • Cardio • Calves",
        listOf(
            "Stand straight with feet together and arms by your sides.",
            "Jump and spread your feet wide while swinging your arms overhead.",
            "Jump again, bringing feet together and arms back down.",
            "Keep a steady rhythm and land softly on the balls of your feet.",
        ),
        listOf(
            "Seedhe khade ho, pair jude hue, haath side me.",
            "Uchho aur pair chaude karo, saath me haath sar ke upar le jao.",
            "Phir uchho aur pair jodke haath neeche le aao.",
            "Ek rhythm me karo aur panjon par narmi se utro.",
        ),
        listOf("Landing flat and hard", "Arms not going all the way up", "Holding your breath"),
        listOf("Sapat aur zor se utarna", "Haath poore upar na le jana", "Saans rok lena"),
        "how to do jumping jacks properly",
    ),
    "dips" to ExGuide(
        "Triceps • Chest • Shoulders",
        listOf(
            "Grip a stable bar, bench or chair, arms straight, body upright.",
            "Bend your elbows backwards and lower until upper arms are about parallel to the floor.",
            "Keep shoulders down and elbows close to your body.",
            "Press up until arms are straight again.",
        ),
        listOf(
            "Majboot bar, bench ya kursi pakdo, haath seedhe, body seedhi.",
            "Kohni peeche ki taraf modo aur neeche jao jab tak upper arm zameen ke barabar na ho.",
            "Kandhe neeche, kohni body ke paas rakho.",
            "Dabaake upar aao jab tak haath seedhe na ho jaye.",
        ),
        listOf("Going too low and straining the shoulders", "Elbows flaring out", "Shoulders shrugging up"),
        listOf("Bahut neeche jaake kandhe par zor dena", "Kohni bahar phailana", "Kandhe upar uthana"),
        "how to do dips correctly",
    ),
    "plank" to ExGuide(
        "Core • Shoulders • Glutes",
        listOf(
            "Place your forearms on the floor, elbows right under your shoulders.",
            "Legs straight, balance on your toes. Body in one line from head to heels.",
            "Squeeze your abs and glutes. Look at the floor so your neck stays neutral.",
            "Hold and breathe steadily. Stop the timer when your form breaks.",
        ),
        listOf(
            "Kohni zameen par, seedhe kandhon ke neeche rakho.",
            "Taangein seedhi, panjon par balance. Sar se edi tak ek seedhi line.",
            "Pet aur glutes tight rakho. Zameen ki taraf dekho taaki gardan seedhi rahe.",
            "Pakde raho aur aaram se saans lo. Form bigde to timer rok do.",
        ),
        listOf("Hips sagging down", "Hips piking up", "Holding your breath"),
        listOf("Kamar neeche latakna", "Kulhe upar uthana", "Saans rok lena"),
        "how to do a plank correctly",
    ),
    "wallsit" to ExGuide(
        "Thighs • Glutes • Calves",
        listOf(
            "Stand with your back against a wall, feet about 60 cm in front.",
            "Slide down until your knees are bent at 90°, thighs parallel to the floor.",
            "Keep your back flat on the wall and knees above your ankles.",
            "Hold and breathe. Rest your hands on your thighs, not on your knees.",
        ),
        listOf(
            "Deewar se peeth lagake khade ho, pair lagbhag 60 cm aage.",
            "Neeche fisalo jab tak ghutne 90° par na ho jaye, jaanghe zameen ke barabar.",
            "Peeth deewar se chipki rahe, ghutne takhno ke upar.",
            "Pakde raho aur saans lo. Haath jaangho par rakho, ghutno par nahi.",
        ),
        listOf("Knees going past the toes", "Pressing hands on the knees", "Sliding too high up"),
        listOf("Ghutne panje se aage jana", "Haath ghutno par dabana", "Bahut upar rukna"),
        "how to do a wall sit properly",
    ),
    "skipping" to ExGuide(
        "Calves • Shoulders • Cardio",
        listOf(
            "Hold the rope handles with elbows close to your sides.",
            "Turn the rope using your wrists, not your whole arms.",
            "Jump just a little (2-3 cm) as the rope passes under your feet.",
            "Land softly on the balls of your feet and keep a steady rhythm.",
        ),
        listOf(
            "Rope ke handle pakdo, kohni body ke paas.",
            "Rope ko kalai se ghumao, poore haath se nahi.",
            "Rope pair ke neeche aaye tab bas thoda (2-3 cm) uchho.",
            "Panjon par narmi se utro aur ek rhythm banaye rakho.",
        ),
        listOf("Jumping too high", "Big arm circles", "Landing on heels"),
        listOf("Bahut upar uchhalna", "Haath bade ghere me ghumana", "Edi par utarna"),
        "how to jump rope for beginners",
    ),
)

private fun openVideo(ctx: Context, query: String) {
    try {
        val uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
        val i = Intent(Intent.ACTION_VIEW, uri)
        if (ctx !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (t: Throwable) {
        try {
            Toast.makeText(ctx, "No browser found", Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Renderer
// ---------------------------------------------------------------------------------------------

private const val HEAD_R = 0.062f

@Composable
fun ExerciseFigure(
    anim: ExAnim,
    accent: Color,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    slow: Boolean = false,
) {
    var phase by remember(anim) { mutableFloatStateOf(0f) }
    LaunchedEffect(anim, playing, slow) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { n ->
                if (last != 0L) {
                    val dtMs = (n - last) / 1_000_000f
                    val len = anim.loopMs * (if (slow) 2.2f else 1f)
                    phase = (phase + dtMs / len) % 1f
                }
                last = n
            }
        }
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val groundColor = onSurface.copy(alpha = 0.22f)
    val propColor = onSurface.copy(alpha = 0.45f)

    Canvas(modifier) {
        val pad = 14.dp.toPx()
        val s = min((size.width - 2 * pad) / anim.w, (size.height - 2 * pad) / anim.h)
        val ox = (size.width - anim.w * s) / 2f
        val oy = (size.height + anim.h * s) / 2f

        fun sx(x: Float) = ox + x * s
        fun sy(y: Float) = oy - y * s

        val n = anim.count
        val pos = phase * n
        val i0 = pos.toInt().coerceIn(0, n - 1)
        val i1 = (i0 + 1) % n
        val f = pos - i0
        val fr = anim.frames
        fun wx(j: Int): Float = fr[i0 * 22 + j * 2] + (fr[i1 * 22 + j * 2] - fr[i0 * 22 + j * 2]) * f
        fun wy(j: Int): Float = fr[i0 * 22 + j * 2 + 1] + (fr[i1 * 22 + j * 2 + 1] - fr[i0 * 22 + j * 2 + 1]) * f
        fun p(j: Int) = Offset(sx(wx(j)), sy(wy(j)))

        // ground / mat
        val gy = sy(0f)
        if (anim.mat) {
            drawRoundRect(
                color = accent.copy(alpha = 0.18f),
                topLeft = Offset(sx(0.02f), gy),
                size = Size((anim.w - 0.04f) * s, 7.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
        drawLine(groundColor, Offset(sx(0f), gy), Offset(sx(anim.w), gy), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)

        // props (bar, wall, posts)
        val pr = anim.props
        var k = 0
        while (k + 4 < pr.size) {
            drawLine(
                propColor,
                Offset(sx(pr[k]), sy(pr[k + 1])),
                Offset(sx(pr[k + 2]), sy(pr[k + 3])),
                strokeWidth = max(pr[k + 4] * s, 3.dp.toPx()),
                cap = StrokeCap.Round,
            )
            k += 5
        }

        val limbW = max(0.05f * s, 4.dp.toPx())
        val torsoW = max(0.075f * s, 6.dp.toPx())
        val farColor = if (anim.front) accent else accent.copy(alpha = 0.5f)

        // far limbs
        drawLine(farColor, p(2), p(9), limbW, StrokeCap.Round)
        drawLine(farColor, p(9), p(10), limbW, StrokeCap.Round)
        drawLine(farColor, p(1), p(5), limbW, StrokeCap.Round)
        drawLine(farColor, p(5), p(6), limbW, StrokeCap.Round)
        // torso
        drawLine(accent, p(2), p(1), torsoW, StrokeCap.Round)
        // near limbs
        drawLine(accent, p(2), p(7), limbW, StrokeCap.Round)
        drawLine(accent, p(7), p(8), limbW, StrokeCap.Round)
        drawLine(accent, p(1), p(3), limbW, StrokeCap.Round)
        drawLine(accent, p(3), p(4), limbW, StrokeCap.Round)
        // head
        drawCircle(accent, radius = max(HEAD_R * s, 7.dp.toPx()), center = p(0))

        // jump rope
        if (anim.rope) {
            val hand = p(4)
            val hy = wy(4)
            val ry = 0.53f + 0.55f * cos((2.0 * PI * phase).toFloat())
            val ctrlY = (ry - 0.25f * hy) / 0.75f
            val path = Path()
            path.moveTo(hand.x, hand.y)
            path.cubicTo(sx(wx(4) + 0.30f), sy(ctrlY), sx(wx(4) - 0.85f), sy(ctrlY), hand.x, hand.y)
            drawPath(path, onSurface.copy(alpha = 0.7f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

// ---------------------------------------------------------------------------------------------
//  Card used on the exercise detail screen
// ---------------------------------------------------------------------------------------------

@Composable
private fun GuidePill(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ExerciseGuideCard(name: String, accent: Color, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val key = remember(name) { exerciseKeyFor(name) }
    val anim = remember(key) { key?.let { ExerciseAnimData.get(it) } }
    val guide = remember(key) { key?.let { GUIDES[it] } }
    var hindi by rememberSaveable { mutableStateOf(false) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var slow by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    MvCard(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "How to do it",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (guide != null) {
                    GuidePill("English", !hindi, accent) { hindi = false }
                    Spacer(Modifier.width(6.dp))
                    GuidePill("Hinglish", hindi, accent) { hindi = true }
                }
            }

            if (anim != null && guide != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(alpha = 0.10f))
                        .clickable { playing = !playing },
                ) {
                    ExerciseFigure(anim, accent, Modifier.fillMaxWidth().height(210.dp), playing, slow)
                    if (!playing) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                    ) {
                        GuidePill(if (slow) "Slow 0.5×" else "Normal 1×", slow, accent) { slow = !slow }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    (if (hindi) "Kaun si muscle: " else "Works: ") + guide.muscles,
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { open = !open }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (hindi) "Steps aur tips" else "Steps & tips",
                        Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        null,
                        tint = muted,
                    )
                }
                if (open) {
                    val steps = if (hindi) guide.stepsHi else guide.stepsEn
                    steps.forEachIndexed { i, st ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(st, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (hindi) "Aam galtiyan" else "Common mistakes",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    (if (hindi) guide.mistakesHi else guide.mistakesEn).forEach { m ->
                        Text("•  $m", Modifier.padding(vertical = 2.dp), style = MaterialTheme.typography.bodyMedium, color = muted)
                    }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "No built-in animation for this exercise yet. You can still watch a tutorial video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { openVideo(ctx, guide?.query ?: ("how to do $name exercise correctly")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (hindi) "Video dekho (online)" else "Watch video (online)")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (hindi) "Animation offline chalta hai. Video browser / YouTube me khulta hai."
                else "Animation works offline. The video opens in your browser / YouTube.",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}
