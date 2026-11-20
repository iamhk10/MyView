package com.x.myview.data

import com.x.myview.data.db.ExerciseEntity
import com.x.myview.data.db.ExerciseLogEntity
import com.x.myview.data.db.HabitEntity
import com.x.myview.data.db.HabitLogEntity
import com.x.myview.data.db.MoodLogEntity
import com.x.myview.data.db.SleepLogEntity
import com.x.myview.data.db.WaterLogEntity
import com.x.myview.data.db.WeightLogEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything the user has logged, plus (format 2) step history, focus minutes, breathing sessions and
 * the app settings. [hasExtras] is false for backups made by older versions, which then leave the
 * current steps, focus, breathing and settings alone when restored.
 */
class BackupData(
    val habits: List<HabitEntity>,
    val habitLogs: List<HabitLogEntity>,
    val exercises: List<ExerciseEntity>,
    val exerciseLogs: List<ExerciseLogEntity>,
    val water: List<WaterLogEntity>,
    val sleep: List<SleepLogEntity>,
    val weight: List<WeightLogEntity>,
    val mood: List<MoodLogEntity>,
    val steps: Map<Long, Int> = emptyMap(),
    val focus: Map<Long, Int> = emptyMap(),
    val breath: Map<Long, Int> = emptyMap(),
    val settings: AppSettings? = null,
    val hasExtras: Boolean = false,
)

/** Reads and writes backups as one readable JSON file. */
object Backup {
    private const val FORMAT = 2

    private fun dayCounts(map: Map<Long, Int>, valueKey: String): JSONArray = JSONArray().also { a ->
        map.entries.sortedBy { it.key }.forEach { a.put(JSONObject().put("day", it.key).put(valueKey, it.value)) }
    }

    private fun settingsToJson(s: AppSettings): JSONObject = JSONObject()
        .put("themeMode", s.themeMode.name).put("accentIndex", s.accentIndex)
        .put("dynamicColor", s.dynamicColor).put("glass", s.glass)
        .put("waterGoalMl", s.waterGoalMl).put("glassMl", s.glassMl).put("stepGoal", s.stepGoal)
        .put("waterReminder", s.waterReminder).put("waterIntervalH", s.waterIntervalH)
        .put("workoutReminder", s.workoutReminder).put("workoutReminderMin", s.workoutReminderMin)
        .put("fontOption", s.fontOption.name).put("textSize", s.textSize.name)
        .put("sleepGoalMin", s.sleepGoalMin).put("useLb", s.useLb).put("weightGoalKg", s.weightGoalKg.toDouble())
        .put("haptics", s.haptics).put("celebrate", s.celebrate).put("liveSteps", s.liveSteps)
        .put("look", s.look.name).put("displayFont", s.displayFont.name)

    private inline fun <reified E : Enum<E>> JSONObject.enumOf(key: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == optString(key) } ?: default

