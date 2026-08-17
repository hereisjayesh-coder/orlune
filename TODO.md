# Orlune — TODO

## First-launch onboarding — 2026-08-17

- [x] 11-screen onboarding flow (`feature/onboarding/`): Welcome, What Orlune does,
      Privacy, Usage Access, Blocking screen, Focus notifications, Goal, Choose apps,
      Daily limit, Finish, then Home. `OnboardingSection.kt` owns one shared
      back-stack (same `mutableStateListOf<Destination>` + `rememberSaveableStateHolder`
      pattern as `FocusSection`/`LimitsSection`/`SettingsSection`), gated at the top
      of `OrluneRoot.kt` — nothing else in `OrluneRoot` restructured.
- [x] Every permission/picker/notification-policy screen **reuses** the existing
      implementation rather than rebuilding it: `UsageAccessPermission`,
      `OverlayPermission`, `NotificationPolicyAccessPermission`, the real
      `NotificationPolicySelector`, the real `AppPickerScreen` (Multi mode, two new
      optional parameters `title`/`subtitle` + an optional `onSkip` callback, both
      backward-compatible defaults so `LimitsSection`/`FocusSection` are unaffected),
      the real `LegalCenterScreen`/`LegalDocumentScreen`, `DurationStepper`/
      `DailyLimitInput` for the daily-limit step.
- [x] New pure `core/domain/onboarding/OnboardingGoal.kt` enum (10 tags, purely
      descriptive — nothing branches on it yet, reserved for later non-AI
      personalization per the original instruction) and new
      `data/local/entity/OnboardingStateEntity.kt` + `OnboardingStateDao` +
      `data/repository/OnboardingRepository.kt` (a new singleton-row table, Room
      v3→v4, `OrluneMigrations.MIGRATION_3_4` — a new table, no existing row
      anywhere touched). Every onboarding selection lives in Bundle-safe
      `rememberSaveable` state during the flow and is committed to Room *once*,
      atomically, at "Finish" — an interrupted onboarding (process death, force-quit)
      restarts from Welcome next launch with nothing partially written.
