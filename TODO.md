# Orlune — TODO

## Phase 0 — Research & Feasibility
- [x] Competitor research (Opal, Digital Wellbeing, ScreenZen, one sec, Freedom, StayFree)
- [x] Android platform capability research → `docs/android-platform-capabilities.md`
- [x] AccessibilityService feasibility & compliance research → `docs/accessibility-service-compliance.md`
- [x] Google Play compliance research
- [x] Consolidated Phase 0 deliverable → `docs/phase-0-research.md`
- [x] Naming-conflict check on original name "Grove" (flagged: collision with "Focus Grove" → renamed to "Orlune")
- [x] Naming-conflict check on "Orlune" (no category collision found)
- [x] User decision on open questions in `docs/phase-0-research.md` Section 17 — **resolved 2026-08-16**

## Phase 1 — Architecture & Android Project Setup
- [x] Gradle/Kotlin/Compose project skeleton (`com.orlune.app`, minSdk 29, compileSdk/targetSdk 36)
- [x] AndroidManifest with no permissions (matches zero-network privacy architecture, Section 10)
- [x] Bare Compose `MainActivity` showing "Orlune" — proves the toolchain wires up, no real UI yet
- [ ] **Unverified: not yet built.** No JDK/Android SDK/Gradle on the machine that scaffolded this — needs to be opened in Android Studio and synced/run before Phase 1 is actually done. See README "Getting Started".
- [ ] `gradlew`/`gradlew.bat` + wrapper jar not committed — Android Studio generates these on first open
- [ ] Confirm exit criteria once built: project compiles, module structure in place, no features yet

## Phase 2 — Local Database & Domain Models
- [ ] Not started — blocked on Phase 1 build verification
