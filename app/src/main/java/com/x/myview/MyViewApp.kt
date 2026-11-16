package com.x.myview

import android.app.Application
import com.x.myview.di.AppContainer

class MyViewApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
