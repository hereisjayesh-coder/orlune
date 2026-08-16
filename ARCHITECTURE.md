# Orlune — Architecture

This is the as-built companion to `docs/phase-0-research.md` Section 1 (the original architecture diagram) and Section 8/9 (data model and algorithm layer, not yet implemented). It records real decisions made while scaffolding the project, not aspirational design.

## Module structure: single Gradle module, package-layered

The project is one Gradle module (`:app`), not a multi-module build. Phase 0's tech-stack decision (Section 2) was deliberately minimal — "Manual DI initially... avoid a dependency until the object graph actually justifies it" — and a multi-module Gradle setup is the same kind of premature complexity at this stage: nothing in Phase 1 needs module-level build isolation. The architecture's layers (platform, data, core, feature, ui) exist as **Kotlin packages** under `com.orlune.app`, not as separate Gradle modules. This can change later if a real need shows up (e.g. build-time isolation, a genuinely reusable layer) — it's not a permanent constraint.

```
com.orlune.app/
├── platform/
│   └── usage/          UsageStatsManager integration (Phase 3)
├── data/
│   ├── local/           Room database, entities, DAOs — done (Phase 2)
│   └── repository/      Repository implementations bridging data ↔ domain (Phase 3+, once something consumes the schema)
├── core/
│   ├── domain/          Business models, use-cases (Phase 3+ — Section 8's entities *are* the domain model for now; a separate mapping layer isn't justified until a consumer needs one)
│   ├── database/        Cross-cutting DB utilities (migrations, converters) — empty; none needed yet at schema version 1
│   ├── privacy/         Privacy Center logic — permission status, export/delete (Phase 9)
│   └── security/        Threat-model-driven safeguards (Phase 10)
├── feature/
│   ├── onboarding/       Welcome, privacy promise, Usage Access setup (MVP)
│   ├── home/             Today's usage, active rules, focus/block status (MVP)
│   ├── settings/         App settings
│   └── privacy/          Privacy Center UI (Phase 9)
└── ui/
    ├── theme/            Compose theme (exists — placeholder Material 3 baseline, real Light/Dark/Forest theming is Phase 8)
    ├── components/        Shared Compose components
    └── navigation/        Navigation graph
```

All of the above except `ui/theme` and the root `MainActivity.kt` are currently **empty packages** (a `.gitkeep` only) — this is Phase 1 structure, not Phase 1 features. Each fills in during the phase that owns it, per `docs/phase-0-research.md` Section 14.

Testing uses Android's standard source sets — `app/src/test` (JVM unit tests) and `app/src/androidTest` (instrumented tests) — rather than a top-level `tests/` folder, since that's what Gradle's Android plugin actually wires up test tasks against.

## Build toolchain (Phase 1, verified 2026-08-16)

