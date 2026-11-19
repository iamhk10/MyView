package com.x.myview.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_logs ORDER BY day")
    fun observe(): Flow<List<SleepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SleepLogEntity)

    @Query("DELETE FROM sleep_logs WHERE day = :day")
    suspend fun delete(day: Long)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY day")
    fun observe(): Flow<List<WeightLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs WHERE day = :day")
    suspend fun delete(day: Long)
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_logs ORDER BY day")
    fun observe(): Flow<List<MoodLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: MoodLogEntity)

    @Query("DELETE FROM mood_logs WHERE day = :day")
    suspend fun delete(day: Long)
}

/** Whole-table reads and writes used by backup and restore. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM habits") suspend fun habits(): List<HabitEntity>
    @Query("SELECT * FROM habit_logs") suspend fun habitLogs(): List<HabitLogEntity>
    @Query("SELECT * FROM exercises") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT * FROM exercise_logs") suspend fun exerciseLogs(): List<ExerciseLogEntity>
    @Query("SELECT * FROM water_logs") suspend fun water(): List<WaterLogEntity>
    @Query("SELECT * FROM sleep_logs") suspend fun sleep(): List<SleepLogEntity>
    @Query("SELECT * FROM weight_logs") suspend fun weight(): List<WeightLogEntity>
    @Query("SELECT * FROM mood_logs") suspend fun mood(): List<MoodLogEntity>

    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM habit_logs") suspend fun clearHabitLogs()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM exercise_logs") suspend fun clearExerciseLogs()
    @Query("DELETE FROM water_logs") suspend fun clearWater()
    @Query("DELETE FROM sleep_logs") suspend fun clearSleep()
    @Query("DELETE FROM weight_logs") suspend fun clearWeight()
    @Query("DELETE FROM mood_logs") suspend fun clearMood()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabits(list: List<HabitEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHabitLogs(list: List<HabitLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putExercises(list: List<ExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putExerciseLogs(list: List<ExerciseLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWater(list: List<WaterLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSleep(list: List<SleepLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putWeight(list: List<WeightLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMood(list: List<MoodLogEntity>)
}

/** Version 1 -> 2: adds the sleep, weight and mood tables. Existing data is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sleep_logs` (`day` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `quality` INTEGER NOT NULL, PRIMARY KEY(`day`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_logs` (`day` INTEGER NOT NULL, `kg` REAL NOT NULL, PRIMARY KEY(`day`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `mood_logs` (`day` INTEGER NOT NULL, `mood` INTEGER NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`day`))")
    }
}

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        ExerciseEntity::class,
        ExerciseLogEntity::class,
        WaterLogEntity::class,
        SleepLogEntity::class,
        WeightLogEntity::class,
        MoodLogEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MyViewDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun waterDao(): WaterDao
    abstract fun sleepDao(): SleepDao
    abstract fun weightDao(): WeightDao
    abstract fun moodDao(): MoodDao
    abstract fun backupDao(): BackupDao
}
