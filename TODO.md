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
- [x] Gradle/Kotlin/Compose project skeleton (`com.orlune.app`, minSdk 29, compileSdk 37, targetSdk 36)
- [x] AndroidManifest with no permissions (matches zero-network privacy architecture, Section 10)
- [x] Bare Compose `MainActivity` showing "Orlune" — proves the toolchain wires up, no real UI yet
- [x] Package structure scaffolded (`core/`, `data/`, `feature/`, `ui/`, `platform/`) — see `ARCHITECTURE.md`
- [x] `docs/dependency-audit.md` created and current
- [x] **Build verified.** Root cause of the original failure: `gradle-wrapper.properties` pointed at `gradle-9.2-bin.zip`, a version that was never released (HTTP 404) — AGP 9.2.0 actually requires Gradle ≥ 9.4.1. Fixed by pointing the wrapper at Gradle 9.7.0 (current stable) and committing `gradlew`/`gradlew.bat`/`gradle-wrapper.jar` (fetched from the official Gradle GitHub release tag, since no local Gradle install existed to generate them).
- [x] Second build failure fixed: the `org.jetbrains.kotlin.android` plugin is no longer compatible with AGP 9.0+ (Kotlin support is now built in) — removed it from `build.gradle.kts`, `app/build.gradle.kts`, and `gradle/libs.versions.toml`; `kotlinOptions { jvmTarget }` removed too since AGP 9 infers it from `compileOptions.targetCompatibility` automatically.
- [x] `GRADLE_USER_HOME` set to `F:\GradleUserHome` (user env var) so Gradle's distribution/dependency caches don't land on the nearly-full `C:` drive.
- [x] Verified: no `INTERNET` permission or networking dependency anywhere (source manifest, merged manifest, and `app/build.gradle.kts` all checked)
- [x] **Exact verified build command:** `.\gradlew.bat assembleDebug --stacktrace` → `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`
- [x] `adb devices` confirmed working (no physical device connected yet — connect one with USB debugging enabled to install/run)
- [ ] One pre-existing, non-blocking issue: a Gradle-10-incompatibility deprecation warning originates from inside AGP 9.2.0 itself (not from this project's build files) — nothing to fix here, just something to recheck when AGP is next upgraded.

## Phase 2 — Local Database & Domain Models
- [ ] Not started — Phase 1 is done, blocked only on explicit go-ahead to begin Phase 2