| Component | Version | Notes |
|---|---|---|
| JDK | 25.0.2 (JetBrains Runtime, bundled with Android Studio) | Runs the Gradle daemon; Android/Kotlin bytecode still targets JVM 17 (see below) |
| Gradle | 9.7.0, via committed wrapper | `gradle-wrapper.jar`/`gradlew`/`gradlew.bat` are committed; never invoke a system-wide Gradle |
| Android Gradle Plugin | 9.2.0 | Kotlin support is built in as of AGP 9.0 — no `org.jetbrains.kotlin.android` plugin applied |
| Kotlin (compiler) | 2.3.20 | Pinned via the `org.jetbrains.kotlin.plugin.compose` plugin version, since the standalone Kotlin plugin isn't applied. Held one patch behind the newest Kotlin (2.3.21) so it exactly matches the latest available KSP release — see below |
| KSP | 2.3.11 | Room's annotation processor. KSP's versioning now tracks Kotlin directly; 2.3.11 targets Kotlin 2.3.20 exactly (confirmed via its own `gradle.properties`), which is why Kotlin is pinned to 2.3.20 rather than 2.3.21 |
| Room | 2.8.4 | Local persistence (Section 8's entities). Room 3.0 exists but is alpha-only as of Aug 2026 — not used |
| Jetpack Compose | BOM 2026.08.00 | Individual artifact versions resolved via the BOM |
| compileSdk | 37 (Android 17) | Matches what's installed on the SDK — avoids an extra platform download |
| targetSdk | 36 (Android 16) | Deliberately one behind compileSdk; Android 17/API 37 changes default behavior (e.g. orientation-lock handling) that hasn't been reviewed yet |
| minSdk | 29 (Android 10) | Per Phase 0 Section 17 decision |
| Build Tools | 36.0.0 | Pinned explicitly; matches what's already installed and AGP 9.2/9.3's own documented default for compileSdk 37 |

**Why JDK 25 + bytecode target 17 works:** AGP 9's built-in Kotlin support infers the Kotlin compiler's `jvmTarget` from `android.compileOptions.targetCompatibility` (set to Java 17) automatically. This produces JVM-17-compatible bytecode for D8/R8 to dex, without requiring a separate installed JDK 17 toolchain (`kotlin { jvmToolchain(17) }` would force Gradle to resolve/download an exact JDK 17, which isn't needed here and was deliberately avoided given limited disk space).

## Phase 2 data model (verified 2026-08-16)

`data/local/entity/` implements all 16 entities from `docs/phase-0-research.md` Section 8 exactly, `data/local/dao/` has one Room DAO per entity, and `OrluneDatabase.kt` wires them into a single `@Database(version = 1, exportSchema = true)`. Schema JSON exports to `app/schemas/` (committed — this is what future migrations diff against).

A few concrete decisions Section 8 left as "illustrative, not final":
- Dates are stored as `epochDay: Long` (`java.time.LocalDate.toEpochDay()`), not a `LocalDate` column — avoids a Room `TypeConverter` for something no code reads yet.
- `Rule.type` and `AppListEntry.listType` are plain `String` columns (documented valid values in KDoc), not enums — the rule engine that gives these values real meaning doesn't exist until Phase 4, so a Room enum `TypeConverter` today would be encoding a business rule this layer doesn't own.
- Section 8's separate `BlockRule`/`AllowRule` rows became one `AppListEntryEntity` table with a `listType` discriminator, since they were already documented with an identical shape.
- Nothing wired `OrluneDatabase` up in Phase 2 — that's now done in Phase 3 (`OrluneApplication`), the first real consumer.

## Phase 3 usage monitoring (verified 2026-08-16)

Pipeline: `UsageEventReader` (platform, wraps `UsageStatsManager`) → `SessionCalculator` (pure, `core/domain/usage`) → `UsageAggregator` (pure) → `UsageRepository` (`data/repository`) persists via the Phase 2 DAOs. A periodic `UsageAggregationWorker` (WorkManager, 15 min — the minimum periodic interval, and there's no blocking-latency requirement in this phase to justify tighter polling) drives it; the debug screen in `MainActivity` triggers it on demand.

Key decisions:
- `MOVE_TO_FOREGROUND`/`MOVE_TO_BACKGROUND` are used deliberately despite being deprecated in favor of `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED` — the replacement fires per-Activity, not per-app, which would fragment a single session into many spurious ones for any multi-activity app. See the KDoc on `UsageEventReader.queryEvents`.
- Idempotency/duplicate-processing safety comes from a watermark (`UserPreferenceEntity`, key `usage.lastProcessedEventTime`), advanced to `lastEventTimestamp + 1` after each run — not from deduplicating rows after the fact.
- Sessions crossing local midnight are split at the boundary by `SessionCalculator` before ever reaching the database, so `DailyUsageEntity` aggregation is a plain per-day sum — no session ever spans two days.
- `DEVICE_SHUTDOWN` events close any still-open session at that timestamp, since there's no guaranteed `MOVE_TO_BACKGROUND` when the OS shuts down — handles the reboot case without needing a `BOOT_COMPLETED` receiver of our own (WorkManager reschedules its own periodic work across reboots automatically).
- `UsageEventSource`/`AppLabelSource` interfaces exist specifically so `UsageRepositoryInstrumentedTest` can run the real Room database against fake platform data — real usage events aren't controllable/deterministic on a test device.
- `OrluneApplication` implements `Configuration.Provider` to inject `UsageRepository` into `UsageAggregationWorker` via a custom `WorkerFactory` (manual DI, per Phase 0 Section 2). This requires removing WorkManager's default auto-initializer from the manifest — the meta-data key to remove is `androidx.work.WorkManagerInitializer`, not the `androidx.work.impl.WorkManagerInitializer` name older tutorials use; using the wrong name is a silent no-op (confirmed by rebuilding and grepping the merged manifest), not a build failure, so it's easy to ship broken.

## Privacy architecture (enforced, not just documented)

- No `INTERNET` permission exists anywhere in the manifest (source or merged build output) — verified by inspecting `app/build/intermediates/merged_manifest/**/AndroidManifest.xml` after every build that touches the manifest. Two real permissions now exist, both required for Phase 3's actual feature: `PACKAGE_USAGE_STATS` (Usage Access — functionally inert on its own; the real gate is `UsageAccessPermission`'s `AppOpsManager` check, granted by the user manually in Settings) and a scoped `<queries>` `CATEGORY_LAUNCHER` filter (not `uses-permission`, and not `QUERY_ALL_PACKAGES`) for app-label lookups.
- WorkManager's own library manifest transitively adds `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, and `ACCESS_NETWORK_STATE` — none of these were added deliberately, none are used by anything in this codebase (no `setForegroundAsync()`, no `ConnectivityManager` call), and none enable networking. `ACCESS_NETWORK_STATE` specifically only permits *querying* connectivity state; `INTERNET` is the permission that would actually allow making a request, and it's still absent.
- `android:allowBackup="false"` and `xml/data_extraction_rules.xml` (empty `<cloud-backup/>` and `<device-transfer/>` rules) block both cloud backup and device-to-device transfer of app data.
- No networking, analytics, or ad dependency exists in `app/build.gradle.kts` — see `docs/dependency-audit.md`.

## Gradle environment notes (machine-specific, not project policy)

- `GRADLE_USER_HOME` is set to `F:\GradleUserHome` (user-level environment variable) so Gradle's distribution and dependency caches land on the drive with free space, not the nearly-full `C:`.
- The Android SDK lives at `F:\Android\Sdk` (see `local.properties`, which is gitignored — machine-specific).
- Android Studio is installed at `F:\Android Stu`.
