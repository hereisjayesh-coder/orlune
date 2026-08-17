# Orlune — Agent Instructions

This file is the permanent, standing brief for any coding agent (Claude Code, Codex,
GitHub Copilot, a human in Android Studio, or anything else) picking up this repo.
It does not depend on any prior conversation. If something here conflicts with the
actual code, the code wins — treat that as a doc bug and fix this file, per the
"documentation reconciliation" rule below.

For the current, dated snapshot (build/test status, known bugs, next task), read
**`docs/PROJECT_STATE.md`** first. This file is the stable rules; that one is the
living status.

## Product identity

**Orlune** — a free, local-only, no-account Android digital wellbeing app. Helps
people reduce distracting app usage, build focus habits, and understand their own
screen time, entirely on-device.

- Android only. No iOS, no web, no desktop.
- Original product: shares no code, design, branding, or algorithms with Opal,
  Digital Wellbeing, ScreenZen, one sec, Freedom, or any other competitor.
- Black-first, minimal, serious, calm, functional UI. No excessive decoration, no
  unnecessary animation, no fake-AI aesthetics, no shame-based messaging, no
  subscription pressure, no login screen (there is no login).

## Forbidden — permanent constraints, not phase-scoped

These are architectural, not preferences. Do not add any of the following without
an explicit, deliberate user decision to change the product's premise:

- No account, no login, no signup, no cloud, no backend, no server.
- No AI, no machine learning, no "smart" recommendation model.
- No ads, no analytics SDK, no tracking SDK, no crash-reporting SDK (Firebase,
  Crashlytics, Mixpanel, etc.).
- No `INTERNET` permission, no networking dependency (Retrofit/OkHttp/Ktor/Volley),
  no outbound network request of any kind. Verify this after every manifest or
  dependency change: check `app/src/main/AndroidManifest.xml` and, if in doubt, the
  merged manifest at `app/build/intermediates/merged_manifest/**/AndroidManifest.xml`
  after a build.
- No screenshots, no screen recording, no microphone, no camera, no keylogging, no
  unnecessary access to private content.
- No `QUERY_ALL_PACKAGES` — use scoped `<queries>` (`CATEGORY_LAUNCHER`) instead.
- AccessibilityService stays **optional and never becomes an automatic dependency**.
  It does not exist in the codebase today; if it's ever added, it must be an
  explicitly-disclosed opt-in upgrade path, never the default detection mechanism.
  See `docs/accessibility-service-compliance.md`.
- VPN / website blocking is deferred indefinitely unless explicitly approved by the
  user — do not start building `VpnService` code speculatively.
- Every algorithm is deterministic. No heuristic/statistical/ML scoring — same
  input, same output, always. See `core/domain/rules/` and `core/domain/focus/` for
  the established style (pure Kotlin, no Android dependency, fully unit-tested).

## Architecture

Single Gradle module (`:app`), package-layered under `com.orlune.app` — not
multi-module. See `ARCHITECTURE.md` for the full as-built layout and the verified
toolchain table (versions change; always trust that file, and `gradle/libs.versions.toml`,
over memory of any past conversation).

```
platform/   Android-framework-touching code (UsageStatsManager, WindowManager, Services)
data/       Room database (local/) and repositories (repository/) — the only layer
            that talks to both platform/ and core/
core/       Pure Kotlin business logic — domain/usage, domain/rules, domain/focus —
            no Android framework import, fully unit-testable on the JVM
feature/    Compose UI screens, one package per screen (home/focus/limits/insights/
            settings/privacy/apppicker/onboarding all implemented). Screens with their
            own sub-navigation (app picker, Privacy/Legal Center, the 11-screen
            onboarding flow) are wrapped in a "Section" composable (e.g. LimitsSection,
            FocusSection, SettingsSection, OnboardingSection) that owns a manual
            back-stack + rememberSaveableStateHolder — see any of those for the
            established pattern before adding a new one. `OnboardingSection` is the
            largest example: an 11-step linear flow plus two side-trips (the app
            picker, the Legal Center) on one shared back-stack, all selections in
            Bundle-safe `rememberSaveable` state, committed to Room once and
            atomically at "Finish" — never incrementally per screen (see
            `docs/PROJECT_STATE.md` for why).
platform/usage/InstalledAppLister.kt enumerates launchable apps for the app picker via
            the existing `<queries>`/CATEGORY_LAUNCHER manifest declaration — never add
            QUERY_ALL_PACKAGES; see docs/app-visibility-compliance.md for why
ui/         Shared Compose theme (ui/theme/), reusable components (ui/components/),
            and the OrluneTab enum + bottom-nav wiring (ui/navigation/); OrluneRoot.kt
            is the thin composition root that owns state and delegates to feature/
```

