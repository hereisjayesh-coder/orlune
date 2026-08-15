# AccessibilityService Compliance & Disclosure

**Status:** Phase 0 research draft. Governs whether/how Project Evergreen uses `AccessibilityService`. Must be re-validated against Google Play policy immediately before submission, as this is the single fastest-moving policy area affecting this app.

---

## 1. Purpose

Project Evergreen's core function requires detecting which app is currently in the foreground so a user-configured block/friction rule can be enforced. Two Android mechanisms can provide this: `UsageStatsManager` (polling) and `AccessibilityService` (event-driven, lower latency). Project Evergreen treats `AccessibilityService` as an **optional, opt-in enhancement**, not a requirement — see `android-platform-capabilities.md` for the reasoning.

If enabled, `AccessibilityService` is used **only** to:
1. Receive `TYPE_WINDOW_STATE_CHANGED` events to identify the current foreground package.
2. Trigger a deterministic, pre-configured local action (show blocking overlay, log a block event) when that package matches a user-defined rule.

## 2. Technical Use — Precisely Scoped

The `AccessibilityServiceInfo` configuration must be scoped as narrowly as the API allows:

- `eventTypes`: `TYPE_WINDOW_STATE_CHANGED` only (not `TYPE_VIEW_*`, not `TYPE_NOTIFICATION_STATE_CHANGED` unless a future, separately-disclosed notification feature needs it).
- `flags`: no `FLAG_RETRIEVE_INTERACTIVE_WINDOWS` or content-capture flags — Project Evergreen does not need and must not request the ability to read on-screen text, form fields, or view hierarchies.
- No use of `dispatchGesture()` or any simulated-input capability.
- The service must not read, log, or transmit the content of any other app's UI. It only ever reads a package name and a timestamp.

## 3. Data Access — What the API *Could* Technically Access vs. What Project Evergreen Actually Uses

| Technically accessible via a broadly-configured AccessibilityService | Does Project Evergreen use it? |
|---|---|
| Foreground package name / window state changes | **Yes** — this is the entire purpose |
| Full view hierarchy / on-screen text content | **No** |
| Typed input / form field values | **No** |
| Clipboard contents | **No** |
| Notification content | **No** (unless a future, separately-disclosed feature is built and re-reviewed) |
| Ability to simulate taps/gestures on other apps | **No** |

This table itself should be reproduced, in plain language, in the in-app disclosure screen (Section 4) and the Play Console accessibility declaration (Section 6), so the gap between "what the API can do" and "what we do" is never ambiguous to a reviewer or a user.

## 4. In-App Disclosure — Draft Text

Shown as its own screen, before the system Settings screen is opened, not buried in a permissions list. Requires an affirmative tap, not a system-dialog-style "Allow."

> **Optional: Faster blocking with Accessibility**
>
> Project Evergreen can block distracting apps two ways:
>
> - **Standard (default):** Project Evergreen checks which app is open every few seconds using Android's Usage Access permission. Blocking may lag by a few seconds.
> - **Instant (optional):** Project Evergreen can detect app switches immediately using Android's Accessibility service. This only tells Project Evergreen *which app just opened* — it cannot read anything on your screen, see what you type, or access your notifications or clipboard.
>
> If you turn this on, Android will show you a system screen describing what Accessibility services can technically do in general. Project Evergreen only uses the part described above. You can turn this off at any time in Project Evergreen's Settings or in Android Settings → Accessibility, and Project Evergreen will fall back to Standard mode automatically.
>
> [Not now] [Enable Instant Blocking →]

If the user proceeds, they are sent to the system Accessibility settings screen to complete the OS-level grant themselves — Project Evergreen cannot do this on their behalf.

## 5. Consent Flow

