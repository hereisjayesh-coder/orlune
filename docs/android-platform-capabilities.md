# Android Platform Capabilities — Usage Monitoring & App Blocking

**Status:** Phase 0 research draft. Informs architecture decisions; not a final implementation spec.
**Scope:** What the public Android SDK can and cannot do for on-device, no-cloud usage monitoring and app blocking, as of Android 13–17 (2026), based on official developer.android.com and Google Play policy documentation.

This document exists so no later phase invents capabilities the platform doesn't actually provide. Where something is not possible with public APIs, it is called out explicitly rather than glossed over.

---

## Capability Matrix

| # | API / Mechanism | Can Do | Cannot Do / Limits | Permission(s) | User Action Required | Background / Doze Behavior | Battery Impact | Complexity | Play Policy Notes |
|---|---|---|---|---|---|---|---|---|---|
| 1 | **UsageStatsManager** (`queryEvents`, `queryUsageStats`) | Detect current foreground app via `MOVE_TO_FOREGROUND`/`MOVE_TO_BACKGROUND` events; aggregate usage totals per interval | Not push-based — must be polled (typically every few seconds from a foreground service); event log is truncated for the last few minutes; historical fine-grained data has a limited retention window | `PACKAGE_USAGE_STATS` (special) | User manually grants "Usage Access" in Settings — cannot be requested via runtime dialog | Polling loop needs a foreground service to survive Doze/App Standby | Moderate–high if polled tightly | Low–Med | Must be justified in Play's Data Safety/permissions declarations |
| 2 | **PACKAGE_USAGE_STATS** ("Usage Access") | Gates #1 | Not grantable programmatically | Special app access | Manual Settings toggle | N/A | N/A | Low | Sensitive permission; scrutinized at review |
| 3 | **AccessibilityService** | Near-real-time foreground-app detection via `TYPE_WINDOW_STATE_CHANGED`; combined with an overlay, can intercept app launches | Cannot be used to read screen content or act autonomously for a non-accessibility-tool app under current Play policy; "monitoring apps" explicitly do not qualify as accessibility tools | `BIND_ACCESSIBILITY_SERVICE` (special) | Manual enable in Settings → Accessibility; Android 13+ blocks sideloaded APKs via "Restricted Settings" until the user explicitly allows it (Play-installed apps are exempt) | Persistent bound service, largely Doze-immune once enabled | Low–Med (event-driven) | Med | Requires a Play Console accessibility declaration + demonstration video; must justify deterministic, rule-based use only — see `accessibility-service-compliance.md` |
| 4 | **ActivityManager.getRunningAppProcesses()** | — | Since Android 5.0, returns only the calling app's own process; unusable for foreground-app detection | Normal | None | N/A | N/A | N/A | Effectively dead API for this use case |
| 5 | **AppOpsManager** | Track op-level access for the app's own ops | Not a general cross-app monitoring tool without system/signature privilege | Varies, mostly restricted | N/A | N/A | Low | Low | Not directly relevant |
| 6 | **AlarmManager / SCHEDULE_EXACT_ALARM** | Schedule exact wake-ups to re-check/re-enforce rules | Denied by default for new installs targeting Android 13+/on Android 14+ devices; Doze limits idle-window alarms to roughly once per 9 minutes even when granted | `SCHEDULE_EXACT_ALARM` (special "Alarms & reminders") | Manual Settings toggle; check via `canScheduleExactAlarms()` | Deferred under Doze unless using `setExactAndAllowWhileIdle()` | Low–Med | Low | A blocking app is not a calendar/alarm app, so it does **not** qualify for the no-permission `USE_EXACT_ALARM` carve-out — must request and justify the special permission |
| 7 | **WorkManager** | Reliable, constraint-aware periodic work (rule refresh, cleanup, aggregation) | Minimum periodic interval is 15 minutes (hard clamp); exact timing not guaranteed under Doze/App Standby | Normal | None | Subject to Doze/App Standby deferral | Low | Low | Preferred over raw AlarmManager for non-time-critical work |
| 8 | **Foreground Services** | Keep the monitoring/enforcement process alive with a persistent notification | Android 14+ requires a declared FGS type; a blocking service best fits `specialUse`, which requires declaring `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` and is manually reviewed at submission; Android 15+ restricts starting certain FGS types from `BOOT_COMPLETED` | `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Persistent notification always visible | Reduces but does not eliminate Doze/App Standby throttling | Med–High | Med | `specialUse` justification is manually reviewed by Google |
| 9 | **DevicePolicyManager** | Full app suspension, device lockdown, cross-profile controls — but only as Device Owner / Profile Owner | Not attainable by a normal consumer install without a deliberate enrollment flow (factory-reset-time QR/NFC provisioning); not viable as the default install path | Device/Profile Owner status | Deliberate enrollment, not a runtime permission | N/A | N/A | High | Enterprise/parental-control territory, not default consumer flow |
| 10 | **VpnService (local only)** | Establish a local TUN interface; `addDisallowedApplication()`/`addAllowedApplication()` to cut network access per app; basic DNS-layer domain blocking | IP/packet-level only — encrypted DNS (DoH/DoT) and IP-shared CDNs can bypass simple blocklists; only one active VPN per profile (conflicts with any other VPN app); requires a persistent, non-dismissible notification | `BIND_VPN_SERVICE` + one-time system confirmation dialog | User must accept the system VPN dialog | Runs persistently while connected, largely Doze-exempt | Med (packet processing) | High | Prevents concurrent use of a real VPN — genuine user friction; must be declared explicitly as local-only, no external routing |
| 11 | **Package visibility / `<queries>`** | List installed apps for the blocklist UI via scoped `<queries>` declarations | Broad "see all apps" needs `QUERY_ALL_PACKAGES`, Play-gated and restricted to a narrow allowed-category list that a distraction-blocker does not clearly fit | `QUERY_ALL_PACKAGES` (special, Play-gated) or scoped `<queries>` | None for scoped queries | N/A | N/A | Low | Prefer a generic `ACTION_MAIN`/`CATEGORY_LAUNCHER` `<queries>` filter (explicitly permitted for launcher-like use cases) over requesting `QUERY_ALL_PACKAGES` |
| 12 | **SYSTEM_ALERT_WINDOW** ("draw over other apps") | Full-screen blocking overlay when a restricted app is foregrounded | Android 12+ lets underlying apps opt out via `HIDE_OVERLAY_WINDOWS`; overlay drawing is a race against detection latency — a brief flash of the blocked app before the overlay appears is possible | `SYSTEM_ALERT_WINDOW` (special) | Manual Settings toggle | N/A | Low | Med | Historically malware-adjacent; Play scrutinizes purpose closely |
| 13 | **Battery optimization exemption / Doze / OEM kill lists** | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` exempts the app from Doze/App Standby deferral | Play restricts requesting this to a narrow acceptable-use list; a "convenience" framing risks rejection. Independent of standard Doze, OEM skins (MIUI, One UI, EMUI, OxygenOS, ColorOS, FuntouchOS) run proprietary background-kill logic that can force-stop the app regardless of the standard exemption | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (special) | Manual Settings toggle or system dialog | Even exempted apps face App Hibernation auto-revoke of special permissions after months of inactivity (Android 11+) | Low (by design) | High practical OEM risk | Must frame the request narrowly around core blocking reliability, and separately guide users through each OEM's undocumented autostart/battery settings |