Pipeline shape (usage monitoring): `UsageEventReader` (platform) → `SessionCalculator`
(pure) → `UsageAggregator` (pure) → `UsageRepository` (data) → Room.

Pipeline shape (blocking): `BlockingMonitorService` (foreground service, 3s poll) →
`UsageRepository.processNewEvents()` + `FocusSessionRepository.reconcileActiveSessions()`
→ `BlockingRepository.evaluate()` (reads `SessionDao.getOpenSessions()` for "current
foreground app", dispatches to `LimitEngine`/`ScheduleEngine`/`FocusSessionEngine`,
reduces through `BlockingEngine.decide()`) → `BlockOverlayController` draws/hides a
`SYSTEM_ALERT_WINDOW` overlay on `Dispatchers.Main`. The same tick also reconciles
Focus's notification policy (Phase 8): `effectiveFocusNotificationState()` (pure,
`core/domain/focus/`) resolves the most-restrictive policy across every currently
`ACTIVE` session, and `platform/notifications/FocusZenRuleController.kt` makes the
single system-owned `AutomaticZenRule` it manages match — never Android's older,
whole-device `setInterruptionFilter()`/`setNotificationPolicy()` APIs. See
`docs/android-notification-policy.md` for the full design and two real bugs that were
only found by testing on real hardware (the tick's "should I keep running" check has
to run notification-policy reconciliation *before* it can decide to `stopSelf()`, not
after — and a stray active rule needs a reconciliation pass at every process cold
start, independent of whether `BlockingMonitorService` itself has a reason to run).

Manual dependency injection throughout (constructor injection, `by lazy` in
`OrluneApplication`) — no DI framework. Don't introduce Hilt/Koin/Dagger unless the
object graph genuinely outgrows this; it hasn't yet.

## Privacy requirements (enforced, not just documented)

- `android:allowBackup="false"` + `xml/data_extraction_rules.xml` (empty
  `<cloud-backup/>`/`<device-transfer/>`) block cloud backup and device transfer.
- Nothing in `app/build.gradle.kts` or `gradle/libs.versions.toml` may add a
  networking, analytics, ads, or AI dependency. `docs/dependency-audit.md` records
  every dependency's purpose/license/network/telemetry — update it when you add one.
- Every sensitive permission (`PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`) is a
  manual Settings grant, never auto-granted, and every enforcement path fails safe
  (ALLOW, not BLOCK, and no crash) if a permission is revoked mid-run.
- Never commit secrets, signing keys, `.jks`/`.keystore` files, `local.properties`,
  or real device usage exports. `.gitignore` already covers these plus
  `/.devcheck/` (ad-hoc device verification dumps) — keep that pattern if you add
  new local-only scratch directories.

## Current phase and completed phases

