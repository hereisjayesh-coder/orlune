# Orlune — Roadmap

Status tracker for the phase plan defined in `docs/phase-0-research.md` Section 14. That document is the source of truth for what each phase means; this file just tracks where we actually are. Day-to-day task checkboxes live in `TODO.md`.

| Phase | Focus | Status |
|---|---|---|
| 0 | Research & feasibility | ✅ Complete, signed off 2026-08-16 |
| 1 | Architecture & Android project setup | ✅ Complete — project scaffolded, build environment fixed and verified, `assembleDebug` succeeds |
| 2 | Local database & domain models | ✅ Complete — all 16 Section 8 entities implemented as Room schema, verified build |
| 3 | Usage monitoring | ✅ Complete — pipeline built, unit-tested (15/15 pass), instrumentation-tested on a real device (3/3 pass), end-to-end data flow confirmed on-device |
| 4 | Deterministic rule engine | Not started |
| 5 | App blocking | Not started |
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

## Explicitly not started (by design, later phases)

App blocking, AccessibilityService, VPN/website blocking, analytics, recommendation/wellbeing-score algorithms, monetization, accounts, cloud sync, AI/ML — none of these exist in the codebase yet. See `docs/phase-0-research.md` Section 13 (MVP scope) for what's in vs. deferred.
