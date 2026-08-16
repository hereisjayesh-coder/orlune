# Orlune — Dependency Audit

Every dependency is recorded here before it's added, per the Phase 0 dependency policy (`phase-0-research.md`, Section 2): purpose, license, network behavior, telemetry behavior, and privacy implication must all be justified. Nothing here may make a network request or collect telemetry — that's a standing constraint, not a one-time check.

| Dependency | Version | Purpose | License | Network behavior | Telemetry | Privacy implication |
|---|---|---|---|---|---|---|
| `androidx.core:core-ktx` | 1.16.0 | Kotlin extensions over Android framework APIs | Apache 2.0 | None | None | None |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.9.0 | Lifecycle-aware coroutines (`lifecycleScope`, etc.) | Apache 2.0 | None | None | None |
| `androidx.activity:activity-compose` | 1.10.1 | Compose integration for `ComponentActivity` | Apache 2.0 | None | None | None |
| `androidx.compose:compose-bom` | 2026.08.00 | Version alignment (BOM) for Compose artifacts below | Apache 2.0 | None | None | None |
| `androidx.compose.ui:ui` | via BOM | Core Compose UI toolkit | Apache 2.0 | None | None | None |
| `androidx.compose.ui:ui-graphics` | via BOM | Compose graphics primitives | Apache 2.0 | None | None | None |
| `androidx.compose.ui:ui-tooling-preview` | via BOM | `@Preview` support (compile-time only) | Apache 2.0 | None | None | None |
| `androidx.compose.ui:ui-tooling` | via BOM | Compose layout inspector/preview rendering (**debug-only**, `debugImplementation`, never in release builds) | Apache 2.0 | None | None | None |
| `androidx.compose.material3:material3` | via BOM | Material 3 components (placeholder UI only — real design system is Phase 8) | Apache 2.0 | None | None | None |
| `androidx.room:room-runtime` | 2.8.4 | Local SQLite persistence (Section 8 data model) — pre-approved by Phase 0 Section 2 ("Room over raw SQLite") | Apache 2.0 | None | None | None — local file only, never backed up (`allowBackup=false`) |
| `androidx.room:room-ktx` | 2.8.4 | Flow-returning DAO queries, coroutine support for Room | Apache 2.0 | None | None | None |
| `androidx.room:room-compiler` | 2.8.4 | Annotation processor (via KSP) generating DAO implementations at compile time — not shipped in the app | Apache 2.0 | None | None | None |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Structured concurrency, `Dispatchers.Main` on Android — pre-approved by Phase 0 Section 2 ("Kotlin Coroutines + Flow/StateFlow") | Apache 2.0 | None | None | None |

**Build/tooling plugins** (not shipped in the app, but part of the trusted build chain):

| Plugin | Version | Purpose | License |
|---|---|---|---|
| Android Gradle Plugin (`com.android.application`) | 9.2.0 | Android build system; Kotlin compilation is now built in (no separate `org.jetbrains.kotlin.android` plugin — see AGP 9 migration note in `app/build.gradle.kts`) | Apache 2.0 |
| Kotlin Compose compiler (`org.jetbrains.kotlin.plugin.compose`) | 2.3.20 | Compose compiler plugin, also pins the Kotlin compiler version used for the whole build. Pinned to 2.3.20 rather than the newer 2.3.21 so it exactly matches the latest available KSP release (below) | Apache 2.0 |
| KSP (`com.google.devtools.ksp`) | 2.3.11 | Annotation processing for Room. KSP's version scheme now tracks Kotlin directly; 2.3.11 is the latest release and targets Kotlin 2.3.20 exactly (confirmed via its `gradle.properties`) | Apache 2.0 |
| Room Gradle plugin (`androidx.room`) | 2.8.4 | Configures Room's schema export directory (`app/schemas/`) | Apache 2.0 |
| Gradle | 9.7.0 | Build tool, invoked only via the committed wrapper (`gradlew`/`gradlew.bat`), never a system-wide install | Apache 2.0 |

## What's deliberately absent

No networking client (Retrofit/OkHttp/Ktor/Volley), no Firebase/analytics/crash-reporting SDK, no ad SDK, no DI framework (manual constructor injection per Phase 0 Section 2 — Hilt only if the object graph outgrows it later, and Hilt itself has no network/telemetry behavior when it's added). Nothing in this project requests the `INTERNET` permission; see `AndroidManifest.xml`.

## Process going forward

Before adding any new dependency in a later phase (Room/Coroutines/WorkManager are already pre-approved by Phase 0 Section 2, but still get an entry here once actually added): add a row to this table with the same five checks, and confirm minSdk 29 compatibility before pinning a version.
