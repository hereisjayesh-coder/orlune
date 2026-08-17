# Orlune — Project State

**Last verification date:** 2026-08-17 (app picker replacing raw package-name input;
launcher icon implemented from the approved reference; Privacy & Legal Center
expanded to full requested structure with new compliance docs)

**Latest verified commit:** working tree as of this file's date, on top of `8451fcb`
("feat: implement approved launcher icon from Logo.PNG reference, polish Privacy &
Legal Center") — verify with `git log`/`git status` before trusting this file's
claims if picking this up later; this file is a snapshot, not a substitute for
checking the real repository state.

This file is the living status snapshot. `AGENTS.MD` is the stable rules/conventions
file — read that first for *how* to work on this repo, this file for *where things
currently stand*. Update this file (not `AGENTS.MD`) after any verification pass or
significant change; keep `AGENTS.MD` stable unless a rule itself changes.

---

## Current phase

**Phase 6 is implemented and tested. Phase 8 (UI) and Phase 9 (Privacy Center) both
advanced significantly this session** — see `ROADMAP.md` for the phase table. The
biggest UI change since the last snapshot: Limits and Focus no longer accept raw
package-name text/CSV input anywhere — both use a native app picker
(`feature/apppicker/AppPickerScreen.kt`) with real app icons, real names, search, and
"Frequently used today" (from real Insights usage data). The Privacy & Legal Center
(Settings → Privacy & Legal) now has all 15 requested documents, with the Privacy
Policy and Terms of Service expanded to their full 23- and 28-section structure,
informed by this session's research into India's DPDP Act 2023/Rules 2025, GDPR/UK
GDPR, CCPA/CPRA, and COPPA (see `docs/legal-compliance-matrix.md`) and current Google
Play policy (see `docs/google-play-privacy-compliance.md`). The launcher icon is now
implemented from the user-approved reference image (`design/orlune-logo-reference.png`),
not the earlier placeholder clock mark. Onboarding remains the largest not-yet-started
piece of Phase 8.

## Build status — VERIFIED

Ran on 2026-08-17 in this environment (`JAVA_HOME=F:\Android Stu\jbr`,
`GRADLE_USER_HOME=F:\GradleUserHome`):

- `.\gradlew.bat assembleDebug --stacktrace` → **BUILD SUCCESSFUL**
- `.\gradlew.bat testDebugUnitTest --stacktrace` → **BUILD SUCCESSFUL**, **119/119**
  unit tests pass, 0 failures, 0 errors (summed from the real per-suite XML results
  in `app/build/test-results/`)
- `.\gradlew.bat connectedDebugAndroidTest --stacktrace` → **BUILD SUCCESSFUL**,
  **25/25** instrumentation tests pass, run against a real physical device (Pixel 7a,
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
| **Unit total** | | **119** | **119/119 pass** |
| Instrumentation | `OrluneDatabaseMigrationTest` | 1 | pass (real device) |
| Instrumentation | `BlockingRepositoryInstrumentedTest` | 8 | pass (real device) |
| Instrumentation | `UsageRepositoryInstrumentedTest` | 7 | pass (real device) |
| Instrumentation | `ThemePreferenceDaoInstrumentedTest` | 4 | pass (real device) |
| Instrumentation | `InstalledAppListerInstrumentedTest` | 5 | pass (real device) |
| **Instrumentation total** | | **25** | **25/25 pass** |
| **Grand total** | | **144** | **144/144 pass** |

## Physical-device status — VERIFIED (manual walkthrough, this session)

Pixel 7a connected over USB. Beyond the automated suites above, manually exercised:

- App picker end-to-end: search filtering, multi-select with per-app remove (Focus),
  single-select instant-return (Limits), and a full add-limit flow confirming the
  real app label (not a package name) appears in "Active rules"
- Launcher icon: app drawer (circular launcher mask, nothing clipped), App Info page
  (large size, still crisp)
- About Orlune, reachable directly from Settings root
- Privacy Center → Legal Center → a document, in both Dark and Light theme, including
  system back-button navigation at every level (not just in-app back arrows)
- A full `am force-stop` + relaunch: theme choice and all data survived
- Clean `logcat` throughout the final, fixed build — no `FATAL EXCEPTION`

One real crash was found and fixed during this session (not shipped): the first
version of `LimitsSection`/`FocusSection`/`SettingsSection` passed raw Kotlin
`data object`/`data class` values as `SaveableStateHolder.SaveableStateProvider`
keys, which crashed immediately on opening Focus (`IllegalArgumentException: Type of
the key Root is not supported` — the API requires Bundle-storable keys). Fixed by
using `.toString()` keys everywhere; re-verified crash-free afterward.

## Implemented features

- Usage monitoring, deterministic rule engines, app blocking, and focus sessions —
  unchanged this session; see prior snapshots / `TODO.md` for their history.
- **App picker** (`feature/apppicker/AppPickerScreen.kt`): real installed-app icons
  and labels via `platform/usage/InstalledAppLister.kt`, which enumerates launchable
  apps through the existing `<queries>`/`CATEGORY_LAUNCHER` manifest declaration —
  not `QUERY_ALL_PACKAGES` (see `docs/app-visibility-compliance.md` for the policy
  research behind that choice). Wired into Limits (single-select, matching
  `RuleEntity`'s one-package-per-rule design) and Focus (multi-select) via new
  `LimitsSection`/`FocusSection` wrapper composables that own each feature's picker
  sub-navigation, matching the `SettingsSection` pattern already established for the
  Privacy/Legal Center.
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
  dependency — re-verified this session via `git diff` showing zero changes to
  `AndroidManifest.xml`, `app/build.gradle.kts`, or `gradle/libs.versions.toml`.

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

## Next recommended task

Pick one, don't start both without checking in:

1. Onboarding flow — the largest remaining piece of Phase 8, and the natural place to
   introduce the permission requests and app picker for the first time.
2. Legal review of the Privacy Policy / Terms of Service against
   `docs/legal-compliance-matrix.md`'s open issues, plus resolving the actual business
   details (legal entity, address, contact) currently held as placeholders.

Do not start either without user sign-off.
