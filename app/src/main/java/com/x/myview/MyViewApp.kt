package com.x.myview

import android.app.Application
import com.x.myview.di.AppContainer
import com.x.myview.reminders.Reminders
import com.x.myview.steps.StepWorker

class MyViewApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.ensureChannels(this)
        Reminders.scheduleAllAsync(this)
        if (container.stepTracker.isSupported) StepWorker.schedule(this)
    }
}