---

## What Is NOT Possible With Current Public APIs

> **NOT POSSIBLE WITH CURRENT PUBLIC API:** True universal website/content blocking without a VPN. There is no non-VPN, non-root API to inspect or block arbitrary URLs/HTTPS content system-wide. `VpnService` is IP/packet-level only; encrypted DNS (DoH/DoT) can bypass simple DNS-based blocklists. **Closest compliant alternative:** local `VpnService`-based DNS/IP filtering for known distracting domains, disclosed honestly as best-effort, not absolute.

> **NOT POSSIBLE WITH CURRENT PUBLIC API:** Blocking that survives a determined uninstall, permission revocation, or Accessibility/Usage Access disable. Without Device Owner/Profile Owner enrollment, a consumer app cannot prevent the user from disabling the service or removing the app. **Closest compliant alternative:** make disabling/uninstalling deliberately effortful (confirmation friction, "why are you leaving" reflection screen) without ever technically trapping the user — see Section 16/17 of the product brief (Blocking Model, Emergency Override).

> **NOT POSSIBLE WITH CURRENT PUBLIC API:** Silent or automatic granting of Usage Access, Accessibility, Overlay, or VPN permissions. All are "special app access," requiring an explicit manual Settings toggle or system dialog — never a manifest-only or single runtime-dialog grant.

