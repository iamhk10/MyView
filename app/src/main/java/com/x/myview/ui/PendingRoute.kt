package com.x.myview.ui

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A screen asked for from outside the app (a launcher shortcut). MainActivity sets it from the intent,
 * the root composable opens it once and clears it. Values: "tab:N" for a tab, otherwise an overlay key.
 */
object PendingRoute {
    val route = MutableStateFlow<String?>(null)
}
