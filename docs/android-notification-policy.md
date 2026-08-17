# Android Notification Policy — Focus / Quiet Mode

**Status:** Implemented and device-verified (Phase 8). Documents the real API behavior
this feature relies on, confirmed against this project's actual compileSdk 37
`android.jar` (via `javap`/decompiled stub source) and a real Pixel 7a running
Android 17 (API 37) — not just written from memory of older documentation. Two real
bugs were found and fixed only by testing on real hardware; both are recorded below
so a future change doesn't reintroduce them.

---

## What Orlune uses, and why

**`AutomaticZenRule` + `NotificationManager.setAutomaticZenRuleState`** — added API 24
(rule registration) and API 29 (direct app-driven state toggling), this is the
platform's own mechanism for a "named Focus/quiet mode" a single app owns and
controls, without becoming a full `NotificationListenerService` or
`ConditionProviderService`. Orlune registers exactly one such rule, named
"Orlune Focus", and only ever flips its `Condition` between `STATE_TRUE` (a
restricting session is active) and `STATE_FALSE` (none is). It never calls the older,
blunter `NotificationManager.setInterruptionFilter()` / `setNotificationPolicy()`
APIs, which directly overwrite the device's single global DND state.

**Why that distinction matters — "most restrictive wins" is not something Orlune
implements, it's what Android already does.** Since Android 9/Q's Zen Mode redesign,
the system computes the *effective* interruption filter as the most restrictive
combination of every currently-active rule: the user's own manual DND toggle, Bedtime,
Driving, other apps' rules, and Orlune's rule, all independently on/off. Orlune's rule
is just one more independently-toggleable input to that computation — it is never the
sole owner of "is DND on right now." This is why:

- **Orlune never needs to save or restore anyone else's state.** There is nothing to
  restore. Ending Focus means: turn *Orlune's own* rule off. Whatever the user's
  manual DND setting, Bedtime, or another app's rule already was, is completely
  untouched and keeps doing whatever it was already doing — because Orlune's code
  never read or touched it in the first place.
- **The user manually changing Android DND settings during Focus** is not a case
  Orlune has to detect or react to. Their manual toggle and Orlune's rule are two
  separate inputs the system combines on its own; Orlune's rule keeps doing exactly
  what it was told regardless.
- This is the deliberate answer to "do not blindly restore a hard-coded 'all
  notifications' state" — the correct fix isn't a smarter restore, it's to never be in
  a position where a global overwrite was needed at all.

**Overlapping Orlune sessions** are resolved the same way, one level down: if two
Focus sessions are simultaneously `ACTIVE` with different notification policies,
`core/domain/focus/FocusNotificationPolicy.kt`'s `effectiveFocusNotificationState`
picks the single most restrictive of *Orlune's own* active sessions (by
`FocusNotificationPolicy.restrictiveness`) and applies only that one policy to the one
rule Orlune owns — mirroring how `FocusSessionEngine`/`BlockingRepository` already
union `blockedPackages` across overlapping sessions.

**`NotificationListenerService` is not used and was never necessary.** It grants
access to read notification *content*, which this feature has no reason to touch —
Orlune only ever changes whether the *system* lets a notification interrupt (sound,
vibration, heads-up), never reads what any notification says. `AutomaticZenRule` alone
covers 100% of this feature's requirements without that broader, more sensitive
permission.

---

## Permission: "Notification Policy Access" (`isNotificationPolicyAccessGranted`)

Special app access, same manual-Settings-grant shape as Usage Access and the overlay
permission — never a runtime dialog, never auto-granted.

- **Check:** `NotificationManager.isNotificationPolicyAccessGranted()` —
  `platform/notifications/NotificationPolicyAccessPermission.kt`.
- **Grant:** `Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)` opens the
  system-wide "Do Not Disturb access" list (there is no per-app deep link, same
  limitation as Usage Access).
