package com.x.myview.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_logs")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM habit_logs WHERE habitId = :id")
    suspend fun deleteLogs(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLog(log: HabitLogEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): HabitEntity?

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND day = :day")
    suspend fun doneCount(habitId: Long, day: Long): Int

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND day = :day")
    suspend fun removeLog(habitId: Long, day: Long)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_logs")
    fun observeLogs(): Flow<List<ExerciseLogEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM exercise_logs WHERE exerciseId = :id")
    suspend fun deleteLogsFor(id: Long)

    @Insert
    suspend fun addLog(log: ExerciseLogEntity)

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE day = :day")
    suspend fun logCount(day: Long): Int

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs ORDER BY createdAt")
    fun observeLogs(): Flow<List<WaterLogEntity>>

    @Insert
    suspend fun addLog(log: WaterLogEntity)

    @Query("SELECT COALESCE(SUM(ml), 0) FROM water_logs WHERE day = :day")
    suspend fun total(day: Long): Int

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)
}

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        ExerciseEntity::class,
        ExerciseLogEntity::class,
        WaterLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MyViewDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun waterDao(): WaterDao
}
