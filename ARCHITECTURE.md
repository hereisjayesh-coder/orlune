# Orlune — Architecture

This is the as-built companion to `docs/phase-0-research.md` Section 1 (the original architecture diagram) and Section 8/9 (data model and algorithm layer, not yet implemented). It records real decisions made while scaffolding the project, not aspirational design.

## Module structure: single Gradle module, package-layered

The project is one Gradle module (`:app`), not a multi-module build. Phase 0's tech-stack decision (Section 2) was deliberately minimal — "Manual DI initially... avoid a dependency until the object graph actually justifies it" — and a multi-module Gradle setup is the same kind of premature complexity at this stage: nothing in Phase 1 needs module-level build isolation. The architecture's layers (platform, data, core, feature, ui) exist as **Kotlin packages** under `com.orlune.app`, not as separate Gradle modules. This can change later if a real need shows up (e.g. build-time isolation, a genuinely reusable layer) — it's not a permanent constraint.

```
com.orlune.app/
├── platform/
│   └── usage/          UsageStatsManager integration (Phase 3)
├── data/
│   ├── local/           Room database, DAOs (Phase 2)
│   └── repository/      Repository implementations bridging data ↔ domain (Phase 2+)
├── core/
│   ├── domain/          Business models, use-cases (Phase 2+)
│   ├── database/        Cross-cutting DB utilities (migrations, converters) (Phase 2)
│   ├── privacy/         Privacy Center logic — permission status, export/delete (Phase 9)
│   └── security/        Threat-model-driven safeguards (Phase 10)
├── feature/
│   ├── onboarding/       Welcome, privacy promise, Usage Access setup (MVP)
│   ├── home/             Today's usage, active rules, focus/block status (MVP)
│   ├── settings/         App settings
│   └── privacy/          Privacy Center UI (Phase 9)
└── ui/
    ├── theme/            Compose theme (exists — placeholder Material 3 baseline, real Light/Dark/Forest theming is Phase 8)
    ├── components/        Shared Compose components
    └── navigation/        Navigation graph
```

All of the above except `ui/theme` and the root `MainActivity.kt` are currently **empty packages** (a `.gitkeep` only) — this is Phase 1 structure, not Phase 1 features. Each fills in during the phase that owns it, per `docs/phase-0-research.md` Section 14.

Testing uses Android's standard source sets — `app/src/test` (JVM unit tests) and `app/src/androidTest` (instrumented tests) — rather than a top-level `tests/` folder, since that's what Gradle's Android plugin actually wires up test tasks against.

## Build toolchain (Phase 1, verified 2026-08-16)

| Component | Version | Notes |
|---|---|---|
| JDK | 25.0.2 (JetBrains Runtime, bundled with Android Studio) | Runs the Gradle daemon; Android/Kotlin bytecode still targets JVM 17 (see below) |
| Gradle | 9.7.0, via committed wrapper | `gradle-wrapper.jar`/`gradlew`/`gradlew.bat` are committed; never invoke a system-wide Gradle |
| Android Gradle Plugin | 9.2.0 | Kotlin support is built in as of AGP 9.0 — no `org.jetbrains.kotlin.android` plugin applied |
| Kotlin (compiler) | 2.3.21 | Pinned via the `org.jetbrains.kotlin.plugin.compose` plugin version, since the standalone Kotlin plugin isn't applied |
| Jetpack Compose | BOM 2026.08.00 | Individual artifact versions resolved via the BOM |
| compileSdk | 37 (Android 17) | Matches what's installed on the SDK — avoids an extra platform download |
| targetSdk | 36 (Android 16) | Deliberately one behind compileSdk; Android 17/API 37 changes default behavior (e.g. orientation-lock handling) that hasn't been reviewed yet |
| minSdk | 29 (Android 10) | Per Phase 0 Section 17 decision |
| Build Tools | 36.0.0 | Pinned explicitly; matches what's already installed and AGP 9.2/9.3's own documented default for compileSdk 37 |

**Why JDK 25 + bytecode target 17 works:** AGP 9's built-in Kotlin support infers the Kotlin compiler's `jvmTarget` from `android.compileOptions.targetCompatibility` (set to Java 17) automatically. This produces JVM-17-compatible bytecode for D8/R8 to dex, without requiring a separate installed JDK 17 toolchain (`kotlin { jvmToolchain(17) }` would force Gradle to resolve/download an exact JDK 17, which isn't needed here and was deliberately avoided given limited disk space).

## Privacy architecture (enforced, not just documented)

- `AndroidManifest.xml` declares zero `<uses-permission>` entries. No `INTERNET` permission exists anywhere in the manifest (source or merged build output) — verified by inspecting `app/build/intermediates/merged_manifest/**/AndroidManifest.xml` after every build that touches the manifest.
- `android:allowBackup="false"` and `xml/data_extraction_rules.xml` (empty `<cloud-backup/>` and `<device-transfer/>` rules) block both cloud backup and device-to-device transfer of app data.
- No networking, analytics, or ad dependency exists in `app/build.gradle.kts` — see `docs/dependency-audit.md` for the full, currently-empty-of-networking dependency list.

## Gradle environment notes (machine-specific, not project policy)

- `GRADLE_USER_HOME` is set to `F:\GradleUserHome` (user-level environment variable) so Gradle's distribution and dependency caches land on the drive with free space, not the nearly-full `C:`.
- The Android SDK lives at `F:\Android\Sdk` (see `local.properties`, which is gitignored — machine-specific).
- Android Studio is installed at `F:\Android Stu`.
