package com.x.myview.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/** Returns a function that shows Android's permission dialog for [permission]. */
@Composable
fun rememberPermissionRequest(permission: String, onResult: (Boolean) -> Unit = {}): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return { launcher.launch(permission) }
}

/** Asks for the notification permission (only Android 13+ has one; earlier versions do nothing). */
@Composable
fun rememberNotificationRequest(): () -> Unit {
    val request = rememberPermissionRequest(Manifest.permission.POST_NOTIFICATIONS)
    return { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) request() }
}
