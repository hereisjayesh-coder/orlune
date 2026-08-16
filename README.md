# Orlune

A free, local-only, no-account, no-AI Android digital wellbeing app. Orlune helps people reduce distracting app usage, build focus habits, and understand their own screen time — entirely on-device, with zero network requests, zero analytics, and zero data leaving the phone.

Orlune is an original product inspired by the general problem space of tools like Opal, Android Digital Wellbeing, ScreenZen, one sec, and Freedom. It shares no code, design, branding, or algorithms with any of them.

## Project Status

Phases 0–5 are complete and verified (research, project setup, Room database, usage
monitoring, deterministic rule engine, app blocking), and Phase 6 (focus sessions) is
implemented with a preserving Room migration — see `docs/PROJECT_STATE.md` for the current, dated snapshot
(build/test status, known bugs, next task) and `ROADMAP.md` for phase-by-phase detail.
`TODO.md` has full task-level history. The current Compose shell provides Home, Focus,
Limits, Insights, and Settings flows; remaining onboarding and release hardening are
tracked in `docs/PROJECT_STATE.md` and `ROADMAP.md`.

Agents picking up this repo: start with `AGENTS.MD`, then `docs/PROJECT_STATE.md`.

## Getting Started

1. Android Studio is installed at `F:\Android Stu`, SDK at `F:\Android\Sdk`. Open this folder in it.
2. Gradle caches live at `F:\GradleUserHome` (via the `GRADLE_USER_HOME` user environment variable) rather than the default `C:\Users\<you>\.gradle`, to keep the nearly-full system drive clear.
3. Build from the command line with:
   ```
   .\gradlew.bat assembleDebug
   ```
4. No emulator is configured (8 GB RAM machine) — use a physical device with USB debugging enabled. `adb devices` should list it once connected; run on it from Android Studio or `.\gradlew.bat installDebug`.

## Core Principles

- Free, no account, no login, no cloud, no AI, no analytics or ad SDKs.
- All data stored and processed on the device. Nothing is ever uploaded.
- Every algorithm is deterministic and documented — same input, same output, always.
- Every sensitive permission is explained in plain language before it's requested.

## Documentation

- `AGENTS.MD` — standing rules for any coding agent working on this repo: product identity, forbidden technologies, architecture, privacy requirements, build/test commands, conventions
- `docs/PROJECT_STATE.md` — dated snapshot: current phase, build/test status, known bugs, next recommended task
- `docs/phase-0-research.md` — product architecture, competitor research, MVP scope, phased plan
- `docs/android-platform-capabilities.md` — what the Android SDK can and can't do for usage monitoring and blocking
- `docs/accessibility-service-compliance.md` — AccessibilityService use, disclosure, and Play compliance
- `docs/dependency-audit.md` — every dependency in the project, with purpose/license/network/telemetry/privacy notes
- `ARCHITECTURE.md` — as-built package structure and verified build toolchain
- `ROADMAP.md` — phase-by-phase status
- `TODO.md` — task-level history