    /** Missing or odd values fall back to the defaults and are kept inside the same limits the settings screen uses. */
    private fun settingsFromJson(o: JSONObject): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = o.enumOf("themeMode", d.themeMode),
            accentIndex = o.optInt("accentIndex", d.accentIndex).coerceAtLeast(0),
            dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
            glass = o.optBoolean("glass", d.glass),
            waterGoalMl = o.optInt("waterGoalMl", d.waterGoalMl).coerceIn(500, 8000),
            glassMl = o.optInt("glassMl", d.glassMl).coerceIn(50, 1500),
            stepGoal = o.optInt("stepGoal", d.stepGoal).coerceIn(1000, 50000),
            waterReminder = o.optBoolean("waterReminder", d.waterReminder),
            waterIntervalH = o.optInt("waterIntervalH", d.waterIntervalH).coerceIn(1, 6),
            workoutReminder = o.optBoolean("workoutReminder", d.workoutReminder),
            workoutReminderMin = o.optInt("workoutReminderMin", d.workoutReminderMin).coerceIn(0, 1439),
            fontOption = o.enumOf("fontOption", d.fontOption),
            textSize = o.enumOf("textSize", d.textSize),
            sleepGoalMin = o.optInt("sleepGoalMin", d.sleepGoalMin).coerceIn(240, 720),
            useLb = o.optBoolean("useLb", d.useLb),
            weightGoalKg = o.optDouble("weightGoalKg", d.weightGoalKg.toDouble()).toFloat().coerceIn(0f, 500f),
            haptics = o.optBoolean("haptics", d.haptics),
            celebrate = o.optBoolean("celebrate", d.celebrate),
            liveSteps = o.optBoolean("liveSteps", d.liveSteps),
            look = o.enumOf("look", d.look),
            displayFont = o.enumOf("displayFont", d.displayFont),
        )
    }

    fun toJson(d: BackupData): String {
        val root = JSONObject()
        root.put("app", "MyView")
        root.put("format", FORMAT)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("habits", JSONArray().also { a ->
            d.habits.forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon)
                    .put("colorIndex", it.colorIndex).put("days", it.days).put("reminderMin", it.reminderMin)
                    .put("archived", it.archived).put("createdAt", it.createdAt).put("sortOrder", it.sortOrder))
            }
        })
        root.put("habitLogs", JSONArray().also { a ->
            d.habitLogs.forEach { a.put(JSONObject().put("habitId", it.habitId).put("day", it.day)) }
        })
        root.put("exercises", JSONArray().also { a ->
            d.exercises.forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind).put("icon", it.icon)
                    .put("dailyGoal", it.dailyGoal).put("archived", it.archived).put("sortOrder", it.sortOrder))
            }
        })
        root.put("exerciseLogs", JSONArray().also { a ->
            d.exerciseLogs.forEach {
                a.put(JSONObject().put("id", it.id).put("exerciseId", it.exerciseId).put("day", it.day)
                    .put("amount", it.amount).put("createdAt", it.createdAt))
            }
        })
        root.put("water", JSONArray().also { a ->
            d.water.forEach { a.put(JSONObject().put("id", it.id).put("day", it.day).put("ml", it.ml).put("createdAt", it.createdAt)) }
        })
        root.put("sleep", JSONArray().also { a ->
            d.sleep.forEach { a.put(JSONObject().put("day", it.day).put("minutes", it.minutes).put("quality", it.quality)) }
        })
        root.put("weight", JSONArray().also { a ->
            d.weight.forEach { a.put(JSONObject().put("day", it.day).put("kg", it.kg.toDouble())) }
        })
        root.put("mood", JSONArray().also { a ->
            d.mood.forEach { a.put(JSONObject().put("day", it.day).put("mood", it.mood).put("note", it.note)) }
        })
        root.put("steps", dayCounts(d.steps, "steps"))
        root.put("focus", dayCounts(d.focus, "minutes"))
        root.put("breath", dayCounts(d.breath, "sessions"))
        d.settings?.let { root.put("settings", settingsToJson(it)) }
        return root.toString(2)
    }

    /** Throws if the text is not a MyView backup. Nothing is written to the database here. */
    fun fromJson(text: String): BackupData {
        val root = JSONObject(text)
        require(root.optString("app") == "MyView") { "This is not a MyView backup file" }
        require(root.optInt("format", 0) in 1..FORMAT) { "This backup is from a newer version of MyView" }

        fun arr(name: String): JSONArray = root.optJSONArray(name) ?: JSONArray()
        fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }

        fun counts(name: String, valueKey: String): Map<Long, Int> =
            arr(name).mapObjects { it.getLong("day") to it.getInt(valueKey) }.toMap()

        val extras = root.optInt("format", 1) >= 2
        return BackupData(
            habits = arr("habits").mapObjects {
                HabitEntity(
                    id = it.getLong("id"), name = it.getString("name"), icon = it.optString("icon", "check"),
                    colorIndex = it.optInt("colorIndex", 0), days = it.optInt("days", 127),
                    reminderMin = it.optInt("reminderMin", -1), archived = it.optBoolean("archived", false),
                    createdAt = it.optLong("createdAt", 0L), sortOrder = it.optInt("sortOrder", 0),
                )
            },
            habitLogs = arr("habitLogs").mapObjects { HabitLogEntity(it.getLong("habitId"), it.getLong("day")) },
            exercises = arr("exercises").mapObjects {
                ExerciseEntity(
                    id = it.getLong("id"), name = it.getString("name"), kind = it.optInt("kind", 0),
                    icon = it.optString("icon", "pushup"), dailyGoal = it.optInt("dailyGoal", 20),
                    archived = it.optBoolean("archived", false), sortOrder = it.optInt("sortOrder", 0),
                )
            },
            exerciseLogs = arr("exerciseLogs").mapObjects {
                ExerciseLogEntity(
                    id = it.getLong("id"), exerciseId = it.getLong("exerciseId"), day = it.getLong("day"),
                    amount = it.getInt("amount"), createdAt = it.optLong("createdAt", 0L),
                )
            },
            water = arr("water").mapObjects {
                WaterLogEntity(id = it.getLong("id"), day = it.getLong("day"), ml = it.getInt("ml"), createdAt = it.optLong("createdAt", 0L))
            },
            sleep = arr("sleep").mapObjects { SleepLogEntity(it.getLong("day"), it.getInt("minutes"), it.optInt("quality", 0)) },
            weight = arr("weight").mapObjects { WeightLogEntity(it.getLong("day"), it.getDouble("kg").toFloat()) },
            mood = arr("mood").mapObjects { MoodLogEntity(it.getLong("day"), it.getInt("mood"), it.optString("note", "")) },
            steps = if (extras) counts("steps", "steps") else emptyMap(),
            focus = if (extras) counts("focus", "minutes") else emptyMap(),
            breath = if (extras) counts("breath", "sessions") else emptyMap(),
            settings = if (extras) root.optJSONObject("settings")?.let { settingsFromJson(it) } else null,
            hasExtras = extras,
        )
    }
}
