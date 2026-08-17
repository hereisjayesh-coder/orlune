# Orlune — Project State

**Last verification date:** 2026-08-17 (added Focus notification/quiet mode: a
per-session notification policy — Allow all / Silence all / Allow calls / Allow calls
+ selected apps — enforced via a single system-owned `AutomaticZenRule`, never the
older whole-device DND APIs. Two real bugs were found and fixed only by testing on
real hardware; see "Current phase" below and `docs/android-notification-policy.md`
for the full detail. `c72f7ea`'s Feedback feature is unchanged by this session.)

**Latest verified commit:** working tree as of this file's date, on top of `c72f7ea`
("feat: add local email feedback flow") — this session's changes are **not yet
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

**Phase 6 remains implemented and tested. This session added Focus notification /
quiet mode to Phase 8 (UI)** — see `ROADMAP.md` for the phase table.

**Focus notification / quiet mode (this session)** — full technical writeup in
`docs/android-notification-policy.md`; summary here:

- **Setup**: Focus screen gained a "Notification interruptions" chip selector (Allow
  all / Silence all / Allow calls / Allow calls + selected apps —
  `core/domain/focus/FocusNotificationPolicy.kt`), a duration-preset chip row (25m /
  45m / 60m / 90m / Custom — `ui/components/DurationPresetSelector.kt`), and, for
  "Allow calls + selected apps", a second app picker for the allowed-apps selection
  (reuses `AppPickerScreen`'s existing `Multi` mode via a new `FocusSection`
  back-stack destination). `FocusSessionEntity` gained
  `notificationPolicy`/`allowedNotificationPackages` columns (Room v2→v3,
  `OrluneMigrations.MIGRATION_2_3`, both `NOT NULL DEFAULT` so old rows behave exactly
  as before — no retroactive silencing).
- **Permission disclosure**: exactly the required copy ("Focus can silence
  interruptions while you work. Orlune does not read or store your notification
  content.") plus an "Open notification settings" button
  (`Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`), shown only when the user
  has picked a policy other than "Allow all" and access isn't yet granted — "Allow
  all" never requests or needs this permission at all.
- **Enforcement mechanism**: exactly one system-owned `AutomaticZenRule` ("Orlune
  Focus"), never the older whole-device `setInterruptionFilter()`/
  `setNotificationPolicy()` APIs. Android's own Zen Mode already combines every
  currently-active rule (manual DND, Bedtime, other apps, this one) as "most
  restrictive wins" — Orlune only ever toggles its own rule's `Condition`
  (`STATE_TRUE`/`STATE_FALSE`) via `platform/notifications/FocusZenRuleController.kt`,
  reconciled every `BlockingMonitorService` tick (≤3s latency, same budget as
  blocking-overlay enforcement) plus once at every process cold start. Overlapping
  Orlune sessions resolve to the single most restrictive active policy
  (`effectiveFocusNotificationState()`, unit-tested, mirrors how `blockedPackages`
  already union across sessions). No `NotificationListenerService` — proven
  unnecessary; this feature never reads notification content, only whether the system
  lets one interrupt.
- **Two real bugs, found only by testing on real hardware** (Pixel 7a, API 37; see
  `docs/android-notification-policy.md` for full detail and exact `dumpsys`
  evidence):
  1. Orlune was completely absent from Settings' "Do Not Disturb access" grant list
     until `android.permission.ACCESS_NOTIFICATION_POLICY` (a normal, auto-granted
     permission) was added to the manifest — required purely for discoverability,
     exactly mirroring `PACKAGE_USAGE_STATS`'s existing documented requirement for the
     Usage Access list.
  2. `addAutomaticZenRule` threw `IllegalArgumentException: Rule must have a valid
     (enabled) ConditionProviderService or configurationActivity` until the rule was
     given `configurationActivity = MainActivity` (the initial `owner`-only approach,
     which several older tutorials describe, does not satisfy this OS version's
     validation). Separately, `BlockingMonitorService`'s tick was self-stopping
     (`hasWorkToEnforce() == false`) *before* ever reconciling the now-off
     notification policy, and `OrluneApplication`'s cold-start resume check never
     restarted the service at all once no rule/session remained — both left the Zen
     rule stuck `STATE_TRUE` (device-wide DND stuck on) with no future tick ever able
     to turn it off. Fixed by reconciling before the early-return/`stopSelf()`, and by
     adding an unconditional one-shot reconciliation to `OrluneApplication.onCreate()`
     independent of whether `BlockingMonitorService` itself has a reason to run.
- **What Orlune can/cannot guarantee**: documented explicitly, in-UI and in
  `docs/android-notification-policy.md` — cannot guarantee emergency calls reach the
  user on every device (depends on Android/OEM DND behavior); outgoing emergency
  dialing is entirely unaffected; Orlune cannot mark another app's channel/conversation
  as DND-priority on the user's behalf, only whether previously-user-marked ones bypass.
- **Tests**: `FocusNotificationPolicyTest.kt` (20 tests — policy selection, restrictiveness
  ordering, `toZenSpec` mapping, overlapping-session resolution including ties and
  package-set union scoping, malformed stored values, cancelled/interrupted sessions),
  3 new `FocusSessionRepositoryTest` cases (notification-policy persistence, defaults,
  normalization), 1 new `OrluneDatabaseMigrationTest` case (`MIGRATION_2_3`, v2→v3 row
  survival). The Android-touching half (`FocusZenPolicyMapper`'s real `ZenPolicy`
  construction, `FocusZenRuleController`'s system calls) isn't JVM-unit-testable (no
  Robolectric) — verified entirely on-device instead; see Physical-device section
  below.

**Prior session (Feedback)**: Settings → Feedback hands off to the device's own email
app via `ACTION_SENDTO` + a mailto URI — see `c72f7ea`'s commit and prior snapshots
for detail; unchanged by this session.

Earlier this phase (two sessions ago), five isolated changes were made, each
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
`GRADLE_USER_HOME=F:\GradleUserHome`), after adding Focus notification/quiet mode and
both fixes described above (final run, after the fixes — not the earlier runs that
surfaced the two bugs):

- `.\gradlew.bat assembleDebug --stacktrace` → **BUILD SUCCESSFUL**
- `.\gradlew.bat testDebugUnitTest --stacktrace` → **BUILD SUCCESSFUL**, **181/181**
  unit tests pass, 0 failures, 0 errors (summed from the real per-suite XML results
  in `app/build/test-results/testDebugUnitTest/`; 158 from before this session + 23
  new: 20 in `FocusNotificationPolicyTest` + 3 new `FocusSessionRepositoryTest` cases)
- `.\gradlew.bat connectedDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**,
  **29/29** instrumentation tests pass, run against a real physical device (Pixel 7a,
  Android 17 / API 37, serial `32201JEHN04765`) — 28 from before this session + 1 new
  `OrluneDatabaseMigrationTest` case (`MIGRATION_2_3`)

## Test status — VERIFIED (real counts, not claimed)

| Suite | Class | Tests | Result |
|---|---|---|---|
| Unit | `FocusSessionEngineTest` | 13 | pass |
| Unit | `BlockingEngineTest` | 8 | pass |
| Unit | `LegalMarkdownTest` | 7 | pass |
| Unit | `GoalEngineTest` | 6 | pass |
| Unit | `LimitEngineTest` | 6 | pass |
| Unit | `ScheduleEngineTest` | 14 | pass |
| Unit | `SessionCalculatorTest` | 11 | pass |
| Unit | `UsageAggregatorTest` | 4 | pass |
| Unit | `BlockingRepositoryTest` | 22 | pass |
| Unit | `FocusSessionRepositoryTest` | 11 (+3 new) | pass |
| Unit | `DailyLimitInputTest` | 13 | pass |
| Unit | `LegalDocumentsTest` | 7 | pass |
| Unit | `AppDisplayResolverTest` | 8 | pass |
| Unit | `ScheduleInputTest` | 9 | pass |
| Unit | `DurationStepperTest` | 8 | pass |
| Unit | `InsightsMetricsTest` | 10 | pass |
| Unit | `FeedbackIntentTest` | 4 | pass |
| Unit | `FocusNotificationPolicyTest` (new) | 20 | pass |
| **Unit total** | | **181** | **181/181 pass** |
| Instrumentation | `OrluneDatabaseMigrationTest` | 2 (+1 new: `MIGRATION_2_3`) | pass (real device) |
| Instrumentation | `BlockingRepositoryInstrumentedTest` | 8 | pass (real device) |
| Instrumentation | `UsageRepositoryInstrumentedTest` | 7 | pass (real device) |
| Instrumentation | `ThemePreferenceDaoInstrumentedTest` | 4 | pass (real device) |
| Instrumentation | `InstalledAppListerInstrumentedTest` | 8 | pass (real device) |
| **Instrumentation total** | | **29** | **29/29 pass** |
| **Grand total** | | **210** | **210/210 pass** |

## Physical-device status — VERIFIED (manual walkthrough, this session)

Pixel 7a connected over USB, via `adb`/`uiautomator` + `dumpsys notification --zen`/
`settings get global zen_mode` (real system Zen state, not just UI text) and `logcat`
(real exception traces, not assumed). Device left as found afterward — Notification
Policy Access, Usage Access, and Overlay all confirmed still granted at the end;
Orlune's Zen rule confirmed `STATE_FALSE` and device-wide `zen_mode=0` at the end.

**Focus notification/quiet mode (this session)** — both bugs below were found by this
walkthrough, not by code review, and both are fixed and re-verified in the final pass:

- **Permission grant flow**: selecting any policy other than "Allow all" with access
  not yet granted showed the exact required disclosure text and an "Open notification
  settings" button; tapping it opened `Settings$ZenAccessSettingsActivity` (confirmed
  via `dumpsys window`). **Bug 1**: Orlune was completely absent from that screen's
  app list (confirmed by granular full-list scroll — alphabetically skipped between
  neighboring apps) until `ACCESS_NOTIFICATION_POLICY` was added to the manifest;
  after that fix, Orlune appeared and the grant flow (tap app → toggle → confirmation
  dialog → "Allow") worked normally.
- **Policy correctness, verified via `dumpsys notification --zen`, not just UI text**:
  - "Silence all" → `zenPolicy` read `calls=disallow repeatCallers=disallow
    alarms=allow` (everything else disallow) — alarms confirmed always audible.
  - "Allow calls" → `calls=allow repeatCallers=allow priorityCallsSenders=anyone` —
    confirmed distinct from "Silence all", not a stale/leftover rule.
  - Device-wide `zen_mode`/`mInterruptionFilter` flipped to the active/restrictive
    values while a session was running and back to `0`/off once ended — real DND
    state, not just an app-local flag.
- **Bug 2** (found while testing "Start Focus"): `addAutomaticZenRule` threw
  `IllegalArgumentException: Rule must have a valid (enabled) ConditionProviderService
  or configurationActivity` — caught by the existing try/catch (no crash), but the
  rule silently never activated. Fixed by passing `configurationActivity =
  MainActivity` instead of an `owner`-only rule; re-verified: rule registers and
  activates correctly.
- **Bug 2b** (found while testing "Stop Focus"): after fixing rule registration, DND
  stayed *on* after ending a session — `BlockingMonitorService`'s tick decided there
  was no more work and called `stopSelf()` before ever reconciling the now-off
  notification policy. Confirmed stuck at `zen_mode=1`/`STATE_TRUE` across several
  app relaunches (a second related gap: `OrluneApplication`'s cold-start resume check
  never restarted the service once no session/rule remained, so no future tick could
  fix it either). Both fixed (reconcile-before-`stopSelf()`, plus an unconditional
  cold-start reconciliation in `OrluneApplication.onCreate()`); re-verified: "Stop
  Focus" now flips the rule to `STATE_FALSE` and `zen_mode` to `0` within one tick
  (~4s), confirmed via `dumpsys` immediately after tapping.
- **Permission revocation mid-flow**: revoked access via Settings (confirmation
  dialog: "All modes created by this app will be removed"), confirmed via the app's
  own UI that the disclosure card correctly reappeared. Started a Focus session with
  access still revoked: no crash, app-blocking selection unaffected, Zen rule never
  activated (`zen_mode` stayed `0`), UI correctly showed "Notifications: silenced —
  not applied (grant notification access in Settings)". Zero `SecurityException`
  needed — the `isGranted()` check caught it before any system call. Re-granted
  access afterward and confirmed normal operation resumed.
- **An unrelated real incoming phone call** occurred mid-test (call state confirmed
  via `dumpsys telephony.registry`: `mCallState=2`/OFFHOOK, active ~15s, then
  `mCallState=0`/idle on its own). Not triggered by this testing — no tap was directed
  at any call-control element; the session paused interaction entirely until the call
  state returned to idle, then resumed. Recorded here for full transparency about
  everything that touched the device during this walkthrough.
- No `INTERNET` permission; the only manifest change is the single normal,
  auto-granted `ACCESS_NOTIFICATION_POLICY` line — confirmed via
  `git diff -- app/src/main/AndroidManifest.xml`.

**Prior sessions' walkthroughs** (Feedback's ACTION_SENDTO flow, app picker, launcher
icon, Legal Center, theme navigation, force-stop/relaunch persistence, the
Bundle-unsafe `SaveableStateProvider` key crash fix) are unchanged by this session —
see git history for that detail if needed.

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
- **Feedback**: `platform/feedback/FeedbackIntent.kt` — a local, backend-free email
  handoff reachable from Settings → Feedback.
- **Focus notification / quiet mode** (this session): per-session notification policy
  (Allow all / Silence all / Allow calls / Allow calls + selected apps), enforced via
  one system-owned `AutomaticZenRule` — `core/domain/focus/FocusNotificationPolicy.kt`
  + `platform/notifications/`. See "Current phase" above and
  `docs/android-notification-policy.md` for the full mechanism, including two real
  bugs found and fixed by on-device testing.
- Privacy architecture: no `INTERNET` permission anywhere, no analytics/ads/AI
  dependency — re-verified this session via `git diff -- app/src/main/AndroidManifest.xml`,
  which shows exactly one addition: `android.permission.ACCESS_NOTIFICATION_POLICY`
  (a normal, auto-granted permission required purely for discoverability in Settings'
  "Do Not Disturb access" list — grants nothing on its own; see
  `docs/android-notification-policy.md`); no changes to `app/build.gradle.kts` or
  `gradle/libs.versions.toml`.

## Unfinished / not started

Onboarding is still not a dedicated first-run flow. Recurring focus-session
scheduling remains out of scope pending a separate product decision. The Privacy &
Legal Center's documents are development drafts, not lawyer-reviewed or published —
see `docs/legal-compliance-matrix.md`'s "Legal review required" column for exactly
what's outstanding before any public claim of compliance. No privacy-policy URL is
hosted anywhere yet, which blocks Google Play submission independent of the document
content itself. Analytics/recommendation algorithms, AccessibilityService, and
website/VPN blocking remain deliberately deferred. Focus notification/quiet mode's
API-29 (legacy `INTERRUPTION_FILTER_*`) fallback path is unverified on real API 29
hardware — this project's only test device is API 37 (see
`docs/android-notification-policy.md`).

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
  called). The `observeLongestSessionBetween` query relies on that actual (not
  documented) retention behavior. Not a regression, but worth flagging: either the
  KDoc or the retention behavior should eventually be corrected to match the other.
  Deliberately not touched this session either, per explicit instruction.
- Focus notification/quiet mode's API-29 legacy fallback path
  (`FocusZenPolicyMapper.toLegacyInterruptionFilter`) and the `allowPriorityChannels`
  API-31 guard are both unverified on real hardware below API 37 — see
  `docs/android-notification-policy.md` for exactly what's unverified and why.
- OEM variance in how third-party `AutomaticZenRule`s are surfaced/handled is
  reported by community sources for some heavily-skinned builds — not independently
  verified or falsified on this project's single test device (same caution already
  applied to OEM background-kill risk elsewhere in this file).

## Next recommended task

Pick one, don't start more than one without checking in:

1. Onboarding flow — the largest remaining piece of Phase 8, and the natural place to
   introduce the permission requests and app picker for the first time.
2. Legal review of the Privacy Policy / Terms of Service against
   `docs/legal-compliance-matrix.md`'s open issues, plus resolving the actual business
   details (legal entity, address, contact) currently held as placeholders.
3. If a second Android test device becomes available (ideally API 29–31), verify
   Focus notification/quiet mode's legacy fallback path and the `allowPriorityChannels`
   API guard — both currently unverified on real hardware below API 37.

Do not start Focus notification/quiet-mode work again without reviewing
`docs/android-notification-policy.md` first — it's now implemented, tested, and
device-verified this session, not still pending.

Do not start any without user sign-off. This session's changes exist only as
uncommitted working-tree changes — not committed, not pushed; see the session's own
summary for the exact file list.
