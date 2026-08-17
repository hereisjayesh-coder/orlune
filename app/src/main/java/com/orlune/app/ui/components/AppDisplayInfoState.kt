package com.orlune.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.orlune.app.platform.usage.AppDisplayInfo
import com.orlune.app.platform.usage.AppDisplayResolver
import com.orlune.app.platform.usage.InstalledAppSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves live label/icon presentation for a list of (packageName, storedLabel)
 * pairs off the main thread, keyed by the package-name list so it only re-resolves
 * when the underlying set of packages changes. Returns an immediate best-effort
 * result (from [storedLabel], never a raw package name) before the PackageManager
 * lookup completes, so the UI never flashes a raw package identifier while loading.
 */
@Composable
fun rememberAppDisplayInfos(
    installedAppSource: InstalledAppSource,
    entries: List<Pair<String, String?>>
): Map<String, AppDisplayInfo> {
    val packageNames = entries.map { it.first }
    var resolved by remember(packageNames) {
        mutableStateOf(entries.associate { (packageName, storedLabel) ->
            packageName to AppDisplayInfo(
                packageName = packageName,
                label = storedLabel?.takeIf { it.isNotBlank() && it != packageName } ?: "Unknown app",
                icon = null
            )
        })
    }
    LaunchedEffect(packageNames) {
        resolved = withContext(Dispatchers.Default) {
            val resolver = AppDisplayResolver(installedAppSource)
            entries.associate { (packageName, storedLabel) -> packageName to resolver.resolve(packageName, storedLabel) }
        }
    }
    return resolved
}
