# Orlune — Project State

**Last verification date:** 2026-08-17 (raw package names eliminated from all normal
UI via a live label/icon presentation layer; recurring-schedule day/time entry
replaced with a day selector and Material time-picker; daily-limit custom duration
replaced with a tap stepper; Insights extended with longest session / focus stats /
limit compliance — all derived from existing local data, no AI, no fabricated stats)

**Latest verified commit:** working tree as of this file's date, on top of `58b998f`
("feat: add initial Orlune product shell") — this session's changes are **not yet
committed** (uncommitted working-tree changes only, not pushed) pending explicit
instruction to commit; verify with `git log`/`git status` before trusting this file's
claims if picking this up later; this file is a snapshot, not a substitute for
checking the real repository state.

This file is the living status snapshot. `AGENTS.MD` is the stable rules/conventions
file — read that first for *how* to work on this repo, this file for *where things
currently stand*. Update this file (not `AGENTS.MD`) after any verification pass or
significant change; keep `AGENTS.MD` stable unless a rule itself changes.

---

## Current phase

**Phase 6 remains implemented and tested. Phase 8 (UI) advanced significantly this
session**, focused entirely on eliminating raw package-name exposure and manual
text-entry from normal UI — see `ROADMAP.md` for the phase table. Five isolated
changes, each built/tested/device-verified before starting the next:

1. **No more raw package names anywhere in normal UI.** Home, Insights, and Limits'
   "Active rules" now resolve every package to a live label + icon via a new
   presentation layer (`platform/usage/AppDisplayResolver.kt`), rather than trusting
   the `apps` table's possibly-stale stored label. The device's current default
   launcher (previously shown as `com.google.android.apps.nexuslauncher`) now reads
   "Home screen" — fixed at the root cause: the `<queries>` manifest block only
   declared `CATEGORY_LAUNCHER`, not `CATEGORY_HOME`, so the launcher's own package
   was never visible to `PackageManager`. Orlune's own package is excluded from Home
   and Insights usage lists (UI-layer filtering only — no `SessionEntity`/
   `DailyUsageEntity` rows or repository/DAO behavior changed; underlying usage data
   is untouched, matching the "presentation, not deletion" rule).
2. **Recurring schedule input**: the "MON,TUE,WED,THU,FRI" free-text field and two
   raw HH:mm text fields are gone, replaced by `ui/components/WeekdaySelector.kt`
   (individual Mon–Sun toggles + Every day/Weekdays/Weekends/Custom presets) and
   `ui/components/TimePickerField.kt` (a Material3 `TimePicker` dialog). Both still
   produce exactly the same wire format `ScheduleEntity`/`ScheduleEngine` already
   expect (comma-separated day codes, "HH:mm" strings) — the domain model and engine
   are unchanged.
3. **Daily-limit custom duration**: the two raw numeric Hours/Minutes text fields are
   gone, replaced by `ui/components/DurationStepper.kt` (tap +/- steppers, hours
   0–24, minutes 0–55 in 5-minute steps) — structurally can't produce invalid text,
   out-of-range values, or an empty field. `DailyLimitInput.toThresholdSeconds`
   validation is unchanged.
4. Home screen presentation requirements (branding, real icons/names, no raw package
   names, "Start Focus" as primary action) were already satisfied as a direct result
   of item 1 — verified, not separately changed.