- **Required manifest declaration — the first bug found on-device:**
  `android.permission.ACCESS_NOTIFICATION_POLICY` is a *normal* permission
  (auto-granted at install, no dialog of its own) — but its declaration in
  `AndroidManifest.xml` is what makes Orlune appear **at all** in the "Do Not Disturb
  access" list. Confirmed by removing it: Orlune was completely absent from that
  list — alphabetically skipped between neighboring apps — so the user had no way to
  grant the real permission. Adding the `<uses-permission>` line fixed it immediately
  (rebuild + reinstall, then Orlune appeared in the list). This exactly mirrors the
  `PACKAGE_USAGE_STATS` discoverability requirement already documented in
  `AndroidManifest.xml` for Usage Access.

**Disclosure copy shown before requesting grant** (exact text, per product spec):
"Focus can silence interruptions while you work. Orlune does not read or store your
notification content." — shown in `FocusScreen.kt` only when the user has picked a
policy other than "Allow all" and the permission isn't yet granted, immediately above
the button that opens Settings. Requesting/showing this is skipped entirely for
"Allow all", since that policy needs no permission at all — Orlune never asks for
access it doesn't need for the user's current choice.

---

## The four policies, and what they actually configure

`core/domain/focus/FocusNotificationPolicy.kt` defines the enum and the pure,
JVM-unit-tested mapping to a platform-free `FocusZenSpec` (booleans only — no Android
import, so `FocusNotificationPolicyTest.kt` can exercise it without Robolectric).
`platform/notifications/FocusZenPolicyMapper.kt` is the thin, Android-touching layer
that turns a `FocusZenSpec` into the real `ZenPolicy`/interruption-filter constants;
that half is verified on-device, not by JVM unit test.

| Policy | Calls | Repeat callers | Selected apps/conversations | Alarms |
|---|---|---|---|---|
| Allow all | — | — | — | — (no Zen rule activated at all) |
| Silence all | blocked | blocked | blocked | **always allowed** |
| Allow calls | allowed (anyone) | allowed | blocked | always allowed |
| Allow calls + selected | allowed (anyone) | allowed | allowed, see caveat below | always allowed |

**Alarms are always left audible, for every policy that silences anything, including
"Silence all".** This is a deliberate product decision: Android's own DND UI draws
exactly this distinction ("Total silence" vs. "Alarms only" vs. "Priority only"), and
silencing a clock alarm the user set for themselves is a materially different,
higher-stakes decision than silencing notifications — Focus never makes that call on
the user's behalf. Confirmed on-device: the "Silence all" `ZenPolicy` correctly reads
`alarms=allow` while every other category reads `disallow`.

**"Allow calls + selected apps" — what Orlune can and cannot actually control.**
Orlune cannot mark another app's notification channel or a specific conversation as
"bypass Do Not Disturb" on the user's behalf — no public API grants a third-party app
that capability over *another* app's channels. What Orlune *can* do is turn on
`ZenPolicy.allowPriorityChannels(true)` and `allowConversations(CONVERSATION_SENDERS_IMPORTANT)`,
which lets through only whatever channels/conversations the user has **already**
separately marked as priority in that other app's own Android notification settings.
The Focus screen's copy says this plainly ("Orlune can't mark another app's
notifications as priority for you — pick apps below, then also mark each one 'Allow
interruptions' in its own Android notification settings"), and the selected-apps list
is Orlune's own record of intent (persisted per session, shown in the summary), not a
claim that selecting an app here is sufficient by itself.

`allowPriorityChannels`/`allowConversations` are guarded behind
`Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` (31) in `FocusZenPolicyMapper` — the
compileSdk 37 stub jar lists every method regardless of the API level it actually
shipped at, so this guard is a documented, deliberately conservative judgment call
(cross-referenced against public documentation, not independently verified against a
real API-30-only device); omitting the call on an older-but-still-R+ device leaves the
`ZenPolicy` builder's default (more restrictive, not less) — fail-safe either way.

---

## API-level branching (`FocusZenPolicyMapper.kt`)

Confirmed against the real `android.jar`/decompiled stubs, not assumed:

