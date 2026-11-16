package com.x.myview.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A habit. [days] is a weekday bit mask: Monday = 1, Tuesday = 2 ... Sunday = 64 (127 = every day). */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorIndex: Int,
    val days: Int,
    /** Reminder time as minutes after midnight, or -1 for none. Used by reminders. */
    val reminderMin: Int = -1,
    val archived: Boolean = false,
    val createdAt: Long,
    val sortOrder: Int = 0,
)

/** One row = the habit was done on [day] (epoch day). */
@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "day"], indices = [Index("day")])
data class HabitLogEntity(
    val habitId: Long,
    val day: Long,
)

const val KIND_REPS = 0
const val KIND_TIME = 1

/** An exercise. [kind] 0 counts reps, 1 counts seconds. [dailyGoal] is reps or seconds per day. */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: Int,
    val icon: String,
    val dailyGoal: Int,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** One saved set (reps) or one timed session (seconds). */
@Entity(tableName = "exercise_logs", indices = [Index("day"), Index("exerciseId")])
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val day: Long,
    val amount: Int,
    val createdAt: Long,
)

@Entity(tableName = "water_logs", indices = [Index("day")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val ml: Int,
    val createdAt: Long,
)
