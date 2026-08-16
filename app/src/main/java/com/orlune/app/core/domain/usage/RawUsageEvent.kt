package com.orlune.app.core.domain.usage

/**
 * The subset of `UsageEvents.Event` that session calculation actually needs.
 * Kept Android-framework-free so [SessionCalculator] is a plain unit-testable
 * function — the mapping from `android.app.usage.UsageEvents` lives in
 * `platform.usage.UsageEventReader`, not here.
 */
data class RawUsageEvent(
    val packageName: String,
    val type: UsageEventType,
    val timestamp: Long
)

enum class UsageEventType {
    FOREGROUND,
    BACKGROUND,

    /** Emitted around device shutdown — treated as an implicit close for any open session. */
    DEVICE_SHUTDOWN,

    /** Not used for session pairing; kept so callers can see the full raw stream if needed. */
    OTHER
}
