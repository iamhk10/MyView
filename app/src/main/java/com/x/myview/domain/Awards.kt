package com.x.myview.domain

import com.x.myview.data.AppSettings
import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.ExerciseLogEntity
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.KIND_REPS
import com.x.myview.data.db.WaterLogEntity
import kotlin.math.sqrt

/** One badge with how far along it is. */
class Badge(
    val title: String,
    val description: String,
    val icon: String,
    val current: Long,
    val target: Long,
) {
    val done: Boolean get() = current >= target
    val progress: Float get() = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

class AwardsResult(
    val xp: Int,
    val level: Int,
    /** XP earned inside the current level. */
    val xpInLevel: Int,
    /** XP the current level is long. */
    val xpLevelSize: Int,
    val badges: List<Badge>,
) {
    val levelProgress: Float get() = (xpInLevel.toFloat() / xpLevelSize.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/** Level n starts at 50 * (n-1)^2 XP, so each level takes a little more than the one before. */
fun levelFor(xp: Int): Int = sqrt(xp / 50.0).toInt() + 1

/** Everything the player has done turned into experience points and badges. Nothing is stored. */
fun computeAwards(
    habits: List<HabitEntity>,
    habitDone: Map<Long, Set<Long>>,
    exercises: List<ExerciseEntity>,
    exerciseLogs: List<ExerciseLogEntity>,
    waterLogs: List<WaterLogEntity>,
    steps: Map<Long, Int>,
    sleepNights: Int,
    moodEntries: Int,
    focusByDay: Map<Long, Int>,
    breathByDay: Map<Long, Int>,
    settings: AppSettings,
    today: Long,
): AwardsResult {
    val habitChecks = habitDone.values.sumOf { it.size }
    val longestStreak = habits.maxOfOrNull { bestStreak(it.days, habitDone[it.id] ?: emptySet(), today) } ?: 0
    val waterDays = waterByDay(waterLogs).count { it.value >= settings.waterGoalMl }
    val stepDays = steps.count { it.value >= settings.stepGoal }
    val totalSteps = steps.values.sumOf { it.toLong() }
    val bestSteps = steps.values.maxOrNull() ?: 0
    val repIds = exercises.filter { it.kind == KIND_REPS }.map { it.id }.toSet()
    val totalReps = exerciseLogs.filter { it.exerciseId in repIds }.sumOf { it.amount.toLong() }
    val workoutDays = exerciseLogs.map { it.day }.distinct().size
    val focusTotal = focusByDay.values.sum()
    val breathTotal = breathByDay.values.sum()

    val xp = habitChecks * 10 + waterDays * 30 + stepDays * 30 + workoutDays * 20 +
        (totalReps / 10).toInt() + sleepNights * 5 + moodEntries * 5 + focusTotal / 5 + breathTotal * 10
    val level = levelFor(xp)
    val start = 50 * (level - 1) * (level - 1)
    val end = 50 * level * level

    val badges = listOf(
        Badge("First tick", "Complete a habit once", "check", habitChecks.toLong(), 1),
        Badge("On a roll", "7 day habit streak", "fire", longestStreak.toLong(), 7),
        Badge("Unstoppable", "30 day habit streak", "bolt", longestStreak.toLong(), 30),
        Badge("Habit machine", "Tick 100 habits", "star", habitChecks.toLong(), 100),
        Badge("Hydrated", "Reach your water goal on 7 days", "water", waterDays.toLong(), 7),
        Badge("Ocean", "Reach your water goal on 30 days", "water", waterDays.toLong(), 30),
        Badge("Iron", "Do 1,000 reps in total", "fitness", totalReps, 1000),
        Badge("Gym rat", "Work out on 20 different days", "pushup", workoutDays.toLong(), 20),
        Badge("10K day", "Walk 10,000 steps in one day", "walk", bestSteps.toLong(), 10000),
        Badge("Wanderer", "Walk 100,000 steps in total", "run", totalSteps, 100000),
        Badge("Sleepyhead", "Log 7 nights of sleep", "sleep", sleepNights.toLong(), 7),
        Badge("Self aware", "Fill the mood diary 7 times", "heart", moodEntries.toLong(), 7),
        Badge("Deep focus", "Focus for 300 minutes in total", "timer", focusTotal.toLong(), 300),
        Badge("Zen", "Finish 10 breathing sessions", "spa", breathTotal.toLong(), 10),
    )
    return AwardsResult(xp, level, xp - start, end - start, badges)
}
