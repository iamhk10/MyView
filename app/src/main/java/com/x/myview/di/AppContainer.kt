package com.x.myview.di

import android.content.Context
import androidx.room.Room
import com.x.myview.data.Repository
import com.x.myview.data.SettingsStore
import com.x.myview.data.db.MIGRATION_1_2
import com.x.myview.data.db.MyViewDatabase
import com.x.myview.steps.StepTracker

/** Hand-made dependency holder. Created once by [com.x.myview.MyViewApp]. */
class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(context, MyViewDatabase::class.java, "myview.db")
        .addMigrations(MIGRATION_1_2)
        .build()
    val repository = Repository(db)
    val settings = SettingsStore(context)
    val stepTracker = StepTracker(context)
}
