# Orlune — Phase 0: Research & Feasibility

**Status:** Phase 0 complete and **signed off 2026-08-16**. All open questions in Section 17 are resolved below. Phase 1 is approved to begin.

Orlune is an original, local-only, no-account, no-AI Android digital wellbeing app. It is inspired by the general problem space of Opal, Android Digital Wellbeing, ScreenZen, one sec, Freedom, and StayFree, but shares no code, design, copy, algorithms, or branding with any of them. Everything below is Orlune's own design, informed by research into that problem space.

---

## 0. Branding Note — Read Before Anything Else

**The product was renamed from "Grove" to "Orlune" on 2026-08-16, final.** The original name "Grove" was dropped after a naming-conflict check found a genuine Play Store search-collision with "Focus Grove: Study & Focus," an existing gamified Pomodoro/focus timer in an adjacent category.

A follow-up naming-conflict check on **"Orlune"** found no app in the screen-time/digital-wellbeing/app-blocking category using this name or a close variant, and no evidence of a registered software/app trademark on it. There is unrelated, different-category use worth noting for later trademark diligence: a women's networking app/podcast ("Orlune," orluneuk.com), an apparel brand (wearorlune.com, orlune.store), and a similarly-spelled but distinct color-analysis app ("Orluna"). None overlap Orlune's category, so this is a low-risk name.

**Action still required before Phase 13 (Release Preparation):** run a formal USPTO TESS + EUIPO trademark search and a Play Store package-name reservation to confirm "Orlune" is clear to ship under, per the original brief's branding-conflict-check requirement. The name itself is locked; this remaining step is diligence, not a decision point.

---

## 1. Product Architecture

```
Android Platform APIs (UsageStatsManager, AccessibilityService[optional], VpnService[optional])
        ↓
Local Monitor          (platform/usage, platform/accessibility)
        ↓
Local Event Processor  (raw events → aggregated usage records)
        ↓
Local Rule Engine       (core/rules — evaluates schedules, limits, blocks)
        ↓
Local Algorithms        (core/algorithms — deterministic calculators)
        ↓
Local Decision           (block / warn / allow / friction)
        ↓
Local UI                (Jetpack Compose — feature/*)
        ↓
Local Database           (Room/SQLite — data/local)
```

No step in this pipeline ever touches the network. There is no server-side component, no remote config, no remote feature flags, and no analytics egress point anywhere in the diagram — this is a deliberate architectural constraint, not just a policy statement (see Section 10, Privacy Architecture).

## 2. Recommended Android Tech Stack

| Component | Choice | Why | Rejected alternative |
|---|---|---|---|
| Language | Kotlin | Standard modern Android, coroutines/Flow support | Java — no benefit here |
| UI | Jetpack Compose | Modern declarative UI, first-class theming or custom design systems, strong accessibility (semantics) support | XML/Views — more boilerplate, weaker for a from-scratch design system |
| Persistence | Room (over raw SQLite) | Type-safe queries, migration tooling, Flow-native observability, no external service, Apache 2.0, zero network | Raw SQLite — more manual work for no real gain; a NoSQL/cloud DB is explicitly out of scope |
| Concurrency | Kotlin Coroutines + Flow/StateFlow | Standard Android async idiom, pairs naturally with Room and Compose | RxJava — heavier, unnecessary given Compose+Coroutines is the modern default |
| Background scheduling | WorkManager | Handles Doze/App Standby correctly, survives reboot, standard AndroidX component, no network calls | Custom AlarmManager-only scheduling — reinvents constraint handling WorkManager already solves |
| DI | Manual DI (constructor injection) initially; Hilt only if the graph outgrows it | Avoid a dependency until the object graph actually justifies it; Hilt itself is AndroidX, Apache 2.0, no telemetry, no network — safe to add later without re-litigating privacy | Full DI framework from day one — premature for an app this size at Phase 1 |
| Charts | Custom Compose Canvas-drawn charts | No third-party charting library needed for the relatively simple visualizations required (bars, lines, rings); avoids an unnecessary dependency and keeps the design system fully original | MPAndroidChart / Vico — extra dependency surface for something Compose Canvas can do directly |

