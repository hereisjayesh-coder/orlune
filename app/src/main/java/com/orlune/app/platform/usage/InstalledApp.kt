package com.orlune.app.platform.usage

import android.graphics.Bitmap

/** One launchable app, as resolved live from PackageManager for the app picker. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)