5. **Insights** gained a "Last 14 days at a glance" card: longest single usage
   session (with resolved app label/icon), focus session count, total focus time, and
   "daily limits met today" (hidden entirely when no limit rules exist, rather than
   showing a misleading 0-of-0) — all derived from existing local tables
   (`sessions`, `focus_sessions`, `rules` + today's `daily_usage`) via a new pure,
   unit-tested `core/domain/insights/InsightsMetrics.kt`. No AI, no invented
   statistics.

Onboarding remains the largest not-yet-started piece of Phase 8.

## Build status — VERIFIED

Ran on 2026-08-17 in this environment (`JAVA_HOME=F:\Android Stu\jbr`,
`GRADLE_USER_HOME=F:\GradleUserHome`), after each of the five changes above and again
at the end:

- `.\gradlew.bat assembleDebug --stacktrace` → **BUILD SUCCESSFUL**
- `.\gradlew.bat testDebugUnitTest --stacktrace` → **BUILD SUCCESSFUL**, **154/154**
  unit tests pass, 0 failures, 0 errors (summed from the real per-suite XML results
  in `app/build/test-results/`)
- `.\gradlew.bat connectedDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**,
  **28/28** instrumentation tests pass, run against a real physical device (Pixel 7a,
  Android 17 / API 37, serial `32201JEHN04765`)

## Test status — VERIFIED (real counts, not claimed)

| Suite | Class | Tests | Result |
|---|---|---|---|
| Unit | `FocusSessionEngineTest` | 13 | pass |
| Unit | `BlockingEngineTest` | 8 | pass |
| Unit | `LegalMarkdownTest` | 13 | pass |
| Unit | `GoalEngineTest` | 6 | pass |
| Unit | `LimitEngineTest` | 6 | pass |
| Unit | `ScheduleEngineTest` | 14 | pass |
| Unit | `SessionCalculatorTest` | 11 | pass |
| Unit | `UsageAggregatorTest` | 4 | pass |
| Unit | `BlockingRepositoryTest` | 22 | pass |
| Unit | `FocusSessionRepositoryTest` | 8 | pass |
| Unit | `DailyLimitInputTest` | 7 | pass |
| Unit | `LegalDocumentsTest` | 7 | pass |
| Unit | `AppDisplayResolverTest` (new) | 8 | pass |
| Unit | `ScheduleInputTest` (new) | 9 | pass |
| Unit | `DurationStepperTest` (new) | 9 | pass |
| Unit | `InsightsMetricsTest` (new) | 10 | pass |
| **Unit total** | | **154** | **154/154 pass** |
| Instrumentation | `OrluneDatabaseMigrationTest` | 1 | pass (real device) |
| Instrumentation | `BlockingRepositoryInstrumentedTest` | 8 | pass (real device) |
| Instrumentation | `UsageRepositoryInstrumentedTest` | 7 | pass (real device) |
| Instrumentation | `ThemePreferenceDaoInstrumentedTest` | 4 | pass (real device) |
| Instrumentation | `InstalledAppListerInstrumentedTest` | 8 (+3 new) | pass (real device) |
| **Instrumentation total** | | **28** | **28/28 pass** |
| **Grand total** | | **182** | **182/182 pass** |

## Physical-device status — VERIFIED (manual walkthrough, this session)

Pixel 7a connected over USB, via `adb`/`uiautomator` (text-dump verification of real
rendered UI content, not just build success). Usage Access was temporarily granted
for the walkthrough and reset to its prior (default/ungranted) state afterward; every
test rule created during verification was removed afterward — device left as found.

- **Home**: "Most used today" showed real resolved labels ("Claude", "Drive",
  "WhatsApp", "YouTube", **"Home screen"**) with icons, no raw package names, Orlune
  itself absent from the list.
- **Insights**: same resolved labels in "Apps in the last 14 days"; new "Last 14 days
  at a glance" card confirmed showing "31m · Home screen" for longest session, real
  focus session/time counts, and "0 of 1" for a deliberately-under-threshold test
  limit rule (compliance line correctly absent when zero limit rules exist).
- **Limits → Active rules**: a test limit and a test schedule rule both displayed
  with real icon + label ("WhatsApp", "3h 30m"; "WhatsApp", "Scheduled restriction")
  instead of a package name.
- **Recurring schedule**: day chips and Every day/Weekdays/Weekends presets toggle
  correctly; tapping Start/End opened a real Material `TimePicker` dialog pre-filled
  with the current value; a schedule created with the "Every day" preset survived a
  full `am force-stop` + relaunch.
- **Daily limit custom duration**: +/- steppers for Hours/Minutes produced a live
  "= 3h 30m" preview (matching the exact example from the task ask) and persisted
  correctly into a rule.
- Clean `logcat` throughout (`*:E AndroidRuntime:E`, filtered to `com.orlune.app`) at
  every verification step — no `FATAL EXCEPTION`, no crash.

No new crashes were found this session (the one known crash class — Bundle-unsafe
`SaveableStateProvider` keys — was already fixed in the prior session and remains
fixed; unaffected by this session's changes).

## Implemented features

- Usage monitoring, deterministic rule engines, app blocking, and focus sessions —
  unchanged this session; see prior snapshots / `TODO.md` for their history.
- **App picker** (`feature/apppicker/AppPickerScreen.kt`): real installed-app icons
  and labels via `platform/usage/InstalledAppLister.kt`, which enumerates launchable
  apps through the existing `<queries>`/`CATEGORY_LAUNCHER` manifest declaration —
  not `QUERY_ALL_PACKAGES` (see `docs/app-visibility-compliance.md` for the policy
  research behind that choice). The manifest now also declares `CATEGORY_HOME` (this
  session), narrowly scoped to make the device's current default launcher resolvable
  for label/icon purposes — still not `QUERY_ALL_PACKAGES`. Wired into Limits
  (single-select, matching `RuleEntity`'s one-package-per-rule design) and Focus
  (multi-select) via `LimitsSection`/`FocusSection` wrapper composables that own each
  feature's picker sub-navigation, matching the `SettingsSection` pattern.
- **Presentation layer** (this session): `platform/usage/AppDisplayResolver.kt` +
  `ui/components/AppDisplayInfoState.kt`/`AppUsageRow.kt`/`AppIcon.kt` — shared,
  tested label/icon resolution used by Home, Insights, and Limits' Active rules list,
  so a package name is never shown raw and a stale stored label can't linger in the
  UI even though the underlying `apps` table row isn't rewritten.
- **Launcher icon**: adaptive-icon foreground + monochrome layers at all 5 density
  buckets, extracted from `design/orlune-logo-reference.png` (the user-approved
  concept #4 reference) via flood-fill background removal, shipped as raster PNGs
  rather than hand-vectorized (to avoid drifting from the approved reference's
  gradients). `ic_launcher_background` is `#000000`, exactly matching the reference.
- **Privacy & Legal Center**: 15 documents (Privacy Policy expanded to 23 sections,
  Terms of Service to 28), all still development drafts with explicit `[TBD]`
  placeholders for unresolved business/legal facts — no company name, address, or
  contact was invented. Cross-references `docs/legal-compliance-matrix.md` and
  `docs/google-play-privacy-compliance.md`. "About Orlune" explicitly states no
  LICENSE file exists and Orlune is not (yet) open source — checked against the
  actual repository, not assumed.
- Privacy architecture: no `INTERNET` permission anywhere, no analytics/ads/AI
  dependency — re-verified this session via `git diff` showing the only manifest
  change is the narrowly-scoped `CATEGORY_HOME` `<queries>` entry described above; no
  changes to `app/build.gradle.kts` or `gradle/libs.versions.toml`.

## Unfinished / not started

Onboarding is still not a dedicated first-run flow. Recurring focus-session
scheduling remains out of scope pending a separate product decision. The Privacy &
Legal Center's documents are development drafts, not lawyer-reviewed or published —
see `docs/legal-compliance-matrix.md`'s "Legal review required" column for exactly
what's outstanding before any public claim of compliance. No privacy-policy URL is
hosted anywhere yet, which blocks Google Play submission independent of the document
content itself. Analytics/recommendation algorithms, AccessibilityService, and
website/VPN blocking remain deliberately deferred.

## Known risks (not yet fixed — deliberately left for a future task)

See `AGENTS.MD`'s "Known risks" section for the standing list (overlapping focus
sessions, recurring scheduling scope, minor dependency-audit doc gap). Additions from
this session:

