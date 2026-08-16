# Orlune — TODO

## Phase 0 — Research & Feasibility
- [x] Competitor research (Opal, Digital Wellbeing, ScreenZen, one sec, Freedom, StayFree)
- [x] Android platform capability research → `docs/android-platform-capabilities.md`
- [x] AccessibilityService feasibility & compliance research → `docs/accessibility-service-compliance.md`
- [x] Google Play compliance research
- [x] Consolidated Phase 0 deliverable → `docs/phase-0-research.md`
- [x] Naming-conflict check on original name "Grove" (flagged: collision with "Focus Grove" → renamed to "Orlune")
- [x] Naming-conflict check on "Orlune" (no category collision found)
- [x] User decision on open questions in `docs/phase-0-research.md` Section 17 — **resolved 2026-08-16**

## Phase 1 — Architecture & Android Project Setup
- [x] Gradle/Kotlin/Compose project skeleton (`com.orlune.app`, minSdk 29, compileSdk 37, targetSdk 36)
- [x] AndroidManifest with no permissions (matches zero-network privacy architecture, Section 10)
- [x] Bare Compose `MainActivity` showing "Orlune" — proves the toolchain wires up, no real UI yet
- [x] Package structure scaffolded (`core/`, `data/`, `feature/`, `ui/`, `platform/`) — see `ARCHITECTURE.md`
- [x] `docs/dependency-audit.md` created and current
- [x] **Build verified.** Root cause of the original failure: `gradle-wrapper.properties` pointed at `gradle-9.2-bin.zip`, a version that was never released (HTTP 404) — AGP 9.2.0 actually requires Gradle ≥ 9.4.1. Fixed by pointing the wrapper at Gradle 9.7.0 (current stable) and committing `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` (fetched from the official Gradle GitHub release tag, since no local Gradle install existed to generate them).
- [x] Second build failure fixed: the `org.jetbrains.kotlin.android` plugin is no longer compatible with AGP 9.0+ (Kotlin support is now built in) — removed it from `build.gradle.kts`, `app/build.gradle.kts`, and `gradle/libs.versions.toml`; `kotlinOptions { jvmTarget }` removed too since AGP 9 infers it from `compileOptions.targetCompatibility` automatically.
- [x] `GRADLE_USER_HOME` set to `F:\GradleUserHome` (user env var) so Gradle's distribution/dependency caches don't land on the nearly-full `C:` drive.
- [x] Verified: no `INTERNET` permission or networking dependency anywhere (source manifest, merged manifest, and `app/build.gradle.kts` all checked)
- [x] **Exact verified build command:** `.\gradlew.bat assembleDebug --stacktrace` → `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`
- [x] `adb devices` confirmed working (no physical device connected yet — connect one with USB debugging enabled to install/run)
- [ ] One pre-existing, non-blocking issue: a Gradle-10-incompatibility deprecation warning originates from inside AGP 9.2.0 itself (not from this project's build files) — nothing to fix here, just something to recheck when AGP is next upgraded.

## Phase 2 — Local Database & Domain Models
- [x] All 16 entities from `docs/phase-0-research.md` Section 8 implemented as Room `@Entity` classes (`data/local/entity/`)
- [x] One Room DAO per entity (`data/local/dao/`), Flow-returning queries per Phase 0 Section 2's coroutines/Flow decision
- [x] `OrluneDatabase.kt` — single `@Database(version = 1, exportSchema = true)` wiring all 16 DAOs
- [x] Room schema exported to `app/schemas/` and committed (baseline for future migrations)
- [x] Room 2.8.4 + KSP 2.3.11 added — required pinning Kotlin to 2.3.20 (not 2.3.21) to exactly match the only available KSP release; see `docs/dependency-audit.md` and `ARCHITECTURE.md`
- [x] `docs/dependency-audit.md` updated with Room/KSP/coroutines entries
- [x] **Build verified:** `.\gradlew.bat assembleDebug --stacktrace` → `BUILD SUCCESSFUL`, `kspDebugKotlin` and `copyRoomSchemas` both ran, schema JSON confirmed to contain all 16 tables
- [x] Re-verified: still no `INTERNET` permission or networking dependency anywhere after adding Room
- [ ] Nothing consumes `OrluneDatabase` yet (no `Room.databaseBuilder` call) — by design, no feature needs it until Phase 3

## Phase 3 — Usage Monitoring
- [x] Usage Access permission flow: `UsageAccessPermission` (AppOpsManager check + Settings intent), functional debug-screen onboarding in `MainActivity` that re-checks on resume
- [x] Pipeline: `UsageEventReader` -> `SessionCalculator` -> `UsageAggregator` -> `UsageRepository` -> Room, per `docs/phase-0-research.md` Section 1
- [x] `App`, `DailyUsage`, `Session` tables populated by the pipeline (not just declared, per Phase 2)
- [x] Edge cases handled: midnight rollover (session splitting), timezone changes (day boundaries computed at processing time, not cached), reboot (DEVICE_SHUTDOWN closes open sessions; WorkManager reschedules itself across reboots automatically), missing/orphaned events (still-open sessions carried forward across runs), duplicate processing (watermark advanced past the last-seen event — **caught and fixed a real off-by-one here**: the watermark wasn't advancing past the last event's own timestamp, which would have reprocessed it every run)
- [x] Raw events aggregated into `DailyUsageEntity`, not retained indefinitely — `SessionEntity` rows persist (short retention per Section 8) but nothing stores raw `UsageEvents` beyond one processing pass
- [x] `PeriodicWorkRequest` (15 min, WorkManager) drives background aggregation; manual DI via a custom `WorkerFactory` — **caught and fixed a real bug here**: the manifest's `tools:node="remove"` initially targeted the wrong meta-data key (`androidx.work.impl.WorkManagerInitializer` instead of the actual `androidx.work.WorkManagerInitializer`), which silently no-op'd and would have let WorkManager's default (uninjected) initializer win at runtime
- [x] Unit tests: 15 tests across `SessionCalculatorTest` (11) and `UsageAggregatorTest` (4), covering pairing, midnight/multi-day splitting, orphaned/open sessions, device shutdown, duplicate foreground events, zero-length pairings — **all pass**
- [x] Instrumentation tests: `UsageRepositoryInstrumentedTest` (3 tests) against a real in-memory Room database with fake platform sources — **compiles and builds successfully, but could not be executed**, no physical device connected (`adb devices` empty) and no emulator installed per the no-emulator-on-8GB-RAM strategy. Run `.\gradlew.bat connectedDebugAndroidTest` once a device is connected via USB debugging.
- [x] Build verified: `.\gradlew.bat assembleDebug`, `testDebugUnitTest`, and `assembleDebugAndroidTest` all succeed
- [x] Re-verified: still no `INTERNET` permission. Two new permissions added, both required: `PACKAGE_USAGE_STATS` (the feature itself) and a scoped `<queries>` `CATEGORY_LAUNCHER` filter (not `QUERY_ALL_PACKAGES`) for app labels. WorkManager transitively adds `RECEIVE_BOOT_COMPLETED`/`WAKE_LOCK`/`FOREGROUND_SERVICE`/`ACCESS_NETWORK_STATE` — unused by this codebase, none enable networking. See `ARCHITECTURE.md`.
- [x] **Verified on a real device (Pixel 7a, 2026-08-16):** `.\gradlew.bat connectedDebugAndroidTest` → 3/3 `UsageRepositoryInstrumentedTest` tests pass against real Android SQLite (exit code 0, `app/build/outputs/androidTest-results/connected/debug/`). Manually exercised the debug UI end-to-end: granted Usage Access, used a few apps, tapped refresh, confirmed real per-app minutes appeared (`Settings`, `LinkedIn`, `WhatsApp`, etc.) — proving the full `UsageEventReader -> SessionCalculator -> UsageAggregator -> UsageRepository -> Room` pipeline against live data, not just fakes. Required adding the `androidx-test-runner` dependency (was missing, needed by the on-device test runner).

## Phase 4 — Deterministic Rule Engine
- [x] Scoped down from Section 9's full 10-module list to the 4 modules not already covered: `UsageCalculator`/`SessionCalculator` were already built in Phase 3 (`UsageAggregator`/`SessionCalculator`); `Statistics`/`Consistency`/`Recommendation` engines belong to Phase 7 per `ROADMAP.md`; `DigitalBalanceEngine` is explicitly deferred to Phase 7 in Section 9 itself. `FrictionEngine` deferred too — no schema field stores a friction-delay config anywhere, and Section 13 (MVP scope) defers the Friction blocking level past MVP.
- [x] `LimitEngine` (`core/domain/rules/LimitEngine.kt`) — binary `UNDER_LIMIT`/`AT_OR_OVER_LIMIT` evaluation of usage vs. `RuleEntity.threshold`, not the illustrative 3-tier "warn/friction/block" (no warning-percentage field exists in the schema to back a third state)
- [x] `ScheduleEngine` (`core/domain/rules/ScheduleEngine.kt`) — parses `ScheduleEntity`'s CSV days-of-week and `HH:mm` times, evaluates a half-open `[start, end)` window against a given `LocalDateTime`. Handles overnight windows (`startTime > endTime`, e.g. 22:00–06:00) as two segments: the evening half on the scheduled day, and the morning half on the day *after* — verified with a schedule wrapping Sunday night into Monday morning
- [x] `BlockingEngine` (`core/domain/rules/BlockingEngine.kt`) — combines `AppListEntryEntity` (block/allow list) with a triggered-rule flag into one `ALLOW`/`BLOCK` decision. Documented precedence: explicit allow-list entry (essential-app exemption, Section 13) always wins, explicit block-list entry wins next, otherwise follows whether any rule targeting the app is triggered
- [x] `GoalEngine` (`core/domain/rules/GoalEngine.kt`) — `completedUnits / plannedUnits` ratio, zero-guarded against a non-positive `plannedUnits`, not clamped above 1.0 for over-completion
- [x] All four engines are pure Kotlin (no Android framework dependency, no DB/repository/WorkManager wiring) — enforcement wiring (reading real `RuleEntity` rows, dispatching by type, applying to live usage data) is Phase 5/6's job, not this phase's
- [x] 33 unit tests across `LimitEngineTest` (6), `ScheduleEngineTest` (14), `BlockingEngineTest` (7), `GoalEngineTest` (6) — all pass. No mocking library used (none exists in this project), matching Phase 3's hand-rolled-fakes convention (not needed here since these engines take plain data, no collaborators to fake)
- [x] Build verified: `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug` both succeed
- [x] Re-verified: merged manifest unchanged, still no `INTERNET` permission — this phase touched no manifest, no dependency, no database schema (no migration, no version bump; existing `RuleEntity`/`ScheduleEntity`/`AppListEntryEntity`/`GoalEntity` already had everything these engines needed)
