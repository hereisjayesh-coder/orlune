# Orlune — Project State

**Last verification date:** 2026-08-17 (added a local, backend-free email Feedback
flow: Settings → Feedback hands off to the device's own email app via ACTION_SENDTO +
a mailto URI, addressed to the Orlune team with a suggested subject/body the user can
edit or discard — no backend, no INTERNET permission, no in-app collection, no
analytics)

**Latest verified commit:** working tree as of this file's date, on top of `30876da`
("feat: improve app selection schedules and usage insights") — this session's changes
are **not yet committed** (uncommitted working-tree changes only, not pushed) pending
explicit instruction to commit; verify with `git log`/`git status` before trusting
this file's claims if picking this up later; this file is a snapshot, not a
substitute for checking the real repository state.

This file is the living status snapshot. `AGENTS.MD` is the stable rules/conventions
file — read that first for *how* to work on this repo, this file for *where things
currently stand*. Update this file (not `AGENTS.MD`) after any verification pass or
significant change; keep `AGENTS.MD` stable unless a rule itself changes.

---

## Current phase

**Phase 6 remains implemented and tested. This session added a single, small
Feedback feature to Phase 8 (UI)** — see `ROADMAP.md` for the phase table.

**Feedback (this session)**: Settings → Feedback is a new row ("Feedback" / "Help
improve Orlune") placed between "Privacy & Legal" and "About Orlune". Tapping it calls
`platform/feedback/FeedbackIntent.compose()`, which builds an
`Intent(ACTION_SENDTO, Uri.parse("mailto:"))` — deliberately not `ACTION_SEND`, so
Android resolves it against email composers only, not the general share sheet —
addressed to `dallemahesh09@gmail.com`, subject "Orlune Feedback", and a fixed body
template with `[User writes here]` / `Device / Android version: [optional]`
placeholders the user fills in themselves; nothing about the device is read or
appended automatically. `OrluneRoot.kt` wraps the `startActivity` call in a
try/catch for `ActivityNotFoundException` and shows a plain `AlertDialog` ("No email
app is available on this device.") instead of crashing when no handler exists. No
manifest changes were needed or made — ACTION_SENDTO/mailto is one of Android's
package-visibility-exempt "common intents", so no new `<queries>` entry, no new
permission, and no `INTERNET` permission. A short "## Feedback" section was also
added to the "About Orlune" legal document describing the same mechanism. See
`FeedbackIntentTest.kt` for the plain-Kotlin recipient/subject/body assertions and
the Physical-device section below for the on-device ACTION_SENDTO/dialog
verification.

Earlier this phase (prior session), five isolated changes were made, each
built/tested/device-verified before starting the next:

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
`GRADLE_USER_HOME=F:\GradleUserHome`), after adding the Feedback feature:

- `.\gradlew.bat assembleDebug --stacktrace` → **BUILD SUCCESSFUL**
- `.\gradlew.bat testDebugUnitTest --stacktrace` → **BUILD SUCCESSFUL**, **158/158**
  unit tests pass, 0 failures, 0 errors (summed from the real per-suite XML results
  in `app/build/test-results/testDebugUnitTest/`; 154 from before this session +
  4 new in `FeedbackIntentTest`)
- `.\gradlew.bat connectedDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**,
  **28/28** instrumentation tests pass, run against a real physical device (Pixel 7a,
  Android 17 / API 37, serial `32201JEHN04765`) — unchanged from before this session;
  Feedback's Intent-building half isn't unit- or instrumentation-tested (no
  Robolectric in this project, and it's simple platform glue), it's verified by the
  on-device walkthrough below instead

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
| Unit | `AppDisplayResolverTest` | 8 | pass |
| Unit | `ScheduleInputTest` | 9 | pass |
| Unit | `DurationStepperTest` | 9 | pass |
| Unit | `InsightsMetricsTest` | 10 | pass |
| Unit | `FeedbackIntentTest` (new) | 4 | pass |
| **Unit total** | | **158** | **158/158 pass** |
| Instrumentation | `OrluneDatabaseMigrationTest` | 1 | pass (real device) |
| Instrumentation | `BlockingRepositoryInstrumentedTest` | 8 | pass (real device) |
| Instrumentation | `UsageRepositoryInstrumentedTest` | 7 | pass (real device) |
| Instrumentation | `ThemePreferenceDaoInstrumentedTest` | 4 | pass (real device) |
| Instrumentation | `InstalledAppListerInstrumentedTest` | 8 | pass (real device) |
| **Instrumentation total** | | **28** | **28/28 pass** |
| **Grand total** | | **186** | **186/186 pass** |

## Physical-device status — VERIFIED (manual walkthrough, this session)

Pixel 7a connected over USB, via `adb`/`uiautomator` (text-dump + screenshot
verification of real rendered UI content, not just build success). Device left as
found afterward.

**Feedback flow (this session):**
- Settings → scrolled down → "Feedback" / "Help improve Orlune" row confirmed present
  between "Privacy & Legal" and "About Orlune" (uiautomator text dump).
- Tapped Feedback with Gmail installed and enabled: opened Gmail's own compose screen
  (`com.google.android.gm`), not a general share sheet. Screenshot-verified: **To**
  chip resolved to `dallemahesh09@gmail.com` ("M. Dalle"), **Subject** =
  "Orlune Feedback" exactly, **Body** prefilled exactly as specified ("Hello Orlune
  team," / "I would like to share the following feedback:" / "[User writes here]" /
  "Device / Android version:" / "[optional]" / "Thank you."), with the cursor left in
  the body for the user to edit. Backed out without sending; draft discarded, Orlune
  regained foreground cleanly, no crash.
- Fallback path: `adb shell pm disable-user --user 0 com.google.android.gm`
  (temporary, reversed immediately after with `pm enable`), relaunched Orlune, tapped
  Feedback again — the app showed its own `AlertDialog`, title "No email app found",
  body "No email app is available on this device.", exactly one "OK" button; no
  `FATAL EXCEPTION`, no `ActivityNotFoundException` crash. Gmail re-enabled and
  confirmed no longer in the disabled-package list immediately after.
- No `INTERNET` permission, no new manifest permission, no new `<queries>` entry —
  confirmed via `git diff -- app/src/main/AndroidManifest.xml` showing zero changes.

**Prior session's walkthrough** (app picker, launcher icon, Legal Center, theme
navigation, force-stop/relaunch persistence, the Bundle-unsafe `SaveableStateProvider`
key crash fix) is unchanged by this session — see git history for that detail if
needed.

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
- **Feedback** (this session): `platform/feedback/FeedbackIntent.kt` — a local,
  backend-free email handoff reachable from Settings → Feedback. See "Current phase"
  above for the full mechanism.
- Privacy architecture: no `INTERNET` permission anywhere, no analytics/ads/AI
  dependency — re-verified this session via `git diff -- app/src/main/AndroidManifest.xml`
  showing zero changes (the Feedback feature needed no manifest change at all); no
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
