package com.orlune.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector

enum class OrluneTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    FOCUS("Focus", Icons.Filled.Timer),
    LIMITS("Limits", Icons.Filled.HourglassBottom),
    INSIGHTS("Insights", Icons.Filled.Insights),
    SETTINGS("Settings", Icons.Filled.Settings)
}
