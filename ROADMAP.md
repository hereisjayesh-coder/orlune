# Orlune — Roadmap

Status tracker for the phase plan defined in `docs/phase-0-research.md` Section 14. That document is the source of truth for what each phase means; this file just tracks where we actually are. Day-to-day task checkboxes live in `TODO.md`.

| Phase | Focus | Status |
|---|---|---|
| 0 | Research & feasibility | ✅ Complete, signed off 2026-08-16 |
| 1 | Architecture & Android project setup | ✅ Complete — project scaffolded, build environment fixed and verified, `assembleDebug` succeeds |
| 2 | Local database & domain models | ✅ Complete — all 16 Section 8 entities implemented as Room schema, verified build |
| 3 | Usage monitoring | ✅ Complete — pipeline built, unit-tested (15/15 pass), instrumentation-tested on a real device (3/3 pass), end-to-end data flow confirmed on-device |
| 4 | Deterministic rule engine | ✅ Complete — `LimitEngine`, `ScheduleEngine`, `BlockingEngine`, `GoalEngine` built pure/unit-tested (33/33 pass); `FrictionEngine` deferred (no config field exists, MVP defers the Friction blocking level) |
| 5 | App blocking | ✅ Complete — Usage-Access-based detection + `SYSTEM_ALERT_WINDOW` overlay, foreground-service enforcement loop, verified on a real device (limit rules, schedule rules, essential-app exemption, permission revocation/regrant, app restart, self-stop all confirmed) |
| 6 | Scheduling & focus sessions | Not started |
| 7 | Analytics & algorithms | Not started |
| 8 | Original UI & themes | Not started |
| 9 | Privacy Center & data controls | Not started |
| 10 | Security & performance | Not started |
| 11 | Testing | Not started |
| 12 | Google Play compliance | Not started |
| 13 | Release preparation | Not started |

## Phase 1 exit criteria (met)

- [x] Gradle/Kotlin/Compose project compiles (`.\gradlew.bat assembleDebug` — BUILD SUCCESSFUL, see `TODO.md` for the exact command and toolchain versions)
- [x] Module/package structure in place (see `ARCHITECTURE.md`)
- [x] No features implemented — bare Compose screen only

## Phase 2 exit criteria (met)

- [x] Room schema from Section 8 — all 16 entities, one DAO each, exported schema JSON committed
- [x] No migrations needed yet (schema version 1, nothing to migrate from)
- [x] No UI — nothing reads from or writes to the database yet

## Phase 3 exit criteria (met)

- [x] UsageStatsManager integration, permission flow
- [x] Raw -> aggregated pipeline, `App`/`DailyUsage`/`Session` populated
- [x] Edge cases (midnight rollover, timezone changes, reboot, orphaned events, duplicate processing) — see `TODO.md` for specifics, including two real bugs caught and fixed during this phase
- [x] Confirmed working against real Room/SQLite on an actual device — `.\gradlew.bat connectedDebugAndroidTest` run on a Pixel 7a, 3/3 pass; manual on-device run confirmed real usage data flows through to the UI

## Phase 4 exit criteria (met)

- [x] `LimitEngine`, `ScheduleEngine`, `BlockingEngine`, `GoalEngine` implemented under `core/domain/rules/`, pure Kotlin, fully unit-tested (33/33 pass) — see `TODO.md` for scoping rationale and per-engine notes
- [x] No enforcement wiring yet (no repository, no WorkManager worker, no UI) — reading real `Rule`/`Schedule`/`AppListEntry` rows and applying these engines to live data is Phase 5/6's job
- [x] No schema changes — existing Phase 2 entities already had everything these engines needed

## Phase 5 exit criteria (met)

- [x] Foreground app detected via `SessionDao.getOpenSessions()` (Phase 3's own session tracking), evaluated against `LimitEngine`/`ScheduleEngine`/`BlockingEngine` (Phase 4, unmodified), decision enforced by a `SYSTEM_ALERT_WINDOW` overlay drawn by a foreground `BlockingMonitorService` — the platform's documented mechanism for blocking without AccessibilityService
- [x] Daily limit rules and scheduled rules both implemented and verified on a real device; essential-app exemptions (allow-list) verified to override a triggered rule
- [x] Local-only: no `INTERNET` permission, no network dependency, no AccessibilityService dependency
- [x] Fails safe: permission revocation stops the service cleanly (no crash, overlay clears); malformed/incomplete rule data never blocks; Orlune's own package is never blockable; the service self-stops (no infinite loop) the moment no rules remain or Usage Access is revoked
- [x] Two real defects found via device testing and fixed before commit — see `TODO.md` for detail: (1) live foreground detection via a short `UsageStatsManager` polling window lost track of long-running sessions, replaced with the existing session-tracking data; (2) `WindowManager.addView`/`removeView` were called off the main thread, silently failing until diagnostic logging surfaced it

## Explicitly not started (by design, later phases)

Website blocking (VPN/`VpnService`), AccessibilityService (stays optional, never wired up), Friction/Strict Focus blocking levels, analytics, recommendation/wellbeing-score algorithms, monetization, accounts, cloud sync, AI/ML — none of these exist in the codebase yet. See `docs/phase-0-research.md` Section 13 (MVP scope) for what's in vs. deferred.
