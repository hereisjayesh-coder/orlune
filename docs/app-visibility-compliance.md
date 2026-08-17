# App Visibility Compliance

**Last verified:** 2026-08-17, against Android's official package-visibility documentation
and Google Play's "Use of the broad package (App) visibility (QUERY_ALL_PACKAGES)
permission" policy (see Sources below).

## Why Orlune needs app visibility at all

Orlune's core function — daily limits, schedules, focus sessions, and the app picker
that sets them up — requires knowing which apps are installed on the device, their
display names, and their icons. Without some form of package visibility, Orlune could
only let a user type a raw package name from memory, which is exactly the
unacceptable-for-a-consumer-app UX this app-picker work replaced.

## Why QUERY_ALL_PACKAGES is NOT used, and never has been

Google Play's policy restricts the broad `QUERY_ALL_PACKAGES` permission (for apps
targeting API 30+) to a short, explicitly-enumerated list of use cases: file
managers, browsers, antivirus/security apps, and finance apps (banking, digital
wallets) that must interoperate with "any and all apps on the device" to function.
Google explicitly disallows the permission when its use isn't directly tied to the
app's core purpose, and separately prohibits querying app-inventory data for sale, or
for analytics/ads monetization.

A digital-wellbeing/app-blocking tool is not on Google's permitted-use list. Even if
it were arguably defensible, Orlune doesn't need it: everything the app picker and
the blocking pipeline require — the set of user-launchable apps, each one's label and
icon — is fully satisfied by the narrower, purpose-scoped mechanism below. Adding
`QUERY_ALL_PACKAGES` here would be requesting more visibility than the feature needs,
which is exactly what Android's package-visibility model (introduced in Android 11)
exists to prevent.

## The mechanism actually used

`AndroidManifest.xml` already declares (since Phase 3, unchanged by this work):

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

This is Android's documented, minimal-visibility `<queries>` declaration: it grants
visibility only into apps that expose a launcher activity (i.e., apps a user could
open from a home screen or app drawer themselves) — not background services, not
system-internal packages, not apps with no user-facing entry point.

Two classes built on top of this same declaration:

- **`AppLabelResolver`** (Phase 3) resolves a single package name to its display
  label, used when Orlune already has a package name (from a stored rule or a usage
  event) and needs to show it to the user.
- **`InstalledAppLister`** (this work) enumerates the *entire* launchable-app set via
  `PackageManager.queryIntentActivities(Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER), ...)`
  — the app picker's data source. It excludes Orlune's own package (a rule blocking
  itself makes no sense) and resolves each app's label and icon.

Both fail gracefully: if a package can no longer be resolved (uninstalled between the
time a rule was created and now), the caller falls back to the raw package name
rather than crashing — see "Fallback behavior" below.

## Data stored

`InstalledAppLister` results are **not persisted**. Every time the app picker opens,
it re-queries `PackageManager` fresh — this is deliberate: it's the simplest way to
stay correct across install/uninstall without needing a separate cache-invalidation
mechanism, and querying the launchable set is fast enough that no caching has been
needed in practice on the reference device (Pixel 7a).

What *is* stored, in the existing `apps` Room table (Phase 2, unchanged by this
work): `packageName`, a display `label`, and two fields (`category`, `isEssential`)
reserved for features not yet built. No icon bitmap is persisted to disk — icons are
decoded fresh from `PackageManager` each time the picker is shown, avoiding both a
storage cost and a staleness risk (an app's icon can change on update).

`RuleEntity.targetPackageOrCategory` (Phase 2) stores the package name a rule
applies to — this is the same operational identifier the platform's `UsageStatsManager`
and `ActivityManager` already report, not new data collection.

## Why this approach is privacy-minimal

- Visibility is scoped to launchable apps only — never the full installed-package
  list (which on a typical device includes many system/background packages a user
  never sees or interacts with).
- Nothing derived from this query is ever transmitted anywhere — Orlune has no
  `INTERNET` permission (see `docs/dependency-audit.md`), so there is no code path by
  which an app inventory could leave the device even if a bug attempted it.
- Nothing derived from this query is sold, shared for advertising, or shared for
  analytics — Orlune has no advertising or analytics SDK of any kind.
- The query result isn't cached beyond the current picker session, minimizing how
  long "what apps does this user have installed" exists anywhere in memory.

## Google Play implications

Because Orlune uses the standard, scoped `<queries>` declaration rather than
`QUERY_ALL_PACKAGES`, it does not need to file Google Play's Permissions Declaration
Form for broad package visibility, and this is not expected to be a Data Safety
disclosure item under "device or other identifiers" the way a broader inventory
query might be. This should still be re-confirmed against the live Play Console Data
Safety questionnaire at the time of actual submission, since Google's forms and
categorization are revised periodically — see `docs/google-play-privacy-compliance.md`.

## Fallback behavior

- **App uninstalled after a rule references it:** `AppLabelResolver.resolveLabel()`
  catches `PackageManager.NameNotFoundException` and falls back to the raw package
  name rather than crashing. The rule keeps functioning (the blocking engine matches
  on package name, not on whether the app is currently resolvable to a label) — the
  user just sees the package identifier instead of a friendly name for that one
  stale entry, until they remove the rule.
- **`InstalledAppLister` encountering an unresolvable package mid-scan:** each
  package is wrapped in `runCatching`; a failure for one package is skipped rather
  than aborting the whole listing (`mapNotNull`), so one broken/inconsistent package
  entry can't blank out the entire app picker.
- **Icon load failure:** `getApplicationIcon` is wrapped separately and defaults to
  `null`; the app picker renders a generic icon placeholder for that row instead of
  leaving it blank or crashing.

## Sources

- [Declare package visibility needs — Android Developers](https://developer.android.com/training/package-visibility/declaring)
- [Package visibility filtering on Android — Android Developers](https://developer.android.com/training/package-visibility)
- [Use of the broad package (App) visibility (QUERY_ALL_PACKAGES) permission — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10158779?hl=en)
- [Permissions and APIs that Access Sensitive Information — Play Console Help](https://support.google.com/googleplay/android-developer/answer/16558241)
