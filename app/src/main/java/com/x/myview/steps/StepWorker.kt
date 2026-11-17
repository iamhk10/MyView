package com.x.myview.steps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.x.myview.MyViewApp
import com.x.myview.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Runs about every 15 minutes: takes a step reading (so steps are filed under the right day even if
 * the app is closed) and refreshes the home-screen widgets (which also moves them over midnight).
 */
class StepWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as MyViewApp).container
        container.stepTracker.readOnce()
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("myview_steps", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
