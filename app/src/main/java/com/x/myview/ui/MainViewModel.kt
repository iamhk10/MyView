package com.x.myview.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.x.myview.MyViewApp
import com.x.myview.data.AppSettings
import com.x.myview.data.Backup
import com.x.myview.data.db.MoodLogEntity
import com.x.myview.data.db.SleepLogEntity
import com.x.myview.data.db.WeightLogEntity
import com.x.myview.domain.exerciseTotal
import com.x.myview.domain.scheduledHabits
import com.x.myview.domain.waterTotal
import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.ExerciseLogEntity
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.WaterLogEntity
import com.x.myview.reminders.Reminders
import com.x.myview.util.todayEpochDay
import com.x.myview.steps.StepService
import com.x.myview.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val container = (app as MyViewApp).container
    private val repo = container.repository
    private val settingsStore = container.settings
    private val stepTracker = container.stepTracker

    val settings: StateFlow<AppSettings> = settingsStore.settings

    private val _today = MutableStateFlow(todayEpochDay())
    /** Today as an epoch day. Updates itself at midnight and when the app comes back to the front. */
    val today: StateFlow<Long> = _today.asStateFlow()

    val habits: StateFlow<List<HabitEntity>> =
        repo.habits.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val habitDone: StateFlow<Map<Long, Set<Long>>> =
        repo.habitDone.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val exercises: StateFlow<List<ExerciseEntity>> =
        repo.exercises.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val exerciseLogs: StateFlow<List<ExerciseLogEntity>> =
        repo.exerciseLogs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val waterLogs: StateFlow<List<WaterLogEntity>> =
        repo.waterLogs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Steps per day. Stays empty on phones without a step sensor. */
    val steps: StateFlow<Map<Long, Int>> = stepTracker.steps
    val stepsSupported: Boolean = stepTracker.isSupported

    val sleepLogs: StateFlow<List<SleepLogEntity>> =
        repo.sleep.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val weightLogs: StateFlow<List<WeightLogEntity>> =
        repo.weights.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val moodLogs: StateFlow<List<MoodLogEntity>> =
        repo.moods.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _celebrate = MutableStateFlow(0)
    /** Goes up by one every time a daily goal is completed; the root screen shows confetti on each change. */
    val celebrate: StateFlow<Int> = _celebrate.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private fun celebrateNow() {
        if (settingsStore.settings.value.celebrate) _celebrate.value = _celebrate.value + 1
    }

    init {
        viewModelScope.launch { repo.ensureSeed() }
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(Duration.between(now, midnight).toMillis() + 500)
                refreshDay()
            }
        }
    }

    /** Call when the app returns to the front: the timer above does not run while the phone sleeps. */
    fun refreshDay() {
        _today.value = todayEpochDay()
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        settingsStore.update(change)
        Reminders.scheduleAllAsync(app)
        WidgetUpdater.updateAllAsync(app)
        if (settingsStore.settings.value.liveSteps) StepService.startIfEnabled(app) else StepService.stop(app)
    }

    // ---- Steps ----
    fun startSteps() {
        stepTracker.startLive()
        StepService.startIfEnabled(app)
    }

    fun stopSteps() = stepTracker.stopLive()
    fun stepsPermissionGranted(): Boolean = stepTracker.hasPermission()

    // ---- Habits ----
    fun toggleHabit(habitId: Long, day: Long, done: Boolean) {
        if (done && day == todayEpochDay()) {
            val planned = scheduledHabits(habits.value, day)
            val doneMap = habitDone.value
            fun isDone(id: Long) = day in (doneMap[id] ?: emptySet())
            val wasAll = planned.isNotEmpty() && planned.all { isDone(it.id) }
            val nowAll = planned.isNotEmpty() && planned.all { it.id == habitId || isDone(it.id) }
            if (nowAll && !wasAll) celebrateNow()
        }
        viewModelScope.launch {
            repo.setHabitDone(habitId, day, done)
            WidgetUpdater.updateAll(app)
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repo.saveHabit(habit)
            Reminders.scheduleAll(app)
            WidgetUpdater.updateAll(app)
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repo.deleteHabit(id)
            Reminders.cancelHabit(app, id)
            WidgetUpdater.updateAll(app)
        }
    }

    // ---- Exercises ----
    fun saveExercise(e: ExerciseEntity) {
        viewModelScope.launch { repo.saveExercise(e) }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            repo.deleteExercise(id)
            WidgetUpdater.updateAll(app)
        }
    }

    fun addExerciseLog(exerciseId: Long, amount: Int) {
        val day = todayEpochDay()
        val goal = exercises.value.firstOrNull { it.id == exerciseId }?.dailyGoal ?: 0
        val before = exerciseTotal(exerciseLogs.value, exerciseId, day)
        if (goal > 0 && before < goal && before + amount >= goal) celebrateNow()
        viewModelScope.launch {
            repo.addExerciseLog(exerciseId, day, amount)
            WidgetUpdater.updateAll(app)
        }
    }

    fun deleteExerciseLog(id: Long) {
        viewModelScope.launch {
            repo.deleteExerciseLog(id)
            WidgetUpdater.updateAll(app)
        }
    }

    // ---- Water ----
    fun addWater(ml: Int) {
        val day = todayEpochDay()
        val goal = settingsStore.settings.value.waterGoalMl
        val before = waterTotal(waterLogs.value, day)
        if (before < goal && before + ml >= goal) celebrateNow()
        viewModelScope.launch {
            repo.addWater(day, ml)
            WidgetUpdater.updateAll(app)
        }
    }

    fun deleteWater(id: Long) {
        viewModelScope.launch {
            repo.deleteWater(id)
            WidgetUpdater.updateAll(app)
        }
    }

    // ---- Sleep, weight, mood ----
    fun saveSleep(day: Long, minutes: Int, quality: Int) {
        viewModelScope.launch { repo.saveSleep(day, minutes, quality) }
    }

    fun deleteSleep(day: Long) {
        viewModelScope.launch { repo.deleteSleep(day) }
    }

    fun saveWeight(day: Long, kg: Float) {
        viewModelScope.launch { repo.saveWeight(day, kg) }
    }

    fun deleteWeight(day: Long) {
        viewModelScope.launch { repo.deleteWeight(day) }
    }

    fun saveMood(day: Long, mood: Int, note: String) {
        viewModelScope.launch { repo.saveMood(day, mood, note) }
    }

    fun deleteMood(day: Long) {
        viewModelScope.launch { repo.deleteMood(day) }
    }

    // ---- Backup ----
    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = try {
                val text = Backup.toJson(repo.exportData())
                withContext(Dispatchers.IO) {
                    val out = app.contentResolver.openOutputStream(uri) ?: error("Could not open the file")
                    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                "Backup saved."
            } catch (e: Exception) {
                "Backup failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupMessage.value = try {
                val text = withContext(Dispatchers.IO) {
                    val input = app.contentResolver.openInputStream(uri) ?: error("Could not open the file")
                    input.use { String(it.readBytes(), Charsets.UTF_8) }
                }
                val data = Backup.fromJson(text)
                repo.importData(data)
                Reminders.scheduleAll(app)
                WidgetUpdater.updateAll(app)
                "Restored ${data.habits.size} habits, ${data.exercises.size} exercises and ${data.habitLogs.size + data.exerciseLogs.size + data.water.size + data.sleep.size + data.weight.size + data.mood.size} log entries."
            } catch (e: Exception) {
                "Restore failed: ${e.message ?: "this is not a MyView backup"}. Your data was not changed."
            }
        }
    }
}
