package com.x.myview.ui.components

/** One exercise of the built-in library (each has an offline animation in [ExerciseAnimData]). */
class LibItem(
    val key: String,
    val name: String,
    val category: String,
    val level: String,
    /** True when it is measured in seconds (plank, wall sit...), false when counted in reps. */
    val timed: Boolean,
    /** Suggested daily goal: reps, or seconds when [timed]. */
    val goal: Int,
    val icon: String,
)

val LIB_CATEGORIES = listOf("Strength", "Legs", "Core", "Cardio")

val LIBRARY: List<LibItem> = listOf(
    LibItem("wallpush", "Wall push-ups", "Strength", "Easy", false, 30, "pushup"),
    LibItem("pushup", "Push-ups", "Strength", "Medium", false, 50, "pushup"),
    LibItem("dips", "Dips", "Strength", "Medium", false, 30, "pushup"),
    LibItem("pullup", "Pull-ups", "Strength", "Hard", false, 20, "pullup"),
    LibItem("superman", "Superman", "Strength", "Easy", false, 30, "yoga"),
    LibItem("squat", "Squats", "Legs", "Easy", false, 100, "squat"),
    LibItem("lunge", "Lunges", "Legs", "Easy", false, 40, "squat"),
    LibItem("bridge", "Glute bridges", "Legs", "Easy", false, 40, "fitness"),
    LibItem("jumpsquat", "Jump squats", "Legs", "Medium", false, 30, "bolt"),
    LibItem("wallsit", "Wall sit", "Legs", "Medium", true, 90, "timer"),
    LibItem("situp", "Sit-ups", "Core", "Easy", false, 50, "situp"),
    LibItem("crunch", "Crunches", "Core", "Easy", false, 40, "situp"),
    LibItem("legraise", "Leg raises", "Core", "Medium", false, 30, "fitness"),
    LibItem("mountain", "Mountain climbers", "Core", "Medium", false, 60, "run"),
    LibItem("plank", "Plank", "Core", "Medium", true, 120, "timer"),
    LibItem("jacks", "Jumping jacks", "Cardio", "Easy", false, 100, "bolt"),
    LibItem("highknees", "High knees", "Cardio", "Easy", true, 60, "run"),
    LibItem("skipping", "Skipping", "Cardio", "Medium", true, 300, "run"),
    LibItem("burpee", "Burpees", "Cardio", "Hard", false, 20, "bolt"),
)

fun libItem(key: String): LibItem? = LIBRARY.firstOrNull { it.key == key }

/** One line of a routine: do [sets] sets of [amount] (reps, or seconds when the exercise is timed). */
class PlanStep(val key: String, val amount: Int, val sets: Int)

class Plan(
    val id: Int,
    val title: String,
    val subtitle: String,
    val level: String,
    val steps: List<PlanStep>,
) {
    /** Rough length in minutes: about 3 s per rep, plus 20 s rest after every set. */
    val minutes: Int
        get() {
            var sec = 0
            for (st in steps) {
                val timed = libItem(st.key)?.timed == true
                sec += st.sets * ((if (timed) st.amount else st.amount * 3) + REST_SECONDS)
            }
            return (sec + 30) / 60
        }
}

const val REST_SECONDS = 20

val PLANS: List<Plan> = listOf(
    Plan(
        0, "Beginner full body", "Easy start for every muscle", "Easy",
        listOf(
            PlanStep("jacks", 20, 2), PlanStep("squat", 12, 2), PlanStep("wallpush", 10, 2),
            PlanStep("lunge", 8, 2), PlanStep("bridge", 12, 2), PlanStep("plank", 20, 2),
        ),
    ),
    Plan(
        1, "Core burner", "Strong abs and a steady middle", "Medium",
        listOf(
            PlanStep("crunch", 15, 3), PlanStep("legraise", 10, 3), PlanStep("mountain", 20, 3),
            PlanStep("bridge", 15, 2), PlanStep("plank", 40, 2),
        ),
    ),
    Plan(
        2, "Upper body", "Chest, arms, shoulders and back", "Medium",
        listOf(
            PlanStep("pushup", 10, 3), PlanStep("dips", 8, 3), PlanStep("superman", 10, 2),
            PlanStep("pullup", 4, 3), PlanStep("plank", 30, 2),
        ),
    ),
    Plan(
        3, "Cardio blast", "Get your heart rate up", "Medium",
        listOf(
            PlanStep("jacks", 30, 2), PlanStep("highknees", 30, 2), PlanStep("burpee", 8, 2),
            PlanStep("skipping", 45, 2), PlanStep("jumpsquat", 10, 2),
        ),
    ),
    Plan(
        4, "Leg day", "Thighs, glutes and calves", "Medium",
        listOf(
            PlanStep("squat", 15, 3), PlanStep("lunge", 12, 2), PlanStep("jumpsquat", 10, 2),
            PlanStep("bridge", 15, 3), PlanStep("wallsit", 40, 2),
        ),
    ),
)
