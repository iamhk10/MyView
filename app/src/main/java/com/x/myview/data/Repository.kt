package com.x.myview.data

import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.ExerciseLogEntity
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.HabitLogEntity
import com.x.myview.data.db.KIND_REPS
import com.x.myview.data.db.KIND_TIME
import com.x.myview.data.db.MyViewDatabase
import com.x.myview.data.db.WaterLogEntity
import com.x.myview.util.todayEpochDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Repository(private val db: MyViewDatabase) {
    private val habitDao = db.habitDao()
    private val exerciseDao = db.exerciseDao()
    private val waterDao = db.waterDao()

    // ---- Habits ----
    val habits: Flow<List<HabitEntity>> = habitDao.observeHabits()

    /** habitId -> set of epoch days on which the habit was done. */
    val habitDone: Flow<Map<Long, Set<Long>>> = habitDao.observeLogs().map { logs ->
        logs.groupBy({ it.habitId }, { it.day }).mapValues { it.value.toSet() }
    }

    suspend fun saveHabit(habit: HabitEntity) {
        if (habit.id == 0L) habitDao.insert(habit) else habitDao.update(habit)
    }

    suspend fun deleteHabit(id: Long) {
        habitDao.deleteLogs(id)
        habitDao.delete(id)
    }

    suspend fun setHabitDone(habitId: Long, day: Long, done: Boolean) {
        if (done) habitDao.addLog(HabitLogEntity(habitId, day)) else habitDao.removeLog(habitId, day)
    }

    // ---- Exercises ----
    val exercises: Flow<List<ExerciseEntity>> = exerciseDao.observeExercises()
    val exerciseLogs: Flow<List<ExerciseLogEntity>> = exerciseDao.observeLogs()

    suspend fun saveExercise(e: ExerciseEntity) {
        if (e.id == 0L) exerciseDao.insert(e) else exerciseDao.update(e)
    }

    suspend fun deleteExercise(id: Long) {
        exerciseDao.deleteLogsFor(id)
        exerciseDao.delete(id)
    }

    suspend fun addExerciseLog(exerciseId: Long, day: Long, amount: Int) {
        if (amount <= 0) return
        exerciseDao.addLog(ExerciseLogEntity(exerciseId = exerciseId, day = day, amount = amount, createdAt = System.currentTimeMillis()))
    }

    suspend fun deleteExerciseLog(id: Long) = exerciseDao.deleteLog(id)

    // ---- Water ----
    val waterLogs: Flow<List<WaterLogEntity>> = waterDao.observeLogs()

    suspend fun addWater(day: Long, ml: Int) {
        if (ml <= 0) return
        waterDao.addLog(WaterLogEntity(day = day, ml = ml, createdAt = System.currentTimeMillis()))
    }

    suspend fun deleteWater(id: Long) = waterDao.deleteLog(id)

    // ---- First run ----
    /** Fills the exercise list the first time the app runs. */
    suspend fun ensureSeed() {
        if (exerciseDao.count() > 0) return
        val reps = listOf(
            Triple("Push-ups", "pushup", 50),
            Triple("Pull-ups", "pullup", 20),
            Triple("Squats", "squat", 100),
            Triple("Sit-ups", "situp", 50),
            Triple("Crunches", "situp", 40),
            Triple("Lunges", "squat", 40),
            Triple("Burpees", "bolt", 20),
            Triple("Jumping jacks", "bolt", 100),
            Triple("Dips", "pushup", 30),
        )
        val timed = listOf(
            Triple("Plank", "timer", 120),
            Triple("Wall sit", "timer", 90),
            Triple("Skipping", "run", 300),
        )
        val list = ArrayList<ExerciseEntity>()
        var order = 0
        reps.forEach { (n, i, g) -> list.add(ExerciseEntity(name = n, kind = KIND_REPS, icon = i, dailyGoal = g, sortOrder = order++)) }
        timed.forEach { (n, i, g) -> list.add(ExerciseEntity(name = n, kind = KIND_TIME, icon = i, dailyGoal = g, sortOrder = order++)) }
        exerciseDao.insertAll(list)
    }

    // ---- One-shot reads for reminders and widgets ----
    suspend fun habitById(id: Long): HabitEntity? = habitDao.getHabit(id)
    suspend fun isHabitDone(habitId: Long, day: Long): Boolean = habitDao.doneCount(habitId, day) > 0
    suspend fun waterTotalFor(day: Long): Int = waterDao.total(day)
    suspend fun hasExerciseLogs(day: Long): Boolean = exerciseDao.logCount(day) > 0

    fun today(): Long = todayEpochDay()
}