1. User opts into "Instant Blocking" from Project Evergreen's onboarding or Settings (never pre-selected/default-on).
2. Disclosure screen (Section 4) shown; requires explicit tap to proceed.
3. Android system Accessibility settings screen opens; user manually enables Project Evergreen's service.
4. On return to Project Evergreen, the app re-checks service status and confirms activation state to the user (success or "not enabled yet" state — never assumes).
5. Project Evergreen's Privacy Center (see product architecture) always shows current Accessibility status live, with a one-tap path to disable it.
6. Disabling produces no data loss and no degraded core functionality beyond the latency difference — Project Evergreen falls back to Usage Access polling automatically and tells the user this happened.

## 6. Play Console Declaration Requirements

Per Google's Accessibility API policy (support.google.com/googleplay/android-developer/answer/10964491) and the October 2025 policy update (enforcement effective on/around Jan 28, 2026):

- Project Evergreen is **not** an accessibility tool (its primary purpose is not assisting users with disabilities) — it must declare under the **non-accessibility use** path, not claim `isAccessibilityTool="true"`.
- The core policy language to satisfy: *"Any use of the Accessibility API that enables an app to autonomously initiate, plan, and execute actions or decisions is strictly prohibited,"* with an explicit exception for *"deterministic, rule-based automation, where behavior follows a static, human-defined script."* Project Evergreen's use — a fixed, user-configured rule ("if package X is foregrounded during window Y, show overlay") — is intended to sit inside that exception. This is Project Evergreen's central compliance argument and must be stated identically in the Play Console declaration and the in-app disclosure so they can't be read as contradicting each other.
- Google requires a **declaration form submission plus a screen-recording video** demonstrating the actual in-app disclosure and consent flow as shipped. The video must match the shipped build exactly — mismatch between declared and actual behavior is a top rejection cause.
- Reviewers increasingly ask "why not `UsageStatsManager`?" — the declaration should proactively state that Usage Access is the default and Accessibility is an opt-in latency improvement, which strengthens the necessity argument.

## 7. Risks

- **Policy risk:** This is the fastest-moving policy area in the entire project. Google has run multiple enforcement waves against monitoring-style apps using Accessibility. A rejected declaration, or a later policy tightening, could force Accessibility support out of the app entirely.
- **Platform risk:** Android 17's Advanced Protection Mode reportedly restricts the Accessibility API for non-accessibility-tool apps at the OS level on devices where the mode is enabled, independent of Play's own review — meaning even an approved app could lose functionality on some devices.
- **Reputational risk:** Users are increasingly wary of any app requesting Accessibility, having seen news coverage of malware abusing it. The disclosure must be unusually clear to avoid triggering that suspicion.

## 8. Alternatives

- **Default and recommended:** `UsageStatsManager` polling (see `android-platform-capabilities.md`, row 1). No accessibility declaration, no video, no Advanced Protection Mode exposure, standard mechanism for parental-control-category apps.
- Accessibility is additive, never load-bearing for core functionality.

## 9. Fallback When Disabled or Rejected

Project Evergreen must be designed so that **Accessibility is never a hard dependency**:

- If the user disables it mid-use: silently and immediately fall back to Usage Access polling; show a one-time non-alarming notice ("Switched to Standard blocking speed").
- If Google rejects the Accessibility declaration at submission: ship Project Evergreen with Usage-Access-only blocking and omit the Accessibility feature from that release, rather than blocking the whole release on an appeal.
- If a future Android version restricts the API further for non-a11y-tool apps: Project Evergreen continues to function on Usage Access alone with no user-facing functionality loss beyond latency.

---

## Sources

- [Use of the AccessibilityService API — Play policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Prominent disclosure & consent requirements](https://support.google.com/googleplay/android-developer/answer/11150561)
- [Permissions Declaration Form](https://support.google.com/googleplay/android-developer/answer/9214102)
- [Permissions and APIs that access sensitive information](https://support.google.com/googleplay/android-developer/answer/9888170)
- Policy update announcement (Oct 30, 2025), enforcement ~Jan 28, 2026 — reinforced "deterministic, rule-based automation" exception language.
- Reporting on Android 17 Advanced Protection Mode restricting non-accessibility-tool use of the API (March 2026 coverage).