- The app-picker's "Frequently used today" section and per-row usage subtext depend
  on `todayUsage`, which is only populated after Usage Access is granted and at least
  one refresh has run — on a completely fresh install with no usage data yet, the
  picker still works (falls back to the plain "All apps" list), but this hasn't been
  explicitly device-tested from a true first-run, zero-data state.
- Several jurisdictions' applicability to Orlune's local-only architecture is
  explicitly unresolved (see `docs/legal-compliance-matrix.md`) — do not represent
  Orlune as compliant with DPDP, GDPR, CCPA, or COPPA without legal sign-off first.
- Insights' new "Last 14 days at a glance" card's "Longest session" and "Focus
  sessions"/"Focus time" facts share the same fresh-install caveat as the app
  picker's "Frequently used today": correct by construction (verified with real data
  on the reference device), but not separately device-tested from a true zero-data
  first run.
- The `sessions` table has no pruning implemented despite its own KDoc describing
  "short retention by design" — every closed session is kept indefinitely in
  practice (confirmed by code inspection: `SessionDao.delete` exists but is never
  called). This session's new `observeLongestSessionBetween` query relies on that
  actual (not documented) retention behavior. Not a regression introduced this
  session, but worth flagging: either the KDoc or the retention behavior should
  eventually be corrected to match the other.

## Next recommended task

Pick one, don't start more than one without checking in:

1. **Notification-policy / Focus quiet-mode implementation** — explicitly deferred
   from this session at the user's request, to be picked up as its own isolated task.
2. Onboarding flow — the largest remaining piece of Phase 8, and the natural place to
   introduce the permission requests and app picker for the first time.
3. Legal review of the Privacy Policy / Terms of Service against
   `docs/legal-compliance-matrix.md`'s open issues, plus resolving the actual business
   details (legal entity, address, contact) currently held as placeholders.

Do not start any without user sign-off. This session's changes exist only as
uncommitted working-tree changes — not committed, not pushed; see the session's own
summary for the exact file list.
