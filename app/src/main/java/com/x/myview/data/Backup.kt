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

/** Everything the user has logged. Settings and step history are not part of a backup. */
class BackupData(
    val habits: List<HabitEntity>,
    val habitLogs: List<HabitLogEntity>,
    val exercises: List<ExerciseEntity>,
    val exerciseLogs: List<ExerciseLogEntity>,
    val water: List<WaterLogEntity>,
    val sleep: List<SleepLogEntity>,
    val weight: List<WeightLogEntity>,
    val mood: List<MoodLogEntity>,
)

/** Reads and writes backups as one readable JSON file. */
object Backup {
    private const val FORMAT = 1

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
        return root.toString(2)
    }

    /** Throws if the text is not a MyView backup. Nothing is written to the database here. */
    fun fromJson(text: String): BackupData {
        val root = JSONObject(text)
        require(root.optString("app") == "MyView") { "This is not a MyView backup file" }
        require(root.optInt("format", 0) in 1..FORMAT) { "This backup is from a newer version of MyView" }

        fun arr(name: String): JSONArray = root.optJSONArray(name) ?: JSONArray()
        fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }

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
        )
    }
}
