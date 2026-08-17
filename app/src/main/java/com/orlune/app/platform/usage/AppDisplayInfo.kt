package com.orlune.app.platform.usage

import android.graphics.Bitmap

/** A resolved, user-safe presentation of a package name — never the raw identifier. */
data class AppDisplayInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap?
)
