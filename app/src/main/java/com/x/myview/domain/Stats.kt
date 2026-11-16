package com.x.myview.domain

import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.ExerciseLogEntity
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.WaterLogEntity
import com.x.myview.util.isScheduled

fun waterTotal(logs: List<WaterLogEntity>, day: Long): Int = logs.sumOf { if (it.day == day) it.ml else 0 }

fun waterByDay(logs: List<WaterLogEntity>): Map<Long, Int> =
    logs.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.ml } }

fun exerciseTotal(logs: List<ExerciseLogEntity>, exerciseId: Long, day: Long): Int =
    logs.sumOf { if (it.exerciseId == exerciseId && it.day == day) it.amount else 0 }

fun exerciseByDay(logs: List<ExerciseLogEntity>, exerciseId: Long): Map<Long, Int> =
    logs.filter { it.exerciseId == exerciseId }.groupBy { it.day }.mapValues { e -> e.value.sumOf { it.amount } }

/** Habits that are planned for [day]. */
fun scheduledHabits(habits: List<HabitEntity>, day: Long): List<HabitEntity> =
    habits.filter { isScheduled(it.days, day) }

/**
 * How far the exercises logged on [day] are towards their goals, 0..1.
 * Only exercises that have something logged count; nothing logged gives 0.
 */
fun workoutProgress(exercises: List<ExerciseEntity>, logs: List<ExerciseLogEntity>, day: Long): Float {
    var used = 0
    var sum = 0f
    for (e in exercises) {
        val total = exerciseTotal(logs, e.id, day)
        if (total > 0) {
            used++
            sum += (total.toFloat() / e.dailyGoal.coerceAtLeast(1)).coerceAtMost(1f)
        }
    }
    return if (used == 0) 0f else sum / used
}
