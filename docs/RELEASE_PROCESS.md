# Orlune — Release Process

Step-by-step process for shipping a real Orlune release to Google Play, plus the
release checklist. This file describes *how* to release; `docs/PROJECT_STATE.md`
tracks *where the current release candidate actually stands* against it — always
read that file for the live status, this file for the fixed procedure.

Nothing in this file authorizes actually publishing anything. Every release still
requires an explicit, deliberate human decision at the Play Console "Publish" step
(and at the signing-key-generation step below) — no automated process, CI job, or
agent should ever cross either of those two points unattended.

## 1. Versioning

- `versionName` (`app/build.gradle.kts`) is the human-facing semantic version shown
  in Play Store listings and Settings → About Orlune (e.g. `"1.0.0"`).
- `versionCode` is the integer Play Store uses to order releases — must strictly
  increase on every release uploaded to Play Console (including staged/internal
  tracks), never reused, never decreased.
- Bump both together in the same commit as the release-prep work, and record the
  change in `docs/CHANGELOG.md`.
- Follow semantic versioning going forward: `MAJOR.MINOR.PATCH`. `1.0.0` is the
  first public release; a patch release (bug fix, no new feature) bumps `PATCH`; a
  release that adds a feature bumps `MINOR`; a release with breaking data/behavior
  changes bumps `MAJOR`.

## 2. Signing setup (one-time, before the first signed release)

**No release signing key exists in this repository, and none should ever be
generated automatically by an agent or script.** Generating a signing key is a
deliberate, human decision: whoever holds this key controls every future update to
the app on Play (Play App Signing manages the *app* signing key on Google's side,
but you still need an *upload* key to sign what you send them, and losing the
upload key — or the original app signing key if not using Play App Signing — can
permanently block future updates).

**What a human needs to do, once, outside of any agent session:**

1. Generate an upload keystore:
   ```
   keytool -genkeypair -v -keystore orlune-upload.jks -alias orlune -keyalg RSA -keysize 2048 -validity 10000
   ```
   Choose a strong, unique keystore password and key password (they may be the
   same or different). Record the exact alias used (`orlune` above is a suggestion,
   not a requirement).
2. Store `orlune-upload.jks` **outside this repository** — a password manager's
   secure file storage, an encrypted volume, or a secrets vault. Keep at least one
   independent backup in a second secure location; a lost upload key with Play App
   Signing enabled is recoverable via Google's key-reset process (slow, requires
   identity verification), but a lost *original* app signing key without Play App
   Signing is unrecoverable and ends that app's update history permanently.
3. Enroll in **Play App Signing** at first upload (Play Console's default and
   recommended path) so Google holds the app signing key and the upload key above
   only needs to authenticate you to Play Console, not sign the final distributed
   APK — this is the single biggest mitigation against upload-key loss.
4. Never commit the `.jks` file, the passwords, or the alias/password pair to git,
   Slack, email, or any other non-secrets-management channel. `.gitignore` already
   blocks `*.jks`, `*.keystore`, `keystore.properties`, `*.keystore.properties`,
   and `signing.properties` — keep that list current if the credential-storage
   approach below changes.

**How the build then reads those credentials** (add this once the keystore
exists — do not add a `signingConfigs` block that reads directly from
`app/build.gradle.kts` literals):

