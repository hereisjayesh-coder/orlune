# Google Play Privacy & Legal Compliance

**Last verified:** 2026-08-17, against Google Play's current Developer Program Policy
pages (Data Safety, User Data, package visibility, permissions) — see Sources. This
document maps Orlune's actual, shipped implementation to Google Play's policy areas.
It is an engineering reference for filling out the Play Console Data Safety form
accurately, not a substitute for that form or for legal review before submission.

## Guiding rule

Every claim below is checked against the actual manifest, dependency list, and code
in this repository as of this document's date — not against what a "privacy-first
app" is generally assumed to do. If Orlune's implementation changes, this document
must be re-verified, not assumed still accurate.

## User Data policy

Google Play requires an accurate, complete Data Safety declaration covering all data
collected or shared, including via third-party SDKs, and a posted privacy policy
(both in Play Console and in-app).

**Orlune's actual data handling**, verified against the manifest and dependency list:

- **Collected (in Google's Data Safety sense — data that leaves the app/device):
  none.** Orlune has no `INTERNET` permission (confirmed in `AndroidManifest.xml`)
  and no third-party SDK capable of transmitting data (confirmed in
  `docs/dependency-audit.md` — AndroidX/Jetpack and Kotlin coroutines only).
- **Processed locally, never transmitted:** app-usage sessions and daily totals (via
  `UsageStatsManager`), the rules/schedules/focus sessions the user creates, an
  allow/block list, and appearance preference — all in a local Room database. See
  `docs/app-visibility-compliance.md` for the app-picker's data handling
  specifically.
- **Shared:** nothing, with anyone, for any purpose (advertising, analytics, or
  otherwise) — there is no mechanism by which sharing could occur.

Google's Data Safety form specifically defines "collection" as data transmitted off
the device; data that is processed and stored only locally, and never leaves the
device, is generally excluded from that definition. Orlune's on-device-only usage
and rule data should therefore not require declaration as "collected" under the
form's own terms — this should be verified directly against the live Play Console
questionnaire at submission time, since Google revises the form's category
boundaries periodically (most recently, the April 2025 update reclassifying Android
ID as a device identifier).

**Do not declare "No data is collected" as a blanket marketing statement** in the
privacy policy or Play listing — say precisely what's true: usage and rule data is
processed and stored locally, and none of it is transmitted by Orlune. See
"IMPORTANT — no false absolute claims" below.

## Privacy Policy requirement

Google requires a privacy policy link in Play Console and a privacy policy
accessible in-app. Orlune's Legal Center (Settings → Privacy & Legal) satisfies the
in-app requirement once published; the Play Console listing will need the same
policy hosted at a stable URL before submission — **this URL does not exist yet**
and is a release blocker independent of the document content itself, since Orlune
has no hosting/website infrastructure as of this document's date.

## Data Safety section — expected category outcomes

Pending a live pass through the actual Play Console form (which this document
cannot substitute for), the expected declarations based on the implementation above:

| Data Safety category | Expected declaration | Basis |
|---|---|---|
| Location | Not collected | No location permission requested |
| Personal info (name, email, etc.) | Not collected | No account, no input field collects this |
| Financial info | Not collected | N/A to this product |
| Health and fitness | Not collected | N/A |
| Messages | Not collected | No messaging access |
| Photos/videos | Not collected | No media permission |
| App activity (app interactions, in-app search history) | Processed locally, not transmitted off-device | `UsageStatsManager`-derived sessions/daily totals stored only in local Room DB |
| Web browsing | Not collected | No browser history access |
| App info and performance (crash logs, diagnostics) | Not collected | No crash-reporting SDK |
| Device or other IDs | Not collected | No Android ID, advertising ID, or other identifier is read |

## Package visibility

Covered in full in `docs/app-visibility-compliance.md`. Summary for this document:
Orlune uses the scoped `<queries>`/`CATEGORY_LAUNCHER` mechanism, not
`QUERY_ALL_PACKAGES`, and therefore does not need Google Play's Permissions
Declaration Form for broad app visibility.

## Accessibility API

**Not applicable.** Orlune does not use Android's `AccessibilityService` API for any
purpose — confirmed in `AGENTS.MD`'s forbidden-technologies list and unchanged by
this work. If this is ever reconsidered, Google Play's Accessibility API disclosure
and justification requirements would apply in full at that time, and this document
must be updated before that feature ships, not after.

## Foreground services

`BlockingMonitorService` runs as a foreground service with `foregroundServiceType="specialUse"`
and a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` of `"app_blocking"` — the closest-fitting
Android 14+ category for enforcement-monitoring that doesn't match a more specific
type (dataSync, location, etc.). This is declared in `AndroidManifest.xml` and
requires no additional Play Console disclosure beyond the standard foreground-service
policy compliance (a visible, ongoing notification while active, which
`BlockingMonitorService` provides).

## Sensitive permissions requiring justification

| Permission | Why it's requested | User-facing grant mechanism |
|---|---|---|
| `PACKAGE_USAGE_STATS` | Reads which apps are opened and for how long, to power Home/Insights/rule enforcement | Manual grant via Android Settings — not a runtime dialog |
| `SYSTEM_ALERT_WINDOW` | Draws the block screen over another app when a rule/session is active | Manual grant via Android Settings — not a runtime dialog |
| `POST_NOTIFICATIONS` | Shows a persistent notification while the monitoring service runs | Standard runtime permission dialog (Android 13+) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Required for the background monitoring service | Declared automatically, not user-facing |

Both `PACKAGE_USAGE_STATS` and `SYSTEM_ALERT_WINDOW` are "special" permissions Google
Play may require a Permissions Declaration Form justification for at submission time;
both have a single, obvious, product-core justification (usage measurement and block
enforcement respectively) that should be straightforward to declare accurately.

## Prominent disclosure and consent

Google's policy requires prominent, in-context disclosure before collecting
sensitive data via a sensitive permission, for permissions where the connection to
the app's core function isn't obvious. Orlune's `PACKAGE_USAGE_STATS` and
`SYSTEM_ALERT_WINDOW` requests are both directly, obviously tied to the app's stated
purpose (a screen-time/blocking tool asking to measure usage and draw a block
screen) — but the actual in-app permission-request screens (`Settings` →
Permissions, `Focus`/`Limits` empty-state prompts) should be reviewed against
Google's specific prominent-disclosure UI requirements (a clear, non-buried
explanation shown before or at the point of the Settings hand-off) before
submission, not assumed compliant by inference.

## IMPORTANT — no false absolute claims

Per this task's explicit instruction, and reflected in every in-app legal document
already: never state "no data is collected" or "your data can never leave the
device" as an unqualified guarantee. State precisely what is true: Orlune's current
architecture does not use a backend, does not transmit data, and has no code path
capable of doing so — while distinguishing "Orlune's own code" from "your device's
security," "your OS," or "your own actions" (e.g., a third-party backup tool you use
independently), which are outside Orlune's software and outside what any app-level
claim can honestly cover.

## What is not yet done

- No privacy policy URL is hosted anywhere — required before Play Console submission.
- No live pass through the actual Data Safety questionnaire has been performed; the
  table above is this document's best-effort mapping from the implementation, not a
  completed submission.
- Content rating / target-audience declarations (relevant to the Children & Teen
  Privacy open questions in `docs/legal-compliance-matrix.md`) are undecided.

## Sources

- [Google Play's data disclosure requirements — Android Developers](https://developers.google.com/admob/android/next-gen/privacy/play-data-disclosure)
- [Provide information for Google Play's Data safety section — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Use of the broad package (App) visibility (QUERY_ALL_PACKAGES) permission — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en)
- [Permissions and APIs that Access Sensitive Information — Play Console Help](https://support.google.com/googleplay/android-developer/answer/16558241)
