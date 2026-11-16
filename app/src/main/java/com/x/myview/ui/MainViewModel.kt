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
import com.x.myview.util.todayEpochDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as MyViewApp).container
    private val repo = container.repository
    private val settingsStore = container.settings

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

    fun updateSettings(change: (AppSettings) -> AppSettings) = settingsStore.update(change)

    // ---- Habits ----
    fun toggleHabit(habitId: Long, day: Long, done: Boolean) {
        viewModelScope.launch { repo.setHabitDone(habitId, day, done) }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch { repo.saveHabit(habit) }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch { repo.deleteHabit(id) }
    }

    // ---- Exercises ----
    fun saveExercise(e: ExerciseEntity) {
        viewModelScope.launch { repo.saveExercise(e) }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch { repo.deleteExercise(id) }
    }

    fun addExerciseLog(exerciseId: Long, amount: Int) {
        val day = todayEpochDay()
        viewModelScope.launch { repo.addExerciseLog(exerciseId, day, amount) }
    }

    fun deleteExerciseLog(id: Long) {
        viewModelScope.launch { repo.deleteExerciseLog(id) }
    }

    // ---- Water ----
    fun addWater(ml: Int) {
        val day = todayEpochDay()
        viewModelScope.launch { repo.addWater(day, ml) }
    }

    fun deleteWater(id: Long) {
        viewModelScope.launch { repo.deleteWater(id) }
    }
}
