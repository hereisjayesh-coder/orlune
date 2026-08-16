package com.orlune.app.platform.usage

/**
 * Contract for [UsageRepository][com.orlune.app.data.repository.UsageRepository]'s
 * app-label lookup. Exists so instrumentation tests can substitute a fake instead of
 * depending on which apps happen to be installed on the test device.
 */
interface AppLabelSource {
    fun resolveLabel(packageName: String): String
}
