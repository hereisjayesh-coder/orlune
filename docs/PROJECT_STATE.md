# Orlune — Project State

**Last verification date:** 2026-08-18 (workspace migration/backup session — see
"Workspace migration" section below. No product feature work was done. The
2026-08-17 first-launch onboarding entry below is otherwise unchanged and describes
real prior work; only its "not yet committed" claim is now stale — see the corrected
commit note immediately below.)

**Latest verified commit:** `47ef5cc` ("chore: checkpoint before workspace
migration"), pushed to and matching `origin/main`, on top of `fa1336e` ("feat: add
first-launch onboarding" — the 2026-08-17 work described below, which **is now
committed**, correcting that entry's "not yet committed" claim). `47ef5cc` itself
checkpoints a *separate*, newer batch of previously-uncommitted work found already
sitting in the working tree at the start of this migration session: a rule-snooze
feature (`RuleSnoozeEntity`, `RuleSnoozeDao`, `RuleRepository`), a new
`core/domain/onboarding/OnboardingDailyLimit.kt`, a new `ui/components/SafeArea.kt`,
Room schema v5, and related modifications across
`BlockingRepository`/`OrluneDatabase`/`OrluneMigrations`/`FocusScreen`/
`FocusSection`/onboarding screens/`BlockOverlayController`/`BlockingMonitorService`/
`OrluneRoot`/`Rows`, plus their unit/instrumentation tests. **This batch's
build/test/device status has not been independently re-verified in this migration
session** — it was committed as-is, untouched, purely to prevent data loss ahead of
the drive move. Do not assume it is build-clean or feature-complete; verify with
`git show 47ef5cc --stat` and a real build before treating it as done. Always verify
with `git log`/`git status` before trusting this file's claims if picking this up
later; this file is a snapshot, not a substitute for checking the real repository
state.

This file is the living status snapshot. `AGENTS.MD` is the stable rules/conventions
file — read that first for *how* to work on this repo, this file for *where things
currently stand*. Update this file (not `AGENTS.MD`) after any verification pass or
significant change; keep `AGENTS.MD` stable unless a rule itself changes.

---

## Workspace migration (2026-08-18)

The primary workspace is moving off `D:\App` because the `C:` drive is nearly full
and the user is preparing to modify/merge the `D:` partition into `C:`. The project
was pre-copied by the user to **`F:\Orlune\App`** (not `F:\App` — the user's own copy
step landed one directory level deeper than expected; this was verified directly,
not assumed).

- Both `D:\App` and `F:\Orlune\App` working trees are confirmed byte-identical
  (`diff -rq`, excluding `.git`/`build`/`.gradle`), including every previously
  uncommitted file.
- `D:\App`'s working tree was committed and pushed as `47ef5cc` (see above) before
  migration, so no work exists only as uncommitted state anymore.
- `F:\Orlune\App`'s `.git` has fetched `47ef5cc` but its `HEAD`/index are still one
  commit behind, at `fa1336e` (a `git reset`/`git pull` there was blocked by this
  session's tool permissions). **Remaining action:** from `F:\Orlune\App`, run
  `git pull --ff-only origin main` (working-tree content already matches, so this
  should apply cleanly) to bring `HEAD` in sync with `origin/main`.
- A full zip backup of `D:\App` (`.git`, `.claude`, everything) was made at
  `D:\ORLUNE_BACKUP_20260818_091020.zip` (~51 MB) as an independent safety copy, plus
  a `git diff --binary` patch at `D:\App\ORLUNE_WORKING_TREE_BACKUP.patch` (superseded
  by the `47ef5cc` commit itself — kept only as a redundant local artifact, not
  committed).
- `D:\App` has **not** been deleted and should not be until the remaining action
  above is done and a build is verified from `F:\Orlune\App`.
- All future sessions should open the project at **`F:\Orlune\App`**, not `D:\App`
  or `F:\App`.

---

## Current phase

**Phase 6 remains implemented and tested. This session completed first-launch
onboarding, the largest remaining piece of Phase 8** — see `ROADMAP.md` for the phase
table.

**First-launch onboarding (this session)** — 11 screens, one shared back-stack
(`feature/onboarding/OnboardingSection.kt`, same `mutableStateListOf<Destination>` +
`rememberSaveableStateHolder` pattern as `FocusSection`/`LimitsSection`/
`SettingsSection`), gated at the very top of `OrluneRoot.kt`: while onboarding isn't
complete, `OnboardingSection` renders instead of the tab `Scaffold`, nothing else in
`OrluneRoot` changed structurally.

1. **Welcome** — "ORLUNE" / "Take back your time." / "A private, local tool for more
   intentional digital use." / **Get Started**, the approved launcher icon mark
   (`R.mipmap.ic_launcher_foreground`, same asset `SettingsScreen`'s "About Orlune"
   row already uses).
2. **What Orlune does** — five plain bullets (tracks usage locally / helps set limits
   / can interrupt distracting apps / provides Focus sessions / keeps data on this
   device). No medical-benefit or "cures" language anywhere, per the explicit
   instruction.
3. **Privacy** — "Your data stays on this device" + seven bullets (no account, no
   cloud, no advertising, no AI, no usage-data upload, local database, export/delete
   anytime) — every claim backed by the actual architecture (no `INTERNET` permission
   exists, so "nothing leaves this device" is enforced, not just promised) — plus a
   **Privacy & Legal** secondary button that pushes the *real*
   `LegalCenterScreen`/`LegalDocumentScreen` (Settings' existing 15-document Legal
   Center) onto the same back-stack — not a copy, the same composables.
4. **Usage Access** — the exact suggested copy, a **Grant Usage Access**/**Not now**
   pair that becomes a single **Continue** once granted (a "Granted" success line
   shown), reusing `UsageAccessPermission` unchanged — no new permission-check logic.
5. **Blocking screen** — plain-language overlay explanation (why/what it does/"Orlune
   does not record your screen"), never mentions `SYSTEM_ALERT_WINDOW` in the user
   copy, **Enable**/**Skip for now**, reusing `OverlayPermission` unchanged.
6. **Focus notifications** — introduces the four Phase 8 policies via the *real*
   `NotificationPolicySelector` component (not a re-implementation); selecting "Allow
   all" requests nothing; selecting any silencing mode shows the exact same
   disclosure card + Settings button `FocusScreen` already has. Entirely optional —
   the primary button is always just **Continue**, never blocked on granting
   anything.
7. **Goal** — "What would you like more time for?" — ten chips (🎯 Focus, 📚 Study,
   🧠 Learn, 🗣 Communication, 🧘 Reset, 🚶 Move, ✍️ Create, 📖 Read, 🌙 Rest, Custom),
   multi-select, freely skippable, a text field appears only when Custom is picked.
   New pure `core/domain/onboarding/OnboardingGoal.kt` enum — purely descriptive,
   nothing in this codebase branches on it yet (reserved for later *optional*
   personalization, explicitly not built now, no AI).
8. **Choose apps** — the *real* `AppPickerScreen` (Multi mode), heading swapped to
   "Which apps steal your time?" via two new optional parameters
   (`title`/`subtitle`, defaulting to the existing text so every other caller is
   unaffected) plus a new optional `onSkip` parameter (shows an explicit "Skip"
   action next to "Done" without loosening "Done"'s existing
   enabled-only-with-a-selection rule for `FocusSection`/`LimitsSection`). Orlune's
   own package and system apps are never pre-selected — inherited for free from the
   picker's existing `ownPackageName` exclusion and empty `initialSelection`.
9. **Daily limit** — the exact same preset/custom shape as the real Limits screen
   (30/45/60/90 chips + `DurationStepper` for Custom, `DailyLimitInput` validation
   unchanged), **Skip for now** available.
10. **Finish** — "Your Orlune setup" summary (Goal / Apps / Daily limit / Focus
    notifications) exactly matching the requested example format, **Start using
    Orlune** commits everything to Room *once*, atomically:
    - `OnboardingRepository.complete(...)` — new singleton-row `onboarding_state`
      table (Room v3→v4, `OrluneMigrations.MIGRATION_3_4`, a new table so no existing
      row anywhere is touched), sets `completed = true` plus the goal/custom-text/
      focus-notification-preference choices.
    - One `RuleEntity(type = "limit", ...)` per selected app (only if a limit wasn't
      skipped) via the exact same `ruleDao().upsert(...)` call `LimitsSection`'s
      "Add limit" already uses — not a parallel code path.
    - An onboarding session interrupted before Finish (process death, force-quit)
      commits nothing and simply restarts from Welcome next launch — no partial
      state to reconcile.
11. **Home** — the existing tab UI, unchanged.

**Existing-install safety (upgrading into this feature, not a fresh install)**:
`onboarding_state` is a brand-new table — an install that already had real data
before this feature shipped would otherwise see "no row" and get shown onboarding for
the first time on update, which is wrong (onboarding is a first-*launch* concept, not
a first-*app-version* one). Fixed:
`OrluneApplication.backfillOnboardingCompletionForExistingInstalls()` — an
unconditional one-shot check at every cold start (independent of Usage Access/Overlay,
which onboarding has nothing to do with): if no onboarding row exists yet AND there's
real evidence of prior use (any session, rule, or focus session ever recorded), it
silently backfills a completed row with empty/default choices, never shown to the
user. A genuinely fresh install has zero rows in all three tables, so onboarding still
shows normally. One accepted, documented tradeoff: `OrluneRoot`'s
`collectAsState(initial = false)` for the completion flag means an *upgrading*
install's very first post-update launch can show a brief, one-frame flash of
onboarding before the backfill's Room write lands and the real value arrives —
self-correcting within the same cold start, not a functional issue, not repeatable on
any later launch.

**Focus's stored preference feeds back as a real default, not a write-only field**:
`FocusSection` gained an optional `initialNotificationPolicy` parameter (default
`ALLOW_ALL`, so the existing call shape is unaffected everywhere else) — `OrluneRoot`
seeds it from `OnboardingRepository.observeFocusNotificationPreference()`, so the
policy chosen (or left at "Allow all") during onboarding is what the user's first real
Focus session starts pre-selected to. Confirmed on-device: choosing "Silence all" at
onboarding, completing, restarting the app, and opening Focus showed "Silence all"
already selected with its disclosure card correctly present (permission was
deliberately left ungranted during this test).

**A real rendering bug, found only by running the flow on-device**:
`OnboardingSection` was originally composed directly under `OrluneRoot`, outside any
`Surface`. Compose's default `LocalContentColor` (which any `Text` without an
explicit `color` falls back to) is **not** made visible by `MaterialTheme` alone —
only a `Surface` establishes it correctly, which is what the tab content already had
and onboarding didn't. Every un-colored title `Text` across all 11 screens — the
entire `OnboardingScaffold` title slot, the Welcome headline — was present in the view
tree (confirmed via `uiautomator dump`, the text was there) but fully invisible: black
text on this app's black background. First screenshot showed "ORLUNE" and the
subtitle rendering fine (both have explicit `onSurfaceVariant` color) with a blank gap
where the large bold headline should be. Fixed by wrapping the onboarding branch in
`Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)`,
identical to what the tab `Scaffold` content already does — re-verified via a second
screenshot showing the headline correctly rendered. Documented as a standing rule in
`AGENTS.MD` for any future top-level branch in `OrluneRoot.kt`.

**Prior session (Focus notification/quiet mode, `bdf4c58`)**: per-session notification
policy enforced via one system-owned `AutomaticZenRule` — see
`docs/android-notification-policy.md`; unchanged by this session except for the
`initialNotificationPolicy` feed-in described above.

**Prior session (Feedback, `c72f7ea`)**: Settings → Feedback hands off to the device's
own email app via `ACTION_SENDTO` + a mailto URI; unchanged by this session.

Earlier this phase (three sessions ago), five isolated UI changes replaced raw
package-name input with the native app picker across Limits/Focus, added the
launcher icon and the 15-document Legal Center, and added Insights' "Last 14 days at
a glance" card — see git history / this file's earlier revisions for that detail.

## Build status — VERIFIED

Ran on 2026-08-17 in this environment (`JAVA_HOME=F:\Android Stu\jbr`,
`GRADLE_USER_HOME=F:\GradleUserHome`), final run after the rendering-bug fix above —
not the earlier run that surfaced it:

- `.\gradlew.bat assembleDebug --stacktrace` → **BUILD SUCCESSFUL**
- `.\gradlew.bat testDebugUnitTest --stacktrace` → **BUILD SUCCESSFUL**, **195/195**
  unit tests pass, 0 failures, 0 errors (summed from the real per-suite XML results
  in `app/build/test-results/testDebugUnitTest/`; 181 from before this session + 14
  new: 6 in `OnboardingGoalTest` + 8 in `OnboardingRepositoryTest`)
- `.\gradlew.bat connectedDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**,
  **30/30** instrumentation tests pass, run against a real physical device (Pixel 7a,
  Android 17 / API 37, serial `32201JEHN04765`) — 29 from before this session + 1 new
  `OrluneDatabaseMigrationTest` case (`MIGRATION_3_4`)

The onboarding flow's own navigation/rendering (11 screens, both side-trips, the
permission grant flows) has **no dedicated Compose UI test** — this project has never
had a Compose UI testing dependency (`androidx.compose.ui:ui-test-junit4` is not in
`app/build.gradle.kts`, confirmed by grep before writing any test), consistent with
every other screen in this codebase. It is verified entirely by the on-device manual
walkthrough below, exactly like every other screen already was.

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
| Unit | `FocusSessionRepositoryTest` | 11 | pass |
| Unit | `DailyLimitInputTest` | 13 | pass |
| Unit | `LegalDocumentsTest` | 7 | pass |
| Unit | `AppDisplayResolverTest` | 8 | pass |
| Unit | `ScheduleInputTest` | 9 | pass |
| Unit | `DurationStepperTest` | 8 | pass |
| Unit | `InsightsMetricsTest` | 10 | pass |
| Unit | `FeedbackIntentTest` | 4 | pass |
| Unit | `FocusNotificationPolicyTest` | 20 | pass |
| Unit | `OnboardingGoalTest` (new) | 6 | pass |
| Unit | `OnboardingRepositoryTest` (new) | 8 | pass |
| **Unit total** | | **195** | **195/195 pass** |
| Instrumentation | `OrluneDatabaseMigrationTest` | 3 (+1 new: `MIGRATION_3_4`) | pass (real device) |
| Instrumentation | `BlockingRepositoryInstrumentedTest` | 8 | pass (real device) |
| Instrumentation | `UsageRepositoryInstrumentedTest` | 7 | pass (real device) |
| Instrumentation | `ThemePreferenceDaoInstrumentedTest` | 4 | pass (real device) |
| Instrumentation | `InstalledAppListerInstrumentedTest` | 8 | pass (real device) |
| **Instrumentation total** | | **30** | **30/30 pass** |
| **Grand total** | | **225** | **225/225 pass** |

`OnboardingGoalTest` covers `fromStored`/`parseOnboardingGoals` round-tripping,
malformed-value fail-safety, and dedup/trim parsing — pure, no Android dependency.
`OnboardingRepositoryTest` (hand-rolled fake `OnboardingStateDao`, no mocking library
per this project's convention) covers: first-launch state (no row ever written →
`observeCompleted() == false`), completion persistence, restart persistence (a second
`OnboardingRepository` instance over the same underlying row sees the same state,
simulating a process restart reading the same on-disk table), multi-goal persistence,
empty-goal persistence, custom-goal-text trimming, and focus-notification-preference
persistence/default. `migrate3To4_...` (instrumentation) verifies the new table is
created empty and every pre-existing `focus_sessions` row survives untouched.

## Physical-device status — VERIFIED (manual walkthrough, this session)

Pixel 7a connected over USB, via `adb`/`uiautomator` + screenshots (real rendered
pixels, not just UI-tree text — this is exactly how the rendering bug above was
actually caught) and `logcat` (real exception traces). A genuine fresh install was
used for the "fresh onboarding" walkthrough: `adb uninstall com.orlune.app` reported
`Failure [DELETE_FAILED_INTERNAL_ERROR]` but the app was confirmed actually removed
(`pm list packages | grep orlune` returned nothing) — a known quirky but harmless
Android behavior, not investigated further since the removal itself was verified
directly. Device left in a clean, working state afterward.

1. **Fresh onboarding**: Welcome screen appeared on first launch, exact required copy
   and layout confirmed via screenshot (after the rendering-bug fix — the first
   screenshot, before the fix, is what caught the bug).
2. **Grant Usage Access**: tapped "Grant Usage Access" → opened the real system Usage
   Access settings list → toggled Orlune on (confirmed via
   `adb shell appops get com.orlune.app android:get_usage_stats` →
   `GET_USAGE_STATS: allow`) → returned to Orlune → screen correctly showed "Granted"
   with a single "Continue" button (the "Not now" secondary correctly disappeared).
3. **Skip a permission**: on the Blocking-screen step, tapped "Skip for now" — advanced
   to Focus notifications without granting overlay; app remained usable throughout.
4. **Choose apps**: the real `AppPickerScreen` opened with the swapped heading "Which
   apps steal your time?" and the "For example: Instagram, YouTube, Facebook, Reddit."
   subtitle; selected 2 real installed apps (multi-select confirmed: "2 selected",
   two distinct checked rows), tapped "Done".
5. **Choose limit**: selected the "45m" preset chip (default was "60m"), preview text
   updated correctly.
6. **Complete onboarding**: Finish screen showed the exact expected summary — "Goal:
   Focus, Study" / "Apps: 2" / "Daily limit: 45m" / "Focus notifications: Silence
   all" (screenshot-confirmed) — tapped "Start using Orlune": no crash, the real tab
   UI (Home/Focus/Limits/Insights/Settings) appeared immediately, Home's "Rules" count
   read "2" — confirmed via the Limits tab showing two distinct "Daily limit · 45m"
   rows (two separate `RuleEntity` rows, one per selected app, both created from the
   single onboarding "Finish" tap).
7. **Restart app**: `adb shell am force-stop com.orlune.app` then relaunched.
8. **Onboarding does not reappear**: confirmed — Home tab loaded directly, "Rules: 2"
   still present, no crash (`logcat` checked for `FATAL`/`AndroidRuntime: java` —
   none from `com.orlune.app`; unrelated system/Google-Play-Services noise present
   and ignored, none referencing this app).
9. **Selected settings persisted**: opened the Focus tab after the restart — the
   "Silence all" notification-policy chip was already selected (confirmed via node
   bounds matching the checked chip to the "Silence all" text), with its disclosure
   card correctly still showing (permission was deliberately left ungranted during
   this walkthrough) — proves `initialNotificationPolicy` correctly threads from the
   onboarding choice through a real process restart, not just within one session.

Also verified during this walkthrough (not separately numbered above): the "Privacy &
Legal" secondary button on the Privacy screen opened the real Legal Center (15
documents listed) and the system back button correctly returned to the Privacy
screen with onboarding progress intact — the same `BackHandler` pattern already
proven in `FocusSection`/`LimitsSection`/`SettingsSection` behaves identically here,
including one accidental extra back-press during testing that correctly popped one
onboarding step backward rather than misbehaving.

**An unrelated real incoming phone call** occurred during the *previous* session's
(Focus notification/quiet mode) walkthrough, not this one — noted here only because
this file's convention is full transparency about anything that touched the test
device; nothing of the kind occurred during this session's testing.

## Implemented features

- Usage monitoring, deterministic rule engines, app blocking, and focus sessions —
  unchanged this session; see prior snapshots / `TODO.md` for their history.
- **App picker** (`feature/apppicker/AppPickerScreen.kt`): real installed-app icons
  and labels via `platform/usage/InstalledAppLister.kt` — not `QUERY_ALL_PACKAGES`
  (see `docs/app-visibility-compliance.md`). This session added two optional
  parameters (`title`/`subtitle`, defaulting to the existing text) and an optional
  `onSkip` callback (adds an explicit "Skip" action in Multi mode without loosening
  "Done"'s existing enabled-only-with-a-selection rule) — both purely additive,
  every existing caller (`LimitsSection`, `FocusSection`) unaffected.
- **Focus notification / quiet mode**: per-session notification policy, enforced via
  one system-owned `AutomaticZenRule` — `core/domain/focus/FocusNotificationPolicy.kt`
  + `platform/notifications/`. This session added `FocusSection.initialNotificationPolicy`
  so onboarding's Screen 6 choice becomes a real starting default. See
  `docs/android-notification-policy.md`.
- **First-launch onboarding** (this session): `feature/onboarding/` (11 screen
  composables + `OnboardingSection`/`OnboardingScaffold`/`OnboardingDestination`),
  `core/domain/onboarding/OnboardingGoal.kt`, `data/local/entity/OnboardingStateEntity.kt`
  + `OnboardingStateDao`, `data/repository/OnboardingRepository.kt`. See "Current
  phase" above for the full mechanism.
- **Feedback**: `platform/feedback/FeedbackIntent.kt` — a local, backend-free email
  handoff reachable from Settings → Feedback.
- **Privacy & Legal Center**: 15 documents, all still development drafts with
  explicit `[TBD]` placeholders for unresolved business/legal facts. This session's
  onboarding Privacy screen links directly into it — no new document content added.
- **Launcher icon**: adaptive-icon foreground + monochrome layers from the approved
  reference; this session's Welcome screen reuses the exact same
  `R.mipmap.ic_launcher_foreground` asset `SettingsScreen` already uses.
- Privacy architecture: no `INTERNET` permission anywhere, no analytics/ads/AI
  dependency — re-verified this session via `git diff -- app/src/main/AndroidManifest.xml`
  showing **zero changes** (onboarding needed no new permission or `<queries>` entry
  at all — every permission it requests is via an existing, already-declared
  mechanism); no changes to `app/build.gradle.kts` or `gradle/libs.versions.toml`.

## Unfinished / not started

Recurring focus-session scheduling remains out of scope pending a separate product
decision. The Privacy & Legal Center's documents are development drafts, not
lawyer-reviewed or published — see `docs/legal-compliance-matrix.md`'s "Legal review
required" column for exactly what's outstanding before any public claim of
compliance. No privacy-policy URL is hosted anywhere yet, which blocks Google Play
submission independent of the document content itself. Analytics/recommendation
algorithms, AccessibilityService, and website/VPN blocking remain deliberately
deferred. Focus notification/quiet mode's API-29 (legacy `INTERRUPTION_FILTER_*`)
fallback path remains unverified on real API 29 hardware — this project's only test
device is API 37 (see `docs/android-notification-policy.md`). Onboarding's goal
selections are captured and persisted but drive no behavior yet — reserved for a
later, explicitly-scoped, non-AI personalization feature, per the original
instruction.

## Known risks (not yet fixed — deliberately left for a future task)

See `AGENTS.MD`'s "Known risks" section for the standing list. Additions from this
session:

- **`OrluneRoot`'s onboarding-gate flash for upgrading installs** — documented in
  detail under "Current phase" above. One-time, one frame, self-correcting; not
  independently fixed further (would require blocking the UI thread on a Room read
  before first composition, adding real startup latency for every user to avoid a
  sub-second cosmetic flash for upgraders only).
- Onboarding's own screens have no dedicated Compose UI test — this project has never
  had that testing dependency; verified via manual on-device walkthrough instead,
  same as every other screen. If a Compose UI testing dependency is ever added for
  another reason, onboarding is the obvious first candidate to backfill given its
  size (11 screens) and how easily a bug like the rendering one above can hide from
  every other test category.
- Focus notification/quiet mode's API-29 legacy fallback path and the
  `allowPriorityChannels` API-31 guard remain unverified on real hardware below API
  37 (carried over from the prior session, unchanged).
- OEM variance in how third-party `AutomaticZenRule`s are surfaced/handled is
  reported by community sources for some heavily-skinned builds — not independently
  verified or falsified on this project's single test device (carried over,
  unchanged).
- The `sessions` table has no pruning implemented despite its own KDoc describing
  "short retention by design" (carried over, unchanged — not touched this session
  either).

## Next recommended task

Pick one, don't start more than one without checking in:

1. Legal review of the Privacy Policy / Terms of Service against
   `docs/legal-compliance-matrix.md`'s open issues, plus resolving the actual
   business details (legal entity, address, contact) currently held as placeholders.
2. Security/performance pass (Phase 10) or broader testing pass (Phase 11) — the
   next unstarted phases per `ROADMAP.md`.
3. If a second Android test device becomes available (ideally API 29–31), verify
   Focus notification/quiet mode's legacy fallback path and the
   `allowPriorityChannels` API guard — both currently unverified on real hardware
   below API 37.

Do not start onboarding work again without reviewing this file first — it's now
implemented, tested, and device-verified this session, not still pending. Do not
start Google Play / release-prep work without explicit sign-off — Phase 12/13 remain
untouched by design.

Do not start any without user sign-off. The onboarding work described above is
committed (`fa1336e`, pushed). A separate, newer batch of work (rule-snooze +
onboarding daily-limit — see the corrected commit note near the top of this file) was
found already uncommitted at the start of the 2026-08-18 migration session and was
checkpointed as `47ef5cc`, also pushed; its build/test status is unverified — see
"Workspace migration" above.
