# Orlune

A free, local-only, no-account, no-AI Android digital wellbeing app. Orlune helps people reduce distracting app usage, build focus habits, and understand their own screen time — entirely on-device, with zero network requests, zero analytics, and zero data leaving the phone.

Orlune is an original product inspired by the general problem space of tools like Opal, Android Digital Wellbeing, ScreenZen, one sec, and Freedom. It shares no code, design, branding, or algorithms with any of them.

## Project Status

**Phase 0: Research & Feasibility — complete and signed off.** Phase 1 (Architecture & Android Project Setup) is next; no application code exists yet. See `docs/phase-0-research.md` for the full architecture, feasibility analysis, MVP scope, and phased plan, and `TODO.md` for current status.

## Core Principles

- Free, no account, no login, no cloud, no AI, no analytics or ad SDKs.
- All data stored and processed on the device. Nothing is ever uploaded.
- Every algorithm is deterministic and documented — same input, same output, always.
- Every sensitive permission is explained in plain language before it's requested.

## Documentation

- `docs/phase-0-research.md` — product architecture, competitor research, MVP scope, phased plan
- `docs/android-platform-capabilities.md` — what the Android SDK can and can't do for usage monitoring and blocking
- `docs/accessibility-service-compliance.md` — AccessibilityService use, disclosure, and Play compliance