> **NOT POSSIBLE WITH CURRENT PUBLIC API:** Guaranteed always-on background monitoring with zero battery/OEM risk. Doze, App Standby, App Hibernation auto-revoke, and non-standard OEM background-kill systems mean persistent monitoring cannot be guaranteed reliable across all devices using only documented Android APIs. OEM-specific workaround guidance is community-sourced (dontkillmyapp.com), not Google-documented.

> **NOT POSSIBLE WITH CURRENT PUBLIC API:** Reading in-app content via `AccessibilityService` for an app not declared as an accessibility tool. Current Play policy explicitly prohibits autonomous content-reading/action for non-a11y-tool apps. **Closest compliant alternative:** use `AccessibilityService` (if used at all) strictly for `TYPE_WINDOW_STATE_CHANGED` foreground-package detection, nothing else.

> **NOT POSSIBLE WITH CURRENT PUBLIC API:** A truly push-based, zero-polling, zero-persistent-process foreground-app detector. `UsageStatsManager` requires polling; `AccessibilityService` is event-driven but still requires a persistent bound service.

---

## Architectural Recommendation

- **Primary detection mechanism:** `UsageStatsManager`, polled from a `WorkManager`/foreground-service loop at a battery-conscious interval (target: 2–5s while a focus/block session is active, otherwise idle). Lower Play scrutiny, no accessibility declaration/video overhead, standard for parental-control-style apps.
- **AccessibilityService:** offered as an **optional, clearly-disclosed upgrade** for users who want lower-latency blocking (near-instant overlay vs. a few seconds of lag), not the default path. This keeps the MVP's Play review surface smaller and gives a graceful fallback if the Accessibility declaration is ever rejected or Android further restricts the API (see `accessibility-service-compliance.md`).
- **Website blocking:** local `VpnService` DNS-based filtering, explicitly disclosed as best-effort (bypassable by disabling the VPN, encrypted DNS, or browser-specific quirks) — never marketed as complete or unbreakable.
- **App list enumeration:** scoped `<queries>` with `CATEGORY_LAUNCHER` intent filter, avoiding `QUERY_ALL_PACKAGES` entirely if feasible for the blocklist UI.

---

## Sources

- [UsageStatsManager reference](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Schedule exact alarms denied by default (Android 14 changes)](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [Play policy: Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491)
- [VPN connectivity guide](https://developer.android.com/develop/connectivity/vpn)
- [Package visibility](https://developer.android.com/training/package-visibility)
- [QUERY_ALL_PACKAGES policy](https://support.google.com/googleplay/android-developer/answer/10158779)
- [Android 15 behavior changes: all apps](https://developer.android.com/about/versions/15/behavior-changes-all)
- [App hibernation](https://developer.android.com/topic/performance/app-hibernation)
- [ActivityManager.RunningAppProcessInfo reference](https://developer.android.com/reference/android/app/ActivityManager.RunningAppProcessInfo)
- [dontkillmyapp.com — OEM background-kill documentation](https://dontkillmyapp.com/problem)
- [WorkManager periodic work interval](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Android Enterprise work profiles](https://developer.android.com/work/managed-profiles)