**Dependency policy going forward:** every dependency added in later phases must be justified in-line (need, license, network behavior, telemetry, privacy implication) per the original brief's Section 4 — this stack list is deliberately minimal so that bar stays easy to hold.

## 3. Competitor Feature Matrix

| App | Free Features | Paid Features & Pricing | Key Permissions | Account Required | Strengths | Weaknesses / Complaints | Privacy Notes |
|---|---|---|---|---|---|---|---|
| **Opal** | Basic blocking, limited sessions, usage tracking | ~$8–20/mo or ~$100/yr or $399 lifetime for Deep Focus, unlimited sessions, Focus Score, sync | Accessibility + Usage Stats | Yes, for sync | Polished UI, cross-device sync | Long persuasive onboarding before paywall; best features locked; Android weaker than iOS with ads on free tier; billing complaints | Claims on-device processing, opt-in minimization |
| **Android Digital Wellbeing** | Fully free: app timers, Focus Mode, Bedtime Mode, dashboard | None | System-level, built-in | Tied to device/Google account | Most accurate (OS-level) data, zero cost | Only ONE Focus Mode schedule (top complaint); "Ignore for today" trivially defeats timers; minimal customization | Stays within Google/OS ecosystem |
| **ScreenZen** | Fully free core: pre-open friction, app + website blocking, limits, schedules | No mandatory sub; optional cloud "Social Accountability" | Usage Access, overlay, Accessibility | Not required for core | Genuinely free core, friction-based design | Inconsistent website blocking; wrongful blocks/lost streaks reported | Core stays local; social feature uses Firebase |
| **one sec** | One protected app, breathing-exercise friction | ~$4/mo, ~$25/yr, or $50 lifetime for multi-app, more friction types | Accessibility (preferred) or Usage Access | Not required offline | Simple, well-researched friction mechanic | Free tier covers only 1 app — most users pushed to Pro fast; doesn't cap in-app session length | Offline by default |
| **Freedom** | Limited free sessions | ~$3–7/mo or ~$40/yr for unlimited blocklists, cross-device sync | Local VPN, Accessibility | Yes, for sync | Best-in-class cross-device sync | VPN-based blocking drains battery; fails under battery-saver; bypassable via browser switch/uninstall | Claims no logs; brand confusion with unrelated "Freedom VPN" apps |
| **StayFree** | Ad-free dashboard, app limits, basic website blocking | Premium tier (ads removal, extra stats) | Accessibility | Not required for core | Detailed usage charts, free core | Misleading marketing (claims Reels/Shorts blocking that reviewers say doesn't fully work); injects its own ads into browser pages | Offers data opt-out but not ads opt-out — trust red flag |

### Gaps and Opportunities for Orlune

- **Permission trust vacuum:** every competitor leans on Accessibility with weak disclosure. Orlune's plain-language, Usage-Access-by-default / Accessibility-as-opt-in model (Section 5, `accessibility-service-compliance.md`) is a direct differentiator.
- **Free tier as bait, not product:** Opal/one sec gate the actually useful features. Orlune's core rule engine, scheduling, and focus sessions ship complete and free — see Section 13 (MVP).
- **No mandatory account:** Opal/Freedom require sign-up for core sync; Orlune never syncs anywhere, so there's nothing to require an account for.
- **Single-schedule limitation** in Google's own tool is a well-documented pain point — Orlune supports multiple named schedules/profiles from day one.
- **Bypass-ability erodes trust category-wide** ("Ignore for today," uninstall/wifi-toggle, "view app details" loopholes) — Orlune's Blocking Model (Section 16 of the brief) is explicit about friction levels and honest about what it can't enforce, rather than silently having an easy escape hatch.
- **VPN battery-drain complaints (Freedom)** and **ad-injection dark patterns (StayFree)** are both avoidable: Orlune defaults to Usage-Access/Accessibility-based blocking (not VPN) for apps, and treats VPN-based website filtering as an optional, clearly-limited feature — never the primary mechanism, and never ad-supported.

## 4–5. Android API Capability & AccessibilityService Feasibility

Full detail lives in `android-platform-capabilities.md` and `accessibility-service-compliance.md`. Summary:

- **Foreground-app detection:** `UsageStatsManager` (polling) is the default, lower-risk mechanism. `AccessibilityService` is offered as an optional, explicitly-disclosed, opt-in latency upgrade — never a hard dependency. This directly follows Google's Oct 2025 Accessibility policy tightening (deterministic/rule-based automation is the narrow exception Orlune relies on) and the emerging Android 17 Advanced Protection Mode restriction.
- **Blocking mechanism:** a `SYSTEM_ALERT_WINDOW` overlay shown on detected foreground-app match. Inherent latency/race-condition limits apply (see capability matrix row 12) — Orlune must never claim instant, flawless blocking.
- **Feasibility verdict:** technically sound and Play-compliant if scoped exactly as documented; the Accessibility path carries real, ongoing policy risk that must be re-checked before every release (see Section 12).

## 6. App-Blocking Feasibility Analysis

Feasible today using `UsageStatsManager` + `SYSTEM_ALERT_WINDOW`, with `AccessibilityService` as an optional lower-latency upgrade. Fundamental limits (from the capability matrix) that must be communicated honestly to users:

- Blocking cannot survive the user disabling Usage Access/Accessibility or uninstalling Orlune — there is no consumer-grantable tamper-proof mode without Device Owner enrollment, which is out of scope for a mainstream consumer install.
- A brief flash of the blocked app before the overlay draws is possible due to detection latency; this should be disclosed rather than hidden.
- OEM-specific background-kill behavior (MIUI, One UI, EMUI, OxygenOS, etc.) can silently stop the monitoring service; Orlune needs OEM-specific "please allow autostart/disable battery restriction" guidance screens, sourced from community documentation (Google does not document this itself).

**Verdict:** feasible as a strong, honest, "raises the friction / makes distraction a deliberate choice" tool — not as an "unbreakable" blocker. This framing should carry through to all product copy (see Section 15 of the brief, No Shame / No Manipulation).

## 7. Website-Blocking Feasibility Analysis

Feasible only as a **best-effort, secondary feature**, via local `VpnService` DNS/IP filtering:

- Works reasonably well for plain HTTP and standard-DNS HTTPS traffic to known domains.
- Does **not** reliably block domains resolved via encrypted DNS (DoH/DoT) or served from shared/IP-rotating CDNs — this is a hard platform limit, not an implementation gap.
- Occupies the device's single VPN slot, conflicting with any other VPN the user runs (privacy tools, corporate VPN) — real, disclosed friction.
- Must never inspect encrypted payload content — filtering decisions are DNS-query/IP-destination based only, never man-in-the-middle content inspection.

**Verdict:** ship as an explicitly-labeled "Website Blocking (Beta / Best-Effort)" feature with an in-app explanation of its limits, not marketed as equivalent to app blocking's reliability.

## 8. Local Data Model (Room/SQLite)

Aggregation-first design — raw usage events are processed into daily aggregates and not retained indefinitely (per the brief's Section 10 instruction).

| Entity | Purpose | Key fields (illustrative, not final schema) |
|---|---|---|
| `App` | Known app metadata | packageName, label, category, isEssential |
| `AppCategory` | User/system category grouping | id, name, isSystemDefined |
| `DailyUsage` | Aggregated per-app, per-day usage | packageName, date, totalUsageSeconds, launchCount, sessionCount |
| `Session` | Individual app-open-to-close session (short retention, rolls into DailyUsage) | packageName, startTs, endTs |
| `FocusSession` | User-initiated focus period | startTs, endTs, plannedMinutes, completedMinutes, blockedCategoryIds |
| `Rule` | A single enforceable rule | type (limit/schedule/block), targetPackageOrCategory, threshold, windowDefinition |
| `Schedule` | Recurring time window | daysOfWeek, startTime, endTime, associatedRuleId |
| `BlockRule` / `AllowRule` | Explicit block/allow lists, including essential-app exemptions | packageName, listType |
| `Goal` | User-defined target | type (usage/focus/balance), targetValue, period |
| `HabitRecord` | Rolling consistency tracking | date, goalId, met (bool) |
| `NotificationPreference` | Quiet-period/reminder config | quietWindows, digestEnabled |
| `ThemePreference` | Light/Dark/Forest selection | themeId |
| `UserPreference` | Misc app settings | key, value |
| `LocalRecommendation` | Output of the deterministic RecommendationEngine, shown then discarded/archived | date, ruleSuggestion, basis |
| `EmergencyOverride` | Logged override usage (for the user's own transparency, not enforcement) | timestamp, ruleId, reason |
| `PrivacySetting` | Per-permission status snapshot shown in the Privacy Center | permissionName, granted, lastChecked |

Raw `UsageEvents` from the platform API are read, aggregated into `DailyUsage`/`Session`, and not stored beyond what's needed to compute that aggregation — consistent with the brief's explicit raw-event aggregation example.

## 9. Algorithm Architecture (`core/algorithms/`)

All algorithms are deterministic: same input → same output, no ML, no hidden weighting. Each ships with unit tests covering edge cases (midnight rollover, timezone changes, DST, empty history).

| Module | Purpose | Formula (illustrative) |
|---|---|---|
| `UsageCalculator` | Per-app/day usage totals | Σ(session durations) per packageName per date |
| `SessionCalculator` | Session boundaries from raw events | pair MOVE_TO_FOREGROUND/BACKGROUND events, clamp orphaned sessions at day rollover |
| `LimitEngine` | Evaluate app/category limits | currentUsage vs. configured threshold → warn/friction/block state |
| `ScheduleEngine` | Evaluate recurring time windows | currentTime ∈ [schedule.start, schedule.end] on currentDayOfWeek |
| `BlockingEngine` | Combine rule + schedule outputs into a single decision | precedence rules across overlapping active rules |
| `FrictionEngine` | Configurable delay before allowing access | delaySeconds(userConfig) |
| `GoalEngine` | Goal progress | completedUnits / plannedUnits |
| `ConsistencyEngine` | Rolling-window consistency | daysGoalMet / totalDaysInWindow |
| `RecommendationEngine` | Deterministic rule-based suggestions | e.g. IF usage ≥ 1.25 × baseline AND goal exceeded THEN suggest shorter limit |
| `StatisticsEngine` | Baselines & trends | 7/14/30-day rolling averages; recentAvg vs. historicalAvg trend delta |
| `DigitalBalanceEngine` | Composite, fully-documented balance metric (Orlune's own, not a copy of "Focus Score") | documented weighted formula over goal/focus/distraction consistency — to be finalized and published in Phase 7 |

Every module gets a spec comment block (purpose/inputs/outputs/formula/assumptions/edge cases) and a unit test file in Phase 7 — not implemented in Phase 0.

## 10. Privacy Architecture

- **Zero outbound network requests** — enforced architecturally (no networking dependency in the app at all for the core product), not just policy-promised. A build-time check (no `INTERNET` permission in the manifest, unless/until a justified, disclosed exception is added) is the actual technical guarantee, stronger than a Data Safety form claim alone.
- **Local-only storage:** Room database in app-private storage, never backed up to cloud by default (`android:allowBackup` and `dataExtractionRules` configured deliberately, reviewed in Phase 9).
- **Aggregation over raw retention:** per Section 8 above.
- **Data Safety form implication:** per the compliance research, on-device-only usage/app-info data can be declared "not collected" — but only if genuinely never transmitted, including by any future dependency. This is a standing constraint on every dependency decision, not a one-time form-filling exercise.
- **Privacy Center (Phase 9):** live, human-readable listing of exactly what's stored locally, current permission status, and one-tap export/delete — see the original brief's Section 22 for full scope.

## 11. Security Architecture

Threat model (full detail deferred to `THREAT-MODEL.md` in a later phase):

| Threat | Orlune's actual posture |
|---|---|
| Malicious co-installed app reading Orlune's local DB | Standard Android app sandboxing; no shared storage of sensitive data; Room DB in private app storage |
| Local database tampering (rooted device) | Not fully preventable on a rooted device — documented as a known limitation, not claimed as solved |
| Bypassing restrictions (disable permission, force-stop, uninstall) | Expected and documented (see Section 6) — Orlune raises friction, never claims tamper-proof enforcement |
| Device reboot | WorkManager/boot-completed re-registration restores active rules; no rule state silently lost |
| Permission revocation mid-use | Detected on next check; graceful fallback (Accessibility→Usage Access) or clear "blocking paused" state — never a silent failure |
| Clock changes | Rule/schedule evaluation must be tested against manual clock manipulation attempts (Phase 11 test case) — documented as a known soft spot, since no consumer app can fully defend against user-controlled system clock |

Orlune will document actual guarantees, not aspirational ones, per the brief's explicit instruction not to claim absolute security.

## 12. Google Play Compliance Risks

Ranked by severity:

1. **AccessibilityService declaration risk (highest, ongoing):** Oct 2025 policy tightening + Android 17 Advanced Protection Mode changes make this the single most volatile compliance surface. Mitigation: optional/opt-in only, Usage-Access-first default, deterministic-rule-based framing matched exactly between in-app disclosure and Play Console declaration + demo video (see `accessibility-service-compliance.md`).
2. **Usage Access disclosure timing:** must show in-app rationale immediately before directing to Settings, not bury it in a general permissions list.
3. **QUERY_ALL_PACKAGES avoidance:** the app-blocklist UI needs the installed-app list; using scoped `<queries>` with a launcher intent filter avoids the Play-gated broad-visibility permission entirely.
4. **SYSTEM_ALERT_WINDOW justification:** must be clearly tied to the disclosed blocking purpose only, never repurposed (e.g., no in-overlay upsells).
5. **VpnService declaration:** must explicitly state local-only filtering, no ad interference, no external traffic routing in the store listing — Play's Nov 2022 VPN policy change explicitly exempts declared parental-control/firewall-style local use, which covers Orlune's case.
6. **Battery-optimization-exemption request:** framed narrowly around blocking reliability, not general convenience, since Play restricts this to a named acceptable-use list.
7. **Data Safety form accuracy:** every field must be re-validated against the actual shipped dependency list before each release — "no data collected" is only true if it's actually true of the final build, including third-party libraries.
8. **Privacy policy + in-app link:** mandatory regardless of the zero-collection posture.

## 13. MVP Feature List

**In scope for MVP:**
- Onboarding: welcome, privacy promise, Usage Access setup, initial rule, theme selection
- Home: today's usage, active rules, current focus/block status
- App limits (daily), one recurring schedule type, one focus-session type
- Reminder + Block levels of the blocking model (Friction and Strict Focus can follow post-MVP)
- Essential-app exemptions + a documented emergency override
- Insights: daily/weekly usage, per-app usage, basic trend vs. personal baseline
- Privacy Center: permissions status, local data listing, export (JSON), delete-all
- Light, Dark, Forest themes
- Full offline operation, zero network permission

**Deferred past MVP:** AccessibilityService opt-in upgrade, website blocking (VpnService), multiple concurrent schedules/profiles, Friction/Strict Focus levels, DigitalBalanceEngine composite metric, CSV export, RecommendationEngine.

This scoping keeps Phase 1–happens-first work small enough to validate the core architecture (Usage Access → Rule Engine → Blocking → Room) before adding the higher-compliance-risk features (Accessibility, VPN).

## 14. Phase-by-Phase Development Plan

Following the brief's Section 40 phase list exactly:

| Phase | Focus | Key exit criteria |
|---|---|---|
| 0 | Research & feasibility | This document set. ✅ Complete pending your review. |
| 1 | Architecture & Android project setup | Project compiles, module structure in place, no features yet |
| 2 | Local database & domain models | Room schema from Section 8, migrations, no UI yet |
| 3 | Usage monitoring | UsageStatsManager integration, permission flow, raw→aggregated pipeline |
| 4 | Deterministic rule engine | `core/rules` + `core/algorithms` from Section 9, fully unit-tested |
| 5 | App blocking | SYSTEM_ALERT_WINDOW overlay, foreground-service loop |
| 6 | Scheduling & focus sessions | Recurring schedules, focus session flow |
| 7 | Analytics & algorithms | Statistics/Consistency/Recommendation engines, full test coverage |
| 8 | Original UI & themes | Compose design system, Light/Dark/Forest |
| 9 | Privacy Center & data controls | Export, delete, live permission status |
| 10 | Security & performance | Battery/CPU/memory measurement, threat model doc |
| 11 | Testing | Full automated + instrumentation suite per Section 35 |
| 12 | Google Play compliance | Data Safety form, accessibility declaration + video, privacy policy finalized |
| 13 | Release preparation | Store listing, branding conflict resolution (Section 0), signing |

## 15. Recommended First Milestone

**Phase 1 + a thin vertical slice of Phase 2/3:** set up the Android project skeleton (module structure per the brief's Section 36), get Usage Access permission flow working end-to-end, and display *today's total screen time* on a bare Home screen backed by a real Room database. This proves the entire core pipeline (platform API → local processor → local DB → local UI) with the riskiest platform integration (UsageStatsManager) validated early, before investing in the rule engine, blocking, or UI polish. No blocking, no Accessibility, no VPN in this milestone — those come in Phases 4–6.

## 16. Technical Risks

1. **AccessibilityService policy volatility** (see Section 12.1) — highest risk, mitigated by making it optional.
2. **OEM background-kill inconsistency** — no Google-documented fix exists; requires per-OEM user guidance and realistic expectation-setting in the UI.
3. **Detection latency / overlay race condition** — inherent to the polling+overlay model; must be measured and disclosed, not hidden.
4. **Website blocking's hard technical ceiling** (encrypted DNS) — must be scoped as best-effort from the start to avoid an unfulfillable promise.
5. **Exact-alarm permission friction** — Orlune doesn't clearly qualify for the no-permission alarm-clock carve-out; users must grant "Alarms & reminders" for time-sensitive rule re-checks, adding an extra onboarding step.
6. **Branding collision** (Section 0) — lower technical risk, real go-to-market risk if unresolved before Phase 13.

## 17. Open Questions — Resolved 2026-08-16

1. **Branding:** name changed from "Grove" to **"Orlune"** to sidestep the "Focus Grove" collision entirely. Locked, final — see Section 0.
2. **AccessibilityService:** confirmed optional/opt-in only. Usage Access remains the default/required foreground-detection mechanism; Accessibility is an explicitly-disclosed opt-in latency upgrade, never a hard dependency. Shapes Phase 5/6 as originally scoped.
3. **Website blocking:** deferred entirely past MVP, per the recommended MVP scope in Section 13. Revisit as a labeled beta in a later release.
4. **Minimum supported Android version:** **minSdk = Android 10 (API 29).** Broader than the Android-13+ assumption used elsewhere in this document — Phase 3 (usage monitoring) and Phase 10 (security & performance) need to cover API 29–32 OEM and permission-model behavior in addition to the 13+ baseline already researched.
5. **Monetization:** **free at launch, no monetization implemented now.** Core features available at launch stay free forever and must never be paywalled later. No ads, tracking, mandatory accounts, cloud services, or subscription infrastructure in the MVP. Future monetization, if ever added, may only cover genuinely new, optional advanced features/services — never something that compromises the local-only core. Given the app has no recurring server/AI costs, a one-time Pro purchase or optional supporter model should be evaluated ahead of a subscription if this is ever revisited. Nothing to implement in Phase 0–1; Phase 13 (store listing) should document this architecture posture so no future feature accidentally locks core functionality behind a paywall.

---

**Phase 0 is complete and signed off.** All open questions above are resolved. Phase 1 (Architecture & Android Project Setup) is approved to begin.
