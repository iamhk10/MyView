package com.x.myview.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.x.myview.MyViewApp
import com.x.myview.data.AppSettings
import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.ExerciseLogEntity
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.WaterLogEntity
import com.x.myview.reminders.Reminders
import com.x.myview.util.todayEpochDay
import com.x.myview.widget.WidgetUpdater
import kotlinx.coroutines.delay
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
    }

    // ---- Steps ----
    fun startSteps() = stepTracker.startLive()
    fun stopSteps() = stepTracker.stopLive()
    fun stepsPermissionGranted(): Boolean = stepTracker.hasPermission()

    // ---- Habits ----
    fun toggleHabit(habitId: Long, day: Long, done: Boolean) {
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
}
