# Orlune — Changelog

All notable changes to Orlune are recorded here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versioning follows
[Semantic Versioning](https://semver.org/) (see `docs/RELEASE_PROCESS.md` Section 1).

No version of Orlune has been published to Google Play. Play Store publishing is
currently paused; `1.0.0` below is distributed as a GitHub Release
(Portfolio/Beta status), not through Play.

## [Unreleased]

Nothing pending beyond `1.0.0`'s remaining Play Store release blockers (irrelevant
while Play publishing is paused) — see `docs/PROJECT_STATE.md`.

## [1.0.0] — 2026-08-19 — Portfolio / Beta

First public release candidate. Full feature set, assembled across the project's
development phases (see `ROADMAP.md` for phase-by-phase detail, `TODO.md` for
task-level history):

### Core
- Local, on-device usage monitoring (`UsageStatsManager` → Room), zero network,
  zero accounts.
- Deterministic rule engine: daily app limits, recurring schedules, an
  essential-app allow-list.
- App blocking via a `SYSTEM_ALERT_WINDOW` overlay enforced by a foreground
  monitoring service — no `AccessibilityService` dependency.
- Focus sessions (one-time/scheduled), with per-session notification "Quiet Mode"
  (Allow all / Silence all / Allow calls / Allow calls + selected apps) enforced
  through a single system-owned `AutomaticZenRule`.
- Room database, schema v5, with an explicit, tested migration for every version
  bump (`MIGRATION_1_2` through `MIGRATION_4_5`) — no destructive fallback.

### UI
- Black-first Compose UI: Home, Focus, Limits, Insights, Settings.
- Native app picker (real icons/labels, search, no raw package names shown) used
  everywhere an app selection is needed.
- 11-screen first-launch onboarding, reusing every existing
  permission/picker/notification-policy flow rather than duplicating any of them;
  existing-install upgrade path backfills onboarding completion so established
  users are never shown it retroactively.
- Insights: 7-day comparison and a 4-week breakdown view, both computed read-time
  from already-stored data.
- Block screen shows the real, specific reason a block fired (daily limit /
  schedule / active Focus session / block-list entry), plus Continue/snooze and a
  one-tap "Start Focus" action.
- Light/Dark/System theming.
- Local JSON export (user-initiated share sheet) and delete-all-local-data /
  reset-to-fresh-install controls.
- Privacy & Legal Center: 15 documents, reachable offline from Settings.

### Privacy & security posture
- No `INTERNET` permission anywhere — structurally zero network capability, not
  just a policy.
- No analytics, ads, tracking, crash-reporting, or AI/ML dependency of any kind.
- `allowBackup="false"` + empty cloud-backup/device-transfer data-extraction
  rules.
- Every sensitive permission (`PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`) is a
  manual, explained Settings grant — never auto-granted, never a runtime dialog
  trick — and every enforcement path fails safe (ALLOW, not BLOCK, no crash) if
  revoked.

### Release hardening (this candidate)
- Full regression pass: 233/233 unit tests, 33/33 real-device instrumentation
  tests, complete manual walkthrough of every screen/flow on a physical Pixel 7a.
- Fixed a real crash: Settings → "Delete all local data" (and the equivalent
  Privacy Center "Reset Orlune") ran `RoomDatabase.clearAllTables()` on the main
  thread, throwing `IllegalStateException` every time — found only by tapping the
  actual button on-device, not by any automated test. Fixed by moving the call to
  `Dispatchers.IO`.
- R8 minification + resource shrinking enabled for the release build type after a
  full on-device verification pass against a release-equivalent build showed no
  crashes or missing-class/resource failures — release APK ≈ 2.93 MiB, release AAB
  ≈ 4.00 MiB.

### Known gaps at this candidate (see `docs/PROJECT_STATE.md` "Remaining release
blockers" for the live list)
- No release signing key generated yet (deliberately — see
  `docs/RELEASE_PROCESS.md` Section 2).
- No privacy-policy URL hosted yet.
- Legal review of `docs/legal-compliance-matrix.md`'s open items not yet done.
- Play Store listing assets (screenshots, feature graphic, store copy) not yet
  created.
- Recurring focus-session scheduling remains out of scope (unchanged product
  decision, not a defect).