- **API 30 (R) and above:** `AutomaticZenRule`'s `ZenPolicy`-accepting constructor
  exists (`public AutomaticZenRule(String, ComponentName, ComponentName, Uri,
  ZenPolicy, int, boolean)`), so every category above is controlled individually.
- **API 29 (Q, this project's `minSdk`), below R:** that constructor does not exist.
  Only the older 5-arg constructor
  (`AutomaticZenRule(String, ComponentName, Uri, int, boolean)`) is available, which
  accepts one of the four `NotificationManager.INTERRUPTION_FILTER_*` constants
  instead of a `ZenPolicy`:
  - "Silence all" → `INTERRUPTION_FILTER_ALARMS` (silences everything but the
    device's own alarms — the closest fidelity match to the modern policy above).
  - "Allow calls"/"Allow calls + selected" → `INTERRUPTION_FILTER_PRIORITY`, which
    defers to whatever the device's own Settings → Sound → Do Not Disturb → "Priority
    only allows" list already says — Orlune cannot independently define "calls only"
    on API 29 the way it can on API 30+. Disclosed as a real, honest platform
    limitation, not glossed over.
  - `setAutomaticZenRuleState` itself (the direct app-toggle mechanism, not tied to
    `ZenPolicy`) is available from API 29 onward — i.e. exactly this project's
    `minSdk`, so the manual/no-`ConditionProviderService` approach works on every
    supported OS version, just with coarser policy fidelity below API 30.
  - **Not independently verified on real API 29 hardware** — this project's only test
    device is the Pixel 7a on API 37. The legacy branch is exercised by code review
    and the documented API surface only; treat it as unverified until tested on an
    actual API 29 device.

---

## The rule registration itself — the second bug found on-device

Initial implementation passed an arbitrary `owner` `ComponentName` (`MainActivity`)
with no `configurationActivity`, on the (incorrect, for this OS version) assumption
that any declared component would satisfy the platform's validation for an app that
manages its own rule state via `setAutomaticZenRuleState` rather than a bound
`ConditionProviderService`. **On-device (API 37) this threw:**

```
java.lang.IllegalArgumentException: Rule must have a valid (enabled) ConditionProviderService or configurationActivity
```

Fix: pass `owner = null` and `configurationActivity = ComponentName(context,
MainActivity::class.java)` instead. `MainActivity` is already an exported activity, so
this satisfies the platform's validation without adding a real
`ConditionProviderService` (a heavier, bound-service mechanism this feature doesn't
otherwise need) or a dedicated settings sub-screen. Confirmed via
`dumpsys notification --zen` after the fix: the rule registers successfully, activates
(`state=STATE_TRUE`, device-wide `zen_mode`/`mInterruptionFilter` flips to match), and
the correct `ZenPolicy` per selected policy is applied — see the physical-device
verification section of `docs/PROJECT_STATE.md` for the exact `dumpsys` output
captured for each policy.

**This same validation may or may not exist on API 29–like this project's legacy
constructor branch has not been independently confirmed on real hardware either** —
flagged for the same reason as the API-level branching note above.

---

## Lifecycle — start, tick, stop, reboot, process death

No stored "is my rule currently on" flag anywhere. Every reconciliation call derives
the *desired* state fresh from `FocusSessionEntity` rows via
`effectiveFocusNotificationState(sessions, now)`, then makes the live system rule
match — the same "derive, don't trust stale stored status" philosophy
`FocusSessionEngine`/`FocusSessionEntity` already use for session state itself (see
their own KDoc).

1. **Focus starts:** `FocusSessionRepository.startSession(...)` persists the chosen
   `notificationPolicy`/`allowedNotificationPackages` on the new row.
   `BlockingMonitorService`'s next tick (≤3s later, same latency budget already
   documented for blocking-overlay enforcement in
   `docs/android-platform-capabilities.md`) computes the effective state across all
   currently-`ACTIVE` sessions and calls `FocusZenRuleController.reconcile(...)`,
   which registers (if needed) and activates the rule.
2. **While active:** every tick recomputes and re-applies the effective state
   (idempotent — safe to call every 3s regardless of whether anything changed).
   Usage tracking and app-blocking are completely independent of this and are
   unaffected — the two features run side-by-side in the same tick, unchanged.
3. **Focus ends (natural completion or "Stop Focus"):**
   `FocusSessionRepository.cancelActiveSessions()`/`reconcileActiveSessions()`
   finalizes the session row (sets `endTs`). **Bug found and fixed:** the tick's
   `hasWorkToEnforce()` check (which decides whether to keep the foreground service
   alive) ran *before* the notification-policy reconciliation call — meaning the very
   tick that finalized the last active session also decided there was nothing left to
   enforce and called `stopSelf()` immediately, **without ever reconciling the now-off
   desired state**, leaving the Zen rule stuck `STATE_TRUE` indefinitely (confirmed
   on-device: `zen_mode` stayed `1`/DND stayed on after tapping "Stop Focus", with no
   further tick ever running to turn it off). Fixed by reconciling notification policy
   as the last action before `stopSelf()`, not skipping it on the service's exit path.
4. **Focus ending while Orlune is not open:** the foreground service (started
   automatically when Focus began, regardless of whether the app UI is open —
   this is the whole point of `BlockingMonitorService`) keeps ticking and will
   deactivate the rule via the same mechanism as (3), whether or not Orlune's UI is
   currently visible.
5. **App restart / process death / device reboot:** **A second, related bug**, found
   the same way — if the process is killed (or the device rebooted) while a rule is
   `STATE_TRUE`, and by the time the process restarts there's no longer any
   `ACTIVE`/`SCHEDULED` session or rule left (the session already finished, or its
   in-memory tick never got to finalize it), `OrluneApplication`'s existing
   `resumeMonitoringIfNeeded()` — which only restarts `BlockingMonitorService` when
   there's live work to enforce — never restarts the service at all, so **no tick ever
   runs again to turn the stray rule off**. Confirmed on-device: reinstalling the app
   mid-session left the Zen rule `STATE_TRUE` and DND on for multiple app relaunches
   with zero rules/sessions present, until fixed. Fix:
   `OrluneApplication.reconcileFocusNotificationPolicyOnColdStart()` — a new,
   unconditional one-shot check in `onCreate()`, independent of Usage
   Access/Overlay (notification policy has nothing to do with either), that finalizes
   any overdue session and reconciles the Zen rule every time the process starts, not
   just when the blocking service itself has a reason to run. This guarantees a
   reboot or a killed process always gets a chance to restore normal notification
   behavior the next time Orlune's process starts, even if that's just the user
   reopening the app rather than a background restart.
6. **Cancelled Focus sessions:** "Stop Focus" is `cancelActiveSessions()`, covered by
   fix (3) above.
7. **Timezone changes:** no special-case code exists or is needed. Every timestamp in
   this feature (`FocusSessionEntity.startTs`/`endTs`, `System.currentTimeMillis()`
   comparisons in `FocusSessionEngine`/`effectiveFocusNotificationState`) is epoch
   milliseconds throughout — never wall-clock/`LocalDateTime` — so a timezone change
   has zero effect on session lifecycle or notification-policy math. This was already
   true of `FocusSessionEngine` before this feature; the new code follows the same
   invariant rather than introducing a new one.
8. **Invalid duration:** unchanged, pre-existing behavior —
   `FocusSessionRepository.startSession` already `require()`s `plannedMinutes` in
   `1..1440`; this feature adds no new duration-validation surface.
9. **Permission revocation (mid-session or before start):** `FocusZenRuleController.reconcile`
   checks `NotificationPolicyAccessPermission.isGranted()` first and no-ops
   immediately if false — confirmed on-device this is a clean early return, not even
   a caught `SecurityException` in the common case. If the permission is somehow
   revoked in the exact instant between that check and the actual system call, every
   `NotificationManager` call in `FocusZenRuleController` is additionally wrapped in a
   `try/catch (SecurityException)`, logged and swallowed — never a crash. Confirmed
   on-device end-to-end: revoked access before starting Focus → session starts fine
   (app-blocking unaffected), Zen rule never activates, UI shows "Notifications:
   silenced — not applied (grant notification access in Settings)"; re-granting access
   and reopening Focus applies normally again.

---

## What Orlune can and cannot guarantee

- **Cannot guarantee emergency calls reach the user on every device.** Regular
  incoming-call handling during a Focus policy that allows calls depends on Android's
  own Do Not Disturb call-filtering behavior and the device manufacturer's own
  implementation of it, which is known to vary across OEM skins — Orlune configures
  the *policy* (`ZenPolicy.allowCalls`/`allowRepeatCallers`) and nothing more; it does
  not and cannot intercept, prioritize, or guarantee delivery of any specific call.
  UI copy states this plainly next to the calls-related policy options.
- **Outgoing emergency dialing is entirely unaffected** by anything in this feature —
  DND/Zen policy only ever governs *incoming* interruptions, never a user's ability to
  place an outgoing call, on any Android version.
- **Wireless Emergency Alerts / Cell Broadcast** (the OS-level "Emergency Alert" siren
  notifications for severe weather, AMBER alerts, etc.) are a separate,
  non-app-configurable system channel that bypasses DND by platform policy — Orlune
  has no control over them and makes no claim about them.
- **Known OEM variance (not independently verified on this project's single Pixel 7a
  test device):** community sources report inconsistent handling of third-party
  `AutomaticZenRule`s on some heavily-skinned OEM builds (e.g. some devices' own
  DND/Modes UI not surfacing a third-party app's rule the same way it surfaces
  first-party ones) — reported broadly, not something this project can independently
  confirm or deny without that hardware. Treated with the same caution as the
  already-documented OEM background-kill risk in `AGENTS.MD`'s "Known platform
  limitations" — never described as guaranteed-consistent across all devices.
- **The ≤3s tick latency** (same budget already documented for blocking-overlay
  enforcement) applies to notification-policy engagement/disengagement too — Focus's
  chosen policy does not necessarily take effect in the exact instant "Start Focus" is
  tapped, and normal notification behavior does not necessarily return in the exact
  instant "Stop Focus" is tapped. A few seconds either way, same as the rest of this
  app's enforcement model.

---

## Privacy / permissions actually added

- `android.permission.ACCESS_NOTIFICATION_POLICY` — normal, auto-granted, required
  purely for discoverability in the "Do Not Disturb access" list (see above); grants
  nothing on its own.
- No new dangerous/runtime permission. Notification Policy Access itself is a special
  app access (manual Settings toggle), same shape as Usage Access/Overlay, not a
  manifest-grantable or runtime-dialog permission.
- **No `NotificationListenerService`**, no notification content read, no notification
  content stored, no notification collection of any kind — this feature only ever
  changes whether the system lets a notification interrupt, never what any
  notification says.
- No `INTERNET` permission, no backend, no analytics — unaffected by this feature;
  re-verified via `git diff -- app/src/main/AndroidManifest.xml` showing only the one
  normal permission line added.

---

## Sources

- [`NotificationManager` reference](https://developer.android.com/reference/android/app/NotificationManager) — `addAutomaticZenRule`, `setAutomaticZenRuleState`, `isNotificationPolicyAccessGranted`.
- [`AutomaticZenRule` reference](https://developer.android.com/reference/android/app/AutomaticZenRule) — constructors, `Builder`.
- [`ZenPolicy` reference](https://developer.android.com/reference/android/service/notification/ZenPolicy) — category/people-type constants.
- [`Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`](https://developer.android.com/reference/android/provider/Settings#ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
- This project's own compileSdk 37 `android.jar` and `android-stubs-src.jar`
  (`F:\Android\Sdk\platforms\android-37.0\`), inspected directly via `javap`/unzip
  rather than trusted from memory, for every constructor/method signature and constant
  referenced above.
- On-device `dumpsys notification --zen`, `settings get global zen_mode`, and
  `logcat` output captured on the Pixel 7a (API 37) during this feature's development
  — the source of both bugs recorded above; neither was discoverable from
  documentation alone.
