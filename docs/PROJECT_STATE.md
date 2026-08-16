# Orlune — Project State

**Last verification date:** 2026-08-16 (full engineering audit — repository, build,
tests, security/privacy, docs reconciliation)

**Latest verified commit:** `ce637d4` — "feat: add initial Orlune product shell" (`main`; local branch remains ahead of `origin/main` until push).

This file is the living status snapshot. `AGENTS.MD` is the stable rules/conventions
file — read that first for *how* to work on this repo, this file for *where things
currently stand*. Update this file (not `AGENTS.MD`) after any verification pass or
significant change; keep `AGENTS.MD` stable unless a rule itself changes.

---

## Current phase

**Phase 6 (Focus Sessions) and a Room v1→v2 migration are implemented and tested.**
The initial Compose product shell and local export/delete controls are now present.
Analytics/recommendation algorithms remain deferred.

## Build status — VERIFIED

Ran on 2026-08-16 in this environment (`JAVA_HOME=F:\Android Stu\jbr`,
`GRADLE_USER_HOME=F:\GradleUserHome`):

- `.\gradlew.bat assembleDebug --stacktrace` → **BUILD SUCCESSFUL**
- `.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**, 92/92 unit tests
  pass, 0 failures, 0 errors (first invocation that session hit a transient Gradle
  test-event-reporter filesystem error unrelated to test content — see `AGENTS.MD`
  test-commands section — retry succeeded cleanly)
- `.\gradlew.bat connectedDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**,
  12/12 instrumentation tests pass, 0 failures, run against a real physical device
  (Pixel 7a, Android 17 / API 37, serial `32201JEHN04765`)

## Test status — VERIFIED (real counts, not claimed)

| Suite | Class | Tests | Result |
|---|---|---|---|
| Unit | `FocusSessionEngineTest` | 13 | pass |
| Unit | `BlockingEngineTest` | 8 (was 7 — 1 added this audit) | pass |
| Unit | `GoalEngineTest` | 6 | pass |
| Unit | `LimitEngineTest` | 6 | pass |
| Unit | `ScheduleEngineTest` | 14 | pass |
| Unit | `SessionCalculatorTest` | 11 | pass |
| Unit | `UsageAggregatorTest` | 4 | pass |
| Unit | `BlockingRepositoryTest` | 22 | pass |
| Unit | `FocusSessionRepositoryTest` | 6 | pass |
| **Unit total** | | **92** | **92/92 pass** |
| Instrumentation | `OrluneDatabaseMigrationTest` | 1 | pass (real device) |
| Instrumentation | `BlockingRepositoryInstrumentedTest` | 8 | pass (real device) |
| Instrumentation | `UsageRepositoryInstrumentedTest` | 3 | pass (real device) |
| **Instrumentation total** | | **12** | **12/12 pass** |
| **Grand total** | | **102** | **102/102 pass** |

## Physical-device status — VERIFIED

Pixel 7a connected over USB, `adb devices` lists it, `connectedDebugAndroidTest` ran
and passed against it (real SQLite, real Room migration, not an emulator/Robolectric
double). Manual on-device exercise of the full UI flow (permissions, blocking,
focus sessions) was **not** re-performed in this audit — TODO.md's Phase 3/5 manual
device-exercise claims from 2026-08-16 predate this audit and were not re-verified
end-to-end interactively; only the automated instrumentation suite was re-run.

## Implemented features

- Usage monitoring pipeline: `UsageEventReader` → `SessionCalculator` →
  `UsageAggregator` → `UsageRepository` → Room, driven by a 15-minute WorkManager
  periodic worker plus on-demand refresh from the debug UI.
- Deterministic rule engines: `LimitEngine`, `ScheduleEngine`, `BlockingEngine`,
  `GoalEngine` — pure Kotlin, unit-tested.
- App blocking: `BlockingMonitorService` (3s foreground-service poll) →
  `BlockingRepository.evaluate()` → `BlockOverlayController` (`SYSTEM_ALERT_WINDOW`
  overlay). Essential-app allow-list, daily-limit rules, and schedule rules all wired
  and tested.