- [x] Finish creates one `RuleEntity(type = "limit", ...)` per selected app (only if
      a daily limit wasn't skipped) via the exact same `ruleDao().upsert(...)` call
      `LimitsSection`'s "Add limit" already uses — not a parallel code path.
- [x] **Existing-install safety**: `OrluneApplication.backfillOnboardingCompletionForExistingInstalls()`
      — an unconditional one-shot cold-start check, independent of Usage
      Access/Overlay, that silently marks onboarding complete for an install
      upgrading into this feature with real pre-existing data (any session, rule, or
      focus session ever recorded), so an established user never gets shown
      onboarding for the first time just because they updated the app. A genuinely
      fresh install still shows onboarding normally.
- [x] `FocusSection` gained an optional `initialNotificationPolicy` parameter
      (default `ALLOW_ALL`, backward-compatible) — onboarding's Focus-notification
      choice becomes the real starting default the next time the user opens Focus,
      not a write-only field. Verified across a real app restart.
- [x] **Real bug found and fixed via on-device testing, not code review**:
      `OnboardingSection` was originally composed outside any `Surface`. Compose's
      default `LocalContentColor` isn't made visible by `MaterialTheme` alone — only
      a `Surface` does that, which the tab content already had and onboarding
      didn't. Every un-colored title `Text` across all 11 screens rendered fully
      present in the view tree but fully invisible (black-on-black) — caught by
      screenshot, not by the UI-tree text dump alone, which showed the text was
      "there." Fixed by wrapping the onboarding branch in the same
      `Surface(color = MaterialTheme.colorScheme.background)` the tab content uses.
      Documented as a standing rule in `AGENTS.MD` for any future top-level branch.
- [x] 14 new unit tests (`OnboardingGoalTest` 6, `OnboardingRepositoryTest` 8) + 1 new
      instrumentation test (`OrluneDatabaseMigrationTest.migrate3To4_...`) — 195/195
      unit, 30/30 instrumentation, all pass.
- [x] **Verified on the Pixel 7a**, genuine fresh install (`adb uninstall` — reported
      a misleading internal error but the app was confirmed actually removed):
      fresh onboarding through all 11 screens, granted real Usage Access (confirmed
      via `appops get`), skipped the overlay permission, selected 2 real apps via
      the real app picker, chose a 45m preset limit, completed onboarding (Finish
      summary screenshot-confirmed exact expected format), landed on the real Home
      tab with "Rules: 2" and two distinct `RuleEntity` rows confirmed in Limits,
      force-stopped and restarted the app — onboarding did not reappear, and the
      Focus tab's initial notification policy was still the onboarding-chosen
      "Silence all". Clean logcat throughout (no `FATAL`/`AndroidRuntime: java` from
      `com.orlune.app`).
- [x] Zero manifest/dependency changes — confirmed via `git diff` before commit: no
      `INTERNET` permission, no new `<queries>` entry, no new library. Onboarding
      requests permissions only through mechanisms that already existed.

## App picker, legal documentation, and launcher icon — 2026-08-17

- [x] Native app picker (`feature/apppicker/AppPickerScreen.kt`) replaced all raw
      package-name text/CSV input in Limits and Focus: real app icons and labels,
      search, "Frequently used today" (from real Insights usage data), alphabetical
      "All apps", single-select (Limits, matching `RuleEntity`'s one-package design)
      and multi-select (Focus) modes. Package names are never shown to the user.
- [x] `InstalledAppLister`/`InstalledAppSource` (`platform/usage/`) enumerate
      launchable apps via the existing `<queries>`/`CATEGORY_LAUNCHER` declaration —
      **not** `QUERY_ALL_PACKAGES`, confirmed against current Google Play policy
      research (digital-wellbeing apps are not on Google's permitted-use list for
      broad package visibility). See `docs/app-visibility-compliance.md`.
- [x] `LimitsSection`/`FocusSection` (new) own each feature's app-picker sub-navigation
      as a manual back-stack, matching `SettingsSection`'s established pattern.
      **Real bug caught and fixed during this work**: the first version passed raw
      `data object`/`data class` destinations as `SaveableStateHolder.SaveableStateProvider`
      keys, which crashed on launch (`IllegalArgumentException: Type of the key Root
      is not supported`) — `SaveableStateProvider` keys must be Bundle-storable.
      Fixed by using `.toString()` keys across all three Section files.
- [x] Launcher icon implemented from `design/orlune-logo-reference.png` (the
      user-approved concept #4 reference, added to the repo this session): the
      gold/gray split-ring clock mark extracted via flood-fill background removal
      (a simple brightness threshold couldn't distinguish the mark's dark-gray half
      from the reference's card border/grain texture), shipped as raster adaptive-icon
      foreground + monochrome layers at all 5 density buckets — not hand-vectorized,
      to avoid drifting from the approved reference. `ic_launcher_background` changed
      to `#000000` to exactly match the reference.
- [x] Privacy & Legal Center expanded: Privacy Policy (23 sections) and Terms of
      Service (28 sections) rewritten to the full requested structure, informed by
      this session's research into India's DPDP Act 2023/Rules 2025, GDPR/UK GDPR,
      CCPA/CPRA, COPPA, and current Google Play Data Safety policy. All 15 documents
      cross-reference the new compliance docs. No legal entity/address/contact was
      invented — all business/legal unknowns remain explicit `[TBD]` placeholders.
      **Real rendering bug caught and fixed**: a markdown table in the Privacy Policy
      draft would have rendered as broken literal pipe-text, since the in-app markdown
      subset doesn't support tables — replaced with a bullet list.
- [x] New compliance docs: `docs/app-visibility-compliance.md`,
      `docs/legal-compliance-matrix.md` (jurisdiction/law/requirement/implementation/
      open-issue table), `docs/google-play-privacy-compliance.md`.
- [x] "About Orlune" is now directly reachable from Settings root (not just nested in
      the Legal Center list), showing real branding (the actual launcher icon mark),
      version/build, and an explicit "not open source — no LICENSE file exists"
      statement (checked against the actual repository, not assumed).
- [x] New tests: `DailyLimitInputTest` (already existed, unchanged), `LegalDocumentsTest`
      (routing/data-integrity, 7 tests), `ThemePreferenceDaoInstrumentedTest`
      (persistence round-trip, 4 tests), `InstalledAppListerInstrumentedTest`
      (package→label, package→icon, exclusion, sorting, dedup, 5 tests).
- [x] Verified on the Pixel 7a: full app-picker flow (search, multi-select with
      remove, single-select instant-return, end-to-end rule creation with real label
      resolution in "Active rules"), launcher icon in the app drawer and App Info
      page, About Orlune, System/Light/Dark theme switching on the Legal Center,
      and a full process force-stop + relaunch (state and theme survived). Clean
      logcat throughout — no `FATAL EXCEPTION` in the final, fixed build.
- [x] Zero manifest or dependency changes this session — confirmed via `git diff`
      before commit: no `INTERNET` permission, no new library of any kind.

## Current takeover checkpoint — 2026-08-16
- [x] Replaced the launch surface with a black-first Compose shell: Home, Focus,
      Limits, Insights, and Settings, backed by the existing repositories.
- [x] Added local JSON export through a user-initiated Android share action and a
      delete-all-local-data control; no network or backup path was added.
- [x] Added repository boundary validation for focus duration/package inputs and
      fail-safe handling for malformed persisted schedules.
- [x] Updated the launcher asset and reconciled project-state documentation.
- [ ] Repeat full UI, permission, blocking, export, and delete flows on the physical
      Pixel 7a after reconnecting it.

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

## Phase 5 — App Blocking
- [x] Detection: `ForegroundAppDetector` (a live `UsageStatsManager` poll over a short trailing window) was the *first* implementation, but real-device testing exposed a real bug — it loses track of any session open longer than its lookback window, which is exactly the "user kept using the blocked app" case blocking exists to catch. Replaced with deriving "current foreground app" from `SessionDao.getOpenSessions()` — the same session data Phase 3's pipeline already maintains — and deleted `ForegroundAppDetector`/`ForegroundAppSource` entirely
- [x] `BlockingRepository` (`data/repository/BlockingRepository.kt`) orchestrates: loads the open session, matches `RuleEntity` rows by `targetPackageOrCategory`, dispatches `type` ("limit"/"schedule") to the **unmodified** Phase 4 `LimitEngine`/`ScheduleEngine`, reduces to one `anyRuleTriggered` boolean, calls the **unmodified** `BlockingEngine.decide(...)`. A limit check adds the currently-open session's elapsed time (clamped to today) on top of `DailyUsageEntity`'s aggregated total — without this, a single continuous session over the limit would never trigger a block until the app was backgrounded, since aggregation only runs on session close
- [x] `BlockingMonitorService` (`platform/blocking/`) — foreground service (`specialUse` type, Android 14+ property declared), 3s poll loop (within the platform doc's 2–5s target), self-stops when no rules remain or Usage Access is revoked, never started unless a rule exists (debug-UI toggle, or automatically at app cold start if a rule already exists and both permissions are granted — one-shot check, not a persistent watcher)
- [x] `BlockOverlayController` (`platform/blocking/`) — plain Android `View` (not Compose — hosting Compose outside an Activity needs a hand-rolled `LifecycleOwner`/`SavedStateRegistryOwner`/`ViewModelStoreOwner`) added via `WindowManager`/`TYPE_APPLICATION_OVERLAY`. **Real bug caught and fixed here**: the first version called `WindowManager.addView`/`removeView` from the polling loop's background dispatcher, which throws `Can't create handler inside thread ... that has not called Looper.prepare()` — silently swallowed by the original fail-safe catch block until diagnostic logging (kept in the shipped code) surfaced it. Fixed by hopping to `Dispatchers.Main` around just the overlay calls
- [x] `OverlayPermission`/`NotificationPermission` (`platform/blocking/`) — same manual-Settings-grant shape as Phase 3's `UsageAccessPermission`. Notifications are requested for UX only; the foreground service runs regardless of whether it's granted
- [x] `OrluneApplication` — added `blockingRepository`; added the one-shot cold-start resume check
- [x] `MainActivity` debug screen extended (same "functional, not polished" spirit as Phase 3): overlay/notification permission buttons, start/stop-monitoring buttons, forms to create a test daily-limit rule, a test schedule rule, and an essential-app allow-list entry, plus a live rules/allow-list listing — the only way to exercise Phase 5 before Phase 6/8 build a real rule-builder UI
- [x] No Room schema changes — `RuleEntity`/`ScheduleEntity`/`AppListEntryEntity` from Phase 2 already had everything needed; all reads go through existing DAOs' Flow-returning queries via `.first()`
- [x] Manifest: added `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, an explicit `FOREGROUND_SERVICE` declaration (already present transitively via WorkManager; now directly justified), and the `BlockingMonitorService` `<service>` block with `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
- [x] 14 new unit tests (`BlockingRepositoryTest`, hand-rolled DAO fakes, no mocking library) + 6 new instrumentation tests (`BlockingRepositoryInstrumentedTest`, real in-memory Room DB) — 62 unit / 9 instrumentation tests total, all pass
- [x] **Verified on a real device (Pixel 7a, 2026-08-16):** created a daily-limit rule and a schedule rule against Calculator via the debug UI; both correctly triggered the full-screen block overlay; essential-app allow-list correctly overrode a triggered rule; "Go to Orlune" correctly dismissed the overlay and returned to an allowed state; Usage Access revocation mid-run stopped the service cleanly (overlay cleared, no crash, process survived) and re-granting + an app restart resumed monitoring and blocking automatically; removing the last rule self-stopped the service (confirmed via `dumpsys activity services` — no infinite loop, no residual foreground service)
- [x] Known, documented limitations (not solved, per the user's explicit scope): detection latency is a few seconds, not instant (no AccessibilityService); a brief flash of the blocked app before the overlay appears is possible; Android 12+ apps can opt out of overlays via `HIDE_OVERLAY_WINDOWS`; OEM background-kill (MIUI/One UI/etc.) can force-stop the service regardless of standard Doze exemptions — not fought (no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no OEM autostart guidance), the cold-start resume check just makes recovery automatic once the process does restart
- [x] Re-verified: merged manifest checked, still no `INTERNET` permission

## Phase 6 — Focus Sessions & Scheduling (partial)

- [x] `FocusSessionEngine` (`core/domain/focus/FocusSessionEngine.kt`) — pure Kotlin, no stored status column: `stateOf()` derives `SCHEDULED`/`ACTIVE`/`COMPLETED`/`INTERRUPTED` from `startTs`/`endTs`/`plannedMinutes`/`completedMinutes` and the current time, so a crash/reboot/restart can't leave a session desynced. `tick()` finalizes at the *exact* planned instant (`startTs + plannedMinutes`), not "now" — deterministic regardless of how late a restart's next poll runs. `interrupt()` is the manual "Stop" path.
- [x] `FocusSessionRepository` (`data/repository/FocusSessionRepository.kt`) — `startSession`/`cancelActiveSessions`/`reconcileActiveSessions`, mirrors `UsageRepository`'s no-dispatcher-switching shape. `reconcileActiveSessions()` is called every `BlockingMonitorService` tick, same cadence as usage/blocking evaluation.
- [x] `BlockingRepository.evaluate()` extended: an active focus session's `blockedPackages` (comma-separated, parsed the same way as elsewhere in this codebase) is OR'd into the same `anyTriggered` boolean as rule evaluation — `BlockingEngine.decide()` itself is unmodified, so the essential-app allow-list overrides a focus-session block exactly the same way it overrides a triggered rule. Multiple concurrently-active sessions union their blocked-package sets (no special-casing needed).
- [x] `BlockingMonitorService.hasWorkToEnforce()` extended to also keep the service alive for a `SCHEDULED` (not-yet-started) focus session, not just `ACTIVE` ones — otherwise a session scheduled to start later would never reach `ACTIVE` because the service would have already self-stopped.
- [x] `MainActivity` debug screen extended: start-now and start-in-N-minutes (one-time scheduled) forms, a "Stop active session" button, a live sessions list showing derived state — same "functional, not polished" debug-only pattern as Phases 3/5. No real UI.
- [x] 19 new unit tests (`FocusSessionEngineTest` 13, `FocusSessionRepositoryTest` 6) + instrumentation coverage added to `BlockingRepositoryInstrumentedTest` (active-focus-session-blocks, allow-list-overrides-focus-session) — all pass, including on a real device (2026-08-16 audit re-verification).
- [x] Room schema bump to version 2 (`OrluneMigrations.MIGRATION_1_2`) — see "Database migration" below.
- [ ] **Not done**: recurring focus-session scheduling (only immediate/one-time-delayed sessions exist) — do not add without reviewing this design with the user first, per the standing project rule.
- [ ] **Not done**: boundary validation on `plannedMinutes` (must be `> 0`) at `FocusSessionRepository`/`FocusSessionEngine` — currently only enforced by `MainActivity`'s form; see `docs/PROJECT_STATE.md` known risks.
- [ ] **Not done at the time**: this phase's commits (`96f1d51`, `96b93d4`) landed without updating this file, `ROADMAP.md`, or `README.md` — reconciled retroactively in the 2026-08-16 audit. If you're an agent reading this: always update these three files in the same commit as the feature, not after.

### Database migration (Room v1 → v2)

- [x] `OrluneMigrations.MIGRATION_1_2` (`data/local/OrluneMigrations.kt`) — adds `schedules.name TEXT NOT NULL DEFAULT ''` and `focus_sessions.blockedPackages TEXT NOT NULL DEFAULT ''`. Plain `ALTER TABLE ADD COLUMN`, non-destructive by construction.
- [x] `OrluneApplication`'s `Room.databaseBuilder(...)` wires the migration via `.addMigrations(OrluneMigrations.MIGRATION_1_2)` — **not** `fallbackToDestructiveMigration()`. (`OrluneDatabase.kt`'s class KDoc previously claimed the opposite — a stale comment written before the migration existed; fixed in the 2026-08-16 audit.)
- [x] `OrluneDatabaseMigrationTest` (`androidTest`) — reconstructs the exact v1 schema via a `SupportSQLiteOpenHelper.Callback`, inserts one representative row into all 16 v1 tables, runs the real `MIGRATION_1_2`, and asserts every table's row count *and* actual column values survived, including the two new columns getting their `''` default. Re-run and passing on a real device (Pixel 7a) as of the 2026-08-16 audit.
- [x] Schema exported: `app/schemas/com.orlune.app.data.local.OrluneDatabase/2.json` committed alongside `1.json`.

### Bugs found and fixed in the 2026-08-16 audit (not part of the original Phase 5/6 work)

- [x] `BlockingEngine.decide()` let list *order* decide allow/block precedence instead of entry *type* — a package with both a "block" and an "allow" `AppListEntryEntity` row (legal: the primary key is `(packageName, listType)`) would silently get BLOCKed, contradicting the documented "ALLOW always wins" precedence. Not reachable via the current debug UI (no block-list form exists yet), but live in the DAO/entity layer. Fixed: precedence is now decided by the set of list types present, not concatenation order. Regression test added.
- [x] `OrluneDatabase.kt`'s class KDoc claimed `fallbackToDestructiveMigration()` was in use — stale, written before the real migration was added, and actively misleading (see "Database migration" above). Fixed.
