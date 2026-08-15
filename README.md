# Orlune

A free, local-only, no-account, no-AI Android digital wellbeing app. Orlune helps people reduce distracting app usage, build focus habits, and understand their own screen time — entirely on-device, with zero network requests, zero analytics, and zero data leaving the phone.

Orlune is an original product inspired by the general problem space of tools like Opal, Android Digital Wellbeing, ScreenZen, one sec, and Freedom. It shares no code, design, branding, or algorithms with any of them.

## Project Status

**Phase 1: Architecture & Android Project Setup — scaffolded, unverified.** The Android project skeleton exists (Gradle/Kotlin/Compose setup, `com.orlune.app`, minSdk 29, no features yet) but has not been opened or built on any machine with a JDK/Android SDK — see "Getting Started" below before trusting that it compiles. See `docs/phase-0-research.md` for the full architecture, feasibility analysis, MVP scope, and phased plan, and `TODO.md` for current status.

## Getting Started

This machine has no JDK, Android SDK, or Gradle installed, so the project skeleton below has only been hand-written against current (August 2026) tool versions — it has never actually been built.

1. Open this folder in Android Studio (Narwhal or newer).
2. If prompted that the Gradle wrapper is missing, let Android Studio generate it (`gradlew`/`gradlew.bat` and the wrapper jar aren't committed — only `gradle/wrapper/gradle-wrapper.properties`, which pins Gradle 9.2).
3. Let Gradle sync. If AGP 9.2.0 / Kotlin 2.3.21 / Compose BOM 2026.08.00 need bumping by the time you open this, Android Studio will prompt for it.
4. Run on a device/emulator — you should see a bare screen with the text "Orlune". That's the full scope of Phase 1; no features are implemented yet.

## Core Principles

- Free, no account, no login, no cloud, no AI, no analytics or ad SDKs.
- All data stored and processed on the device. Nothing is ever uploaded.
- Every algorithm is deterministic and documented — same input, same output, always.
- Every sensitive permission is explained in plain language before it's requested.

## Documentation

- `docs/phase-0-research.md` — product architecture, competitor research, MVP scope, phased plan
- `docs/android-platform-capabilities.md` — what the Android SDK can and can't do for usage monitoring and blocking
- `docs/accessibility-service-compliance.md` — AccessibilityService use, disclosure, and Play compliance
