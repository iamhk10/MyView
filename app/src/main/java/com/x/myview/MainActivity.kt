package com.x.myview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.MyViewRoot

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyViewRoot(vm) }
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