**Read `docs/PROJECT_STATE.md` for the authoritative, dated answer.** As of the last
audit (see that file's date), status is approximately:

| Phase | Focus | Status |
|---|---|---|
| 0 | Research & feasibility | Complete |
| 1 | Android project setup | Complete, build-verified |
| 2 | Room database (16 entities) | Complete |
| 3 | Usage monitoring | Complete, unit + real-device instrumentation verified |
| 4 | Deterministic rule engine | Complete, unit-tested |
| 5 | App blocking | Complete, real-device verified |
| 6 | Focus sessions & Room v1→v2 migration | Implemented; recurring focus scheduling intentionally deferred |
| 7+ | Analytics/algorithms, full Privacy Center, security/perf, testing, Play compliance, release | Partially started; first-launch onboarding is done — see `docs/PROJECT_STATE.md` |

**Important history note:** commits `96f1d51` and `96b93d4` added a working Phase 6
(`FocusSessionEngine`/`FocusSessionRepository`, `blockedPackages` wired into
`BlockingRepository`) and a Room v1→v2 migration (`OrluneMigrations.MIGRATION_1_2`),
but `TODO.md`/`ROADMAP.md` were never updated to reflect this — they still read as if
Phase 5 was the last work done. Treat `TODO.md`/`ROADMAP.md`'s Phase 6+ rows as stale
until you've re-read the actual code; `git log` and the source tree are the source of
truth, not those files. (This audit brought them back in sync — see
`docs/PROJECT_STATE.md` for the reconciled status, and re-check for the same kind of
drift after any future work that isn't immediately followed by a doc update.)

## Deferred features (explicitly not started, by design)

Website/VPN blocking, AccessibilityService-based detection, Friction/Strict Focus
blocking levels (no config field exists for them yet), analytics, recommendation/
wellbeing-score algorithms (`StatisticsEngine`/`ConsistencyEngine`/
`RecommendationEngine`/`DigitalBalanceEngine` — planned for Phase 7), monetization,
accounts, cloud sync, AI/ML, recurring focus-session scheduling (only one-time
scheduled/immediate focus sessions exist today — do not add recurrence without
first reviewing the current Phase 6 design with the user).

## Build commands

From `D:\App` in PowerShell, with both env vars set inline (the persistent user env
vars don't propagate to already-running shells):

```powershell
$env:JAVA_HOME = "F:\Android Stu\jbr"
$env:GRADLE_USER_HOME = "F:\GradleUserHome"
.\gradlew.bat assembleDebug --stacktrace
```

Never invoke a system-wide Gradle — always the committed wrapper (`gradlew.bat`).

## Test commands

```powershell
.\gradlew.bat testDebugUnitTest --stacktrace        # JVM unit tests (core/, most of data/repository)
.\gradlew.bat connectedDebugAndroidTest --stacktrace # real-device instrumentation tests — requires `adb devices` to list a device
```

Check `adb devices` (full path `F:\Android\Sdk\platform-tools\adb.exe` if not on
`PATH`) before attempting `connectedDebugAndroidTest`. If no device is listed, report
test status as **NOT VERIFIED IN THIS ENVIRONMENT**, do not guess or claim it passed.

Occasionally `testDebugUnitTest` fails with `NoSuchFileException` on
`in-progress-results-generic.bin` on the very first invocation of a session — this is
a transient Gradle test-event-reporter/filesystem hiccup on this machine, not a real
test failure. Re-run once before treating it as a real regression.

**Never report a build or test as passing without having actually run it to
completion and read the real output.** Never guess at version numbers or
compatibility — verify against actually-released versions before writing them into
config. (See `docs/PROJECT_STATE.md` for why this rule exists on this project.)

## Android setup (this machine)

- Android Studio: `F:\Android Stu`. SDK: `F:\Android\Sdk` (via `local.properties`,
  gitignored). Gradle caches: `F:\GradleUserHome` (`GRADLE_USER_HOME`) — the `C:`
  drive is nearly full, never let caches land there.
- No emulator (8GB RAM machine) — physical device over USB (`adb`) is the only test
  strategy. `adb install -r` does not reliably preserve app-ops/runtime permission
  grants across reinstalls on this device — re-grant and re-verify after every
  reinstall:
  ```
  adb shell appops set com.orlune.app android:get_usage_stats allow
  adb shell pm grant com.orlune.app android.permission.POST_NOTIFICATIONS
  ```
  (`SYSTEM_ALERT_WINDOW` still requires the manual Settings toggle — no `appops`/`pm
  grant` shortcut exists for it.)
- Pull binary files (e.g. a Room `.db`) with `adb exec-out`, not `adb shell ... >
  file` — the latter does CRLF translation on Windows and corrupts binary data. Pull
  `-wal`/`-shm` sidecar files alongside the main `.db` if using WAL mode.

## Coding conventions

- Manual constructor injection, no DI framework.
- Pure Kotlin for anything in `core/domain/` — no Android framework import, so it
  stays plain-JVM-unit-testable. Platform-touching code stays in `platform/` behind
  a small interface (`UsageEventSource`, `AppLabelSource`) so `data/repository/` can
  be tested against fakes, not mocks — **no mocking library is used anywhere in this
  project**; hand-rolled fakes are the established pattern.
- Fail-safe, not fail-crash: malformed/incomplete rule data is treated as
  "not triggered," never "block everything." A missing permission stops enforcement
  cleanly (ALLOW, no crash), never blocks incorrectly. Follow this pattern for any
  new rule/decision logic.
- No comments explaining *what* code does. KDoc is used narrowly, for the *why* —
  a non-obvious constraint, a documented precedence rule, a bug that was fixed and
  why the fix looks the way it does. Match the existing terse-but-load-bearing KDoc
  style already in `core/domain/rules/` and `core/domain/focus/`.
- Don't rewrite working code for style. Preserve existing phases' working behavior;
  fix actual defects, don't restructure around them.
- **Any top-level content rendered outside the tab `Scaffold`'s `Surface(color =
  MaterialTheme.colorScheme.background)` must wrap itself in an equivalent `Surface`
  (or otherwise establish `LocalContentColor`).** Real bug, found only by running the
  onboarding flow on-device (first-launch onboarding milestone): `Text` with no
  explicit `color` renders using Compose's default `LocalContentColor`, which is
  **not** automatically set to something visible by `MaterialTheme` alone — only a
  `Surface` (or an explicit override) does that. `OnboardingSection` was originally
  composed directly under `OrluneRoot`, outside any `Surface`, and every un-colored
  title `Text` (the entire `OnboardingScaffold` title, the whole Welcome headline)
  rendered fully present in the view tree but fully invisible — black text on this
  app's black background. Fixed by wrapping the onboarding branch in
  `Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)`,
  same as the tab content already had. Any new top-level branch in `OrluneRoot.kt`
  (a future gate/wizard/full-screen state) needs the same wrapping — it will not be
  visually obvious in isolation, only against this app's specifically black-on-black
  palette, and it will not fail any build or unit test.

## Git workflow

- Meaningful, scoped commits (`fix: preserve local data across Room v1 to v2
  migration`, `test: verify blocking on physical device`, `docs: reconcile project
  state`) — not giant mixed commits.
- Never commit secrets, signing keys, `.jks`/`.keystore`, local databases, or
  temporary device exports (`.gitignore` already covers the known patterns).
- Before any destructive git operation, run `git status` and stash/commit anything
  uncommitted first.

## Migration rules

- **Never use `fallbackToDestructiveMigration()`.** Every schema version bump needs
  an explicit `Migration` object in `OrluneMigrations.kt`, wired into
  `OrluneApplication`'s `Room.databaseBuilder(...).addMigrations(...)`, plus a test
  in `OrluneDatabaseMigrationTest` that inserts realistic prior-version data via a
  reconstructed prior-version schema (see the `Version1Callback` pattern already
  there), runs the real migration, and asserts every table's row count *and* actual
  column values survived — not just that the schema shape matches.
- Bump `OrluneDatabase`'s `version` and update its class KDoc to describe exactly
  what changed and why, in the same commit as the entity change and the migration.
  A stale KDoc here is exactly the kind of drift this audit found and fixed once
  already (see `docs/PROJECT_STATE.md`) — don't reintroduce it.
- Export schemas are committed under `app/schemas/` — never delete or hand-edit a
  past version's exported JSON; it's what future migrations diff against.

## Release rules

No release has happened yet (`versionCode = 1`, `versionName = "0.1.0"`). Before any
real release: re-verify every item in "Privacy requirements" above, work through
Play's Data Safety / permissions declarations for `PACKAGE_USAGE_STATS` and
`SYSTEM_ALERT_WINDOW`, and confirm `isMinifyEnabled`/ProGuard rules are deliberately
chosen, not left at whatever a template produced.

## Known platform limitations (do not promise otherwise in UI copy or docs)

See `docs/android-platform-capabilities.md` for the full matrix and sourcing. In
short: blocking detection latency is a few seconds, not instant (no
AccessibilityService); a brief flash of a blocked app before the overlay appears is
possible; Android 12+ apps can opt out of overlays via `HIDE_OVERLAY_WINDOWS`; OEM
background-kill (MIUI/One UI/etc.) can force-stop the monitoring service regardless
of standard Doze exemptions; there is no way to prevent a determined user from
disabling permissions or uninstalling the app. Never describe blocking as
"unbreakable" or monitoring as "guaranteed always-on." Same enforcement-latency
budget (a few seconds, not instant) applies to Focus's notification-policy
engagement/disengagement (Phase 8) — see `docs/android-notification-policy.md`. Never
claim Orlune guarantees emergency calls reach the user on every device: call handling
during a Focus session ultimately depends on Android's own Do Not Disturb behavior and
the device manufacturer's implementation of it, which is known to vary; Orlune
configures the policy, nothing more.

## Known risks (as of the last audit — see `docs/PROJECT_STATE.md` for date)

- The initial real Compose shell is now the launch surface; the old `DebugScreen`
  reference code has been removed from `MainActivity.kt` (it's now just the
  `ComponentActivity` that sets `OrluneTheme { OrluneRoot(app) }`). Dedicated
  onboarding and Privacy Center screens still remain.
- `FocusSessionRepository.startSession` validates duration and package inputs. The
  engine still assumes persisted rows are structurally valid, so malformed imported
  data should continue to be treated conservatively.
- Overlapping/concurrent focus sessions are not explicitly prevented — the engine
  correctly unions blocked packages across simultaneously-active sessions, but no
  UI/repository guard stops a user (or a caller) from starting two at once. **Resolved
  for notification policy in Phase 8**: `effectiveFocusNotificationState()` explicitly
  decides overlapping sessions by picking the single most restrictive active policy
  (see `docs/android-notification-policy.md`) — the same question for blocking-overlay
  behavior (which packages actually get blocked when two sessions disagree) remains
  open and undecided.
- `docs/dependency-audit.md`'s test-dependency table is missing `androidx.test:runner`
  and `androidx.test:core`/`kotlinx-coroutines-test` for `androidTest`, even though
  they're in `app/build.gradle.kts` — minor doc gap, not a privacy/security issue.
- Focus's notification-policy API-29 fallback path (`FocusZenPolicyMapper`'s legacy
  `INTERRUPTION_FILTER_*` branch) is unverified on real API 29 hardware — this
  project's only test device is a Pixel 7a on API 37. Two real bugs in the API 30+
  path were only found by testing on real hardware (see
  `docs/android-notification-policy.md`); the legacy branch could have similar
  surprises an API-29 device would need to surface.

## How to resume work

1. Read `docs/PROJECT_STATE.md` for the dated snapshot.
2. Run `git log --oneline -20` and `git status` — don't trust any prior
   conversation's memory of what's done.
3. Run the build and test commands above and read the real output before believing
   anything is green.
4. Re-check this file's "Known risks" section against the current code — if a risk
   was fixed, move it to `docs/PROJECT_STATE.md`'s history and remove it from here;
   if a new one was found, add it here.
5. Do not start a new phase's feature work without first confirming the previous
   phase's exit criteria (`ROADMAP.md`) actually hold against the current code, not
   just against what `ROADMAP.md` claims.
