package com.x.myview

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.x.myview.run.RunTracker
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.MyViewRoot
import com.x.myview.ui.PendingRoute

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch carries a shortcut; after a rotation the route was already handled.
        // A run that is still going (the service keeps tracking while the app is away) takes you straight back to it.
        if (savedInstanceState == null) {
            PendingRoute.route.value = intent?.getStringExtra("route") ?: if (RunTracker.isActive()) "run" else null
        }
        setContent { MyViewRoot(vm) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PendingRoute.route.value = intent.getStringExtra("route")
    }

    override fun onStart() {
        super.onStart()
        vm.startSteps()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshDay()
    }

    override fun onStop() {
        vm.stopSteps()
        super.onStop()
    }
}