- Focus sessions (Phase 6): `FocusSessionEngine` (pure, state derived from
  timestamps — no stored status column) + `FocusSessionRepository`
  (start/reconcile/cancel), integrated into `BlockingRepository.evaluate()` as an
  additional OR'd trigger alongside rules. Supports immediate and one-time-delayed
  ("start in N minutes") sessions. **Recurring focus scheduling does not exist** —
  don't add it without reviewing this design with the user first.
- Room v1→v2 migration (`OrluneMigrations.MIGRATION_1_2`): adds `schedules.name` and
  `focus_sessions.blockedPackages`, both `NOT NULL DEFAULT ''`, verified
  non-destructive by `OrluneDatabaseMigrationTest` against all 16 tables' real data,
  and re-verified this audit on a real device.
- Privacy architecture: no `INTERNET` permission anywhere, `allowBackup=false`, empty
  cloud-backup/device-transfer extraction rules, no analytics/ads/AI dependency —
  all re-verified this audit (manifest, `libs.versions.toml`, `build.gradle.kts`,
  full-repo secret/credential grep — all clean).

## Unfinished / not started

Onboarding is not yet a dedicated first-run flow. The product shell now includes Home,
Focus, Limits, Insights, Settings, permission status, local JSON export, and delete-all
controls; a fuller Privacy Center and broader test/release hardening remain. Analytics/
recommendation algorithms, AccessibilityService, and website/VPN blocking remain
deliberately deferred.

## Bugs found and fixed this audit

1. **`BlockingEngine.decide()` — allow/block precedence violated by list order**
   (`core/domain/rules/BlockingEngine.kt`). `AppListEntryEntity`'s primary key is
   `(packageName, listType)`, so a package can legally have both a "block" row and
   an "allow" row simultaneously. The old code picked `appListEntries.firstOrNull { it.packageName == packageName }`
   against a list built as `blockEntries + allowEntries` (`BlockingRepository.evaluate()`),
   so when both existed for the same package, the block entry — always first in the
   concatenation — silently won, contradicting the documented and tested precedence
   ("an explicit ALLOW list entry always wins... must override any triggered rule").
   Not reachable through the current debug UI (which only ever writes "allow" rows),
   but live in the DAO/entity layer and would silently break the essential-app safety
   guarantee once a real UI, import, or future feature could write both rows for the
   same package. **Fixed**: precedence is now decided by the *set* of list types
   present for the package, not list order — ALLOW checked first regardless of
   position. Added a regression test
   (`allow entry wins even when a block entry exists for the same package`).
2. **`OrluneDatabase.kt` KDoc — stale, actively misleading migration claim**. The
   class doc said version 2 uses `fallbackToDestructiveMigration()` "since... no
   migration path has ever existed" — written before commit `96b93d4` added the real,
   tested, non-destructive `MIGRATION_1_2` and wired it into `OrluneApplication`. The
   actual code was correct; only the comment was wrong, but a future agent trusting
   this comment over the code could easily reintroduce a destructive migration
   believing there'd been no working alternative. **Fixed**: KDoc now describes the
   real migration and points to `OrluneMigrations.kt` and `OrluneDatabaseMigrationTest`.

Neither bug affected currently-shipped behavior in a way real usage could trigger
today (see reachability notes above), but both were real, silent violations of this
project's own documented safety contracts, exactly the class of defect this audit
was commissioned to find.

## Known risks (not yet fixed — deliberately left for a future task)

See `AGENTS.MD`'s "Known risks" section — kept there since it's read alongside the
rules that explain why each one matters. Summary: overlapping/concurrent focus
sessions aren't explicitly prevented (a product decision, not obviously a bug);
recurring focus scheduling remains intentionally out of scope; manual UI/device
exercise must be repeated after the shell replacement; minor
`docs/dependency-audit.md` test-dependency table gap.

## Next recommended task

Pick one, don't start both without checking in:

1. Repeat the Compose shell flow and export/delete flow on the physical Pixel 7a,
   then perform a release-hardening review.
2. Decide with the user whether to add a dedicated onboarding/Privacy Center flow or
   move to deferred analytics/recommendation work. Do not add recurring focus
   scheduling without a separate product decision.

Do not start either without user sign-off — this audit's mandate was to stabilize
and document, not to open new feature work.