Create `keystore.properties` (gitignored, lives only on the signing machine / CI
secrets store) next to `local.properties`:
```
storeFile=/absolute/path/to/orlune-upload.jks
storePassword=...
keyAlias=orlune
keyPassword=...
```
Then in `app/build.gradle.kts`:
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...existing isMinifyEnabled/isShrinkResources/proguardFiles unchanged
        }
    }
}
```
This keeps the build reproducible on any machine that has its own
`keystore.properties` (or CI secrets injecting one), with zero credentials ever
touching version control. A build without `keystore.properties` present
(e.g. this repo, right now) continues to produce an explicitly unsigned
`app-release-unsigned.apk`, exactly as today — never a silently-debug-signed one.

## 3. Build

From the project root, with the environment variables `AGENTS.MD`'s "Build
commands" section documents:
```
.\gradlew.bat clean testDebugUnitTest connectedDebugAndroidTest assembleRelease bundleRelease --stacktrace
```
- `assembleRelease` produces the APK (`app/build/outputs/apk/release/`) — useful
  for direct-install smoke testing, not what gets uploaded to Play.
- `bundleRelease` produces the AAB (`app/build/outputs/bundle/release/`) — this is
  the artifact Play Console requires for a production/testing-track upload.
- Confirm the output filename does **not** contain `unsigned` before uploading —
  `app-release-unsigned.apk`/an unsigned bundle means `keystore.properties` wasn't
  found or didn't resolve; do not hand-sign an ad-hoc APK as a workaround.

## 4. Tests (must pass before any tag/upload)

- `testDebugUnitTest` — every unit test, 0 failures.
- `connectedDebugAndroidTest` — every instrumentation test against a real device
  (`adb devices` must list one; never claim this passed without a device attached
  and real output read — see `AGENTS.MD`).
- Full manual walkthrough per `docs/PROJECT_STATE.md`'s release-hardening checklist
  (onboarding, app picker, limits, blocking, Focus, Quiet Mode, Insights, Settings,
  export/delete, restart, permission revoke/restore) — run it again on the actual
  release-signed build before the *first* production upload, not just on a
  debug-signed stand-in.

## 5. Play Console submission checklist

Work through in order — each step blocks the next:

1. **Signing** — release signing key generated and secured (Section 2); Play App
   Signing enrolled.
2. **Versioning** — `versionName`/`versionCode` bumped, `docs/CHANGELOG.md` updated
   (Section 1).
3. **Build** — signed AAB built and smoke-tested on a real device (Sections 3-4).
4. **Tests** — full unit + instrumentation + manual pass, all green, recorded in
   `docs/PROJECT_STATE.md`.
5. **Privacy policy** — hosted at a stable, publicly reachable URL (does not exist
   yet — see `docs/google-play-privacy-compliance.md`); linked from both Play
   Console's app content settings and the in-app Legal Center (already built).
6. **Legal review** — every "Legal review required" row in
   `docs/legal-compliance-matrix.md` resolved by qualified counsel; the in-app
   Privacy Policy/Terms of Service `[TBD]` placeholders (legal entity, address,
   contact) replaced with real, verified business details.
7. **Data Safety form** — filled in Play Console directly against the mapping in
   `docs/google-play-privacy-compliance.md` (that document is a drafting aid, not a
   substitute for the live form).
8. **Play Store listing** — app name, short description, full description,
   category, contact details, entered in Play Console.
9. **Screenshots** — real device screenshots for each required form factor (phone
   at minimum), reflecting the actual shipped UI, not mockups.
10. **Feature graphic** — 1024×500 PNG/JPG per Play's current spec, on-brand
    (black-first, the approved launcher mark), created and uploaded.
11. **Content rating & target audience** — questionnaire completed in Play
    Console; cross-check against the Children & Teen Privacy open questions in
    `docs/legal-compliance-matrix.md` before answering.
12. **Package visibility / permissions declarations** — `PACKAGE_USAGE_STATS` and
    `SYSTEM_ALERT_WINDOW` Permissions Declaration Form justifications submitted if
    Play Console requests them (see `docs/google-play-privacy-compliance.md`).
13. **Staged/closed testing** — upload to an Internal Testing or Closed Testing
    track first; install on at least one additional real device beyond the Pixel
    7a if available; verify install-from-Play behaves identically to a sideloaded
    build (permissions flow, first launch, no dev-only artifacts visible).
14. **Production release** — only after every prior step is confirmed and a human
    has explicitly decided to publish; staged rollout percentage is a reasonable
    default over 100% on day one.

## 6. After release

- Tag the release commit in git (`git tag v1.0.0`) once actually published, not
  before.
- Update `docs/PROJECT_STATE.md`'s "Latest verified commit"/status to reflect the
  live Play Store version.
- Any post-release hotfix follows this same process at a `PATCH` version bump —
  no shortcuts for "it's just a small fix."
