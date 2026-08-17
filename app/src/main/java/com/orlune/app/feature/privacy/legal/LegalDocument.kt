package com.orlune.app.feature.privacy.legal

/**
 * [body] uses a small self-parsed subset of markdown (see LegalDocumentBody): lines
 * starting with "# " are the document title, "## "/"### " are section headers, "- "
 * are bullet items, blank lines separate paragraphs. No markdown library dependency
 * — deliberately, per this project's no-unnecessary-dependency rule.
 */
data class LegalDocument(
    val id: String,
    val listTitle: String,
    val body: String
)

object LegalDocuments {

    /** Bracketed placeholders throughout stand in for business/legal facts not yet
     * decided (entity name, address, jurisdiction, contact). Every document here is
     * a development-time draft — see "legal-version" — not a reviewed, published
     * legal document. Do not remove the placeholders without replacing them with
     * real, confirmed values from legal counsel. Content is cross-checked against
     * docs/legal-compliance-matrix.md and docs/google-play-privacy-compliance.md;
     * update all three together when the underlying facts change. */
    const val DOCUMENT_SET_VERSION = "0.2.0-draft"
    const val MATCHES_APP_VERSION = "0.1.0"
    const val EFFECTIVE_DATE_STATUS = "Not yet effective — no version of these documents has been published."

    val all: List<LegalDocument> = listOf(
        LegalDocument(
            id = "privacy-policy",
            listTitle = "Privacy Policy",
            body = """
                # Privacy Policy

                ## 1. Introduction
                This Privacy Policy describes how Orlune ("Orlune," "the app," "we," "us") handles information when you use it. Orlune is developed by [LEGAL ENTITY NAME — TBD]. This is a development-time draft prepared for internal review — it has not been reviewed by a lawyer and is not yet published as Orlune's binding privacy policy. Do not rely on it as a final legal document. See "Legal document version / effective date" for this draft's status.

                ## 2. Scope
                This Policy covers the Orlune Android application only. It does not cover any third-party app you use alongside Orlune, any Android system behavior outside Orlune's control, or any future Orlune product not yet released. If Orlune ever adds a website, companion service, or account system, this Policy will be revised before that feature ships — it describes today's local-only architecture, not a hypothetical future one.

                ## 3. Definitions
                "Personal data" means information relating to an identified or identifiable person. "Processing" means anything done with data — collecting, storing, analyzing, or deleting it. "On-device" or "local" means data that exists only in Orlune's private app storage on your device, per Android's standard app-sandboxing model. "Data Fiduciary" and "Data Principal" are terms from India's Digital Personal Data Protection Act, 2023, used here only where that Act may be relevant (see Section 18).

                ## 4. Data Orlune accesses
                Through Android's Usage Access permission (`PACKAGE_USAGE_STATS`), Orlune reads which apps you have opened and for how long, using the `UsageStatsManager` system API. Through the app picker, Orlune reads the list of apps installed on your device that have a launcher icon (see Section 9). Orlune does not access your files, photos, messages, call log, contacts, location, camera, or microphone — it has never requested permission to do so.

                ## 5. Data processed locally
                - Derived app-usage sessions (which app, when it started/ended), computed from raw Usage Access events
                - Daily usage totals per app
                - The rules, schedules, and focus sessions you create
                - An allow/block list of apps you've explicitly configured
                - Your appearance preference (System/Light/Dark)
                - A small internal marker used to avoid re-processing the same usage events twice

                All of this is processed entirely on your device, inside the Orlune app process, and stored in a local database described in Section 11. See Section 9 for what "app metadata" specifically means.

                ## 6. Data not collected
                Orlune has no account system, so it never collects a name, email address, phone number, or password. It has no advertising SDK and collects no advertising identifier. It has no analytics or crash-reporting SDK and collects no diagnostic or telemetry data. It does not request Android's `INTERNET` permission, so none of the data in Section 5 — or anything else — can be transmitted from your device by Orlune, technically or otherwise.

                ## 7. Purpose of processing
                Every processing activity described in Section 5 exists to power a specific, visible feature: usage totals power Home and Insights; rules and schedules power the Limits screen and the blocking engine; focus-session data powers the Focus screen; the allow/block list powers essential-app exemptions; appearance preference powers System/Light/Dark theming. Nothing is processed for a purpose you can't see reflected in the app's own screens.

                ## 8. App usage monitoring
                Orlune's usage pipeline reads raw foreground/background events from `UsageStatsManager`, pairs them into sessions, and aggregates sessions into daily per-app totals — all in a background service on your device. This pipeline cannot run at all until you manually grant Usage Access in Android Settings; Android does not allow apps to request this permission via an in-app dialog.

                ## 9. App/package metadata
                To show real app names and icons instead of raw package identifiers (e.g. "YouTube" instead of `com.google.android.youtube`), Orlune's app picker reads the list of installed apps that have a launcher entry, using Android's scoped `<queries>` mechanism — not the broader `QUERY_ALL_PACKAGES` permission. See `docs/app-visibility-compliance.md` in the source repository for the full technical rationale. This list is read fresh each time you open the picker and is not saved beyond what a rule or session already references (its package name).

                ## 10. Permissions
                - Usage Access — measures app usage — granted manually in Android Settings
                - Display over other apps — shows the block screen — granted manually in Android Settings
                - Notifications — shows a status notification while monitoring runs — granted via the standard runtime dialog (Android 13+)

                Full detail, including exactly why each permission is needed and what happens if it's denied or revoked, is in "Data Collection & Permissions."

                ## 11. Local database
                All data in Section 5 is stored in a local SQLite database (via Android's Room library) inside Orlune's private, sandboxed app storage. Cloud backup and device-to-device transfer of this data are explicitly disabled at the Android level (`allowBackup="false"`, empty backup/transfer extraction rules) — it is excluded from Android's automatic backup systems by design, not merely by policy.

                ## 12. Retention
                Orlune does not delete data automatically. Usage sessions and daily totals accumulate until you delete them yourself; there is no fixed retention window (e.g., "30 days") implemented today. Full detail is in "Data Storage & Retention."

                ## 13. Export
                "Export local data" in the Privacy Center creates a single JSON file containing every table in Orlune's local database and hands it to Android's system share sheet — you choose where it goes. Orlune does not upload this file anywhere itself.

                ## 14. Deletion
                "Delete all local data" and "Reset application" both immediately and irreversibly clear every table in Orlune's local database. Uninstalling Orlune removes its private app storage entirely, as a standard consequence of Android's app-storage model, independent of whether you use the in-app delete option first.

                ## 15. Security
                Orlune's security posture is built around having no network attack surface: no `INTERNET` permission, no server, no listening socket. Standard Android app sandboxing isolates its local storage from other apps without root access. Full detail, including what this does not cover (a rooted device, physical access to an unlocked device), is in "Security Statement."

                ## 16. Children's and teen users
                Orlune has no age gate, no account, and no age-based feature differentiation — it processes the same on-device data regardless of who uses it. Whether specific children's-privacy obligations (India's DPDP Act Section 9, COPPA, or similar) apply to Orlune has not been determined. See "Children & Teen Privacy" and `docs/legal-compliance-matrix.md`.

                ## 17. International users
                Orlune may be used by people in jurisdictions with their own data-protection frameworks (for example, the EU/UK's GDPR regime, or California's CCPA/CPRA). Whether and how each framework applies to a local-only, non-transmitting app has not been finalized — see `docs/legal-compliance-matrix.md` for the current, per-jurisdiction analysis and its open questions.

                ## 18. Legal basis / applicable legal framework
                Where a legal basis for processing is required by an applicable framework (for example, GDPR's Article 6, or DPDP's consent/legitimate-use grounds), Orlune's on-device-only processing is intended to rely on it being processing you directly initiate and control, for your own benefit, entirely on your own device. Whether this reasoning is legally sufficient under each specific framework is an open question — see `docs/legal-compliance-matrix.md`. [LEGAL REVIEW REQUIRED — TBD]

                ## 19. User rights
                Because your data never leaves your device, most rights a privacy law grants against a data holder (access, correction, erasure, portability) are things Orlune already gives you directly, immediately, and without needing to make a request to anyone: Export gives you a full copy: Delete gives you full erasure; there is nothing held elsewhere to correct or port. If you believe a right you're entitled to isn't covered by these in-app controls, contact us using Section 20.

                ## 20. Grievance and contact process
                Questions or concerns about this Policy: [CONTACT EMAIL — TBD]. Some jurisdictions require a named grievance officer or regional representative — this has not yet been designated. Full detail is in "Contact / Grievance."

                ## 21. Policy updates
                If this Policy changes after Orlune is released, the new version and its effective date will be shown here, and prior versions' change history will be kept in "Legal document version / effective date."

                ## 22. Effective date
                Not yet effective — no version of this Policy has been published.

                ## 23. Version
                Document set version 0.2.0-draft, matching app version 0.1.0.
            """.trimIndent()
        ),
        LegalDocument(
            id = "terms-of-service",
            listTitle = "Terms of Service",
            body = """
                # Terms of Service

                ## Status
                Draft, not yet legally reviewed or published. Placeholder terms for internal development use. Also referred to as "Terms & Conditions."

                ## 1. Acceptance
                By installing and using Orlune, you agree to these Terms. If you do not agree, do not install or use the app.

                ## 2. Eligibility
                Orlune does not currently verify age or eligibility — there is no account system to gate. If you are using Orlune on behalf of a child or a device you manage for someone else, see "Children & Teen Privacy" for the current, unresolved state of that question.

                ## 3. Description of Orlune
                Orlune is a local, offline digital-wellbeing application for Android, published by [LEGAL ENTITY NAME — TBD]. It helps you observe your own app usage and optionally set limits, schedules, and focus sessions. It operates no backend service — these Terms govern your use of the software itself.

                ## 4. Free-use license
                Subject to these Terms and the "Intellectual Property" and "Open-Source Components" sections below, you are granted a personal, non-exclusive, non-transferable, revocable license to install and use Orlune on devices you own or control, for your own personal, non-commercial use, at no charge.

                ## 5. User responsibilities
                You are responsible for the device Orlune runs on, for granting or withholding the permissions it requests, and for the rules, schedules, and focus sessions you configure. Orlune enforces exactly what you set up — it does not exercise independent judgment about what should be blocked.

                ## 6. Rules and app-blocking limitations
                Orlune's blocking relies on Android's overlay mechanism, checked periodically by a background service — not real-time, kernel-level enforcement. See "Wellness / Product Disclaimer" for the full, specific list of known limitations (detection latency, overlay opt-outs on Android 12+, OEM background-kill behavior). By using Orlune's blocking features, you accept these limitations as inherent to the platform, not as defects.

                ## 7. Device/OS limitations
                Orlune's functionality depends on Android APIs and permissions that vary by manufacturer, OS version, and user-configured battery/background-process settings. [LEGAL ENTITY NAME — TBD] does not control, and is not responsible for, how a given device or OS build implements these APIs.

                ## 8. Permissions
                Orlune requests only the permissions described in "Data Collection & Permissions." You may revoke any of them at any time in Android Settings; doing so degrades or disables the corresponding feature (see that document for exactly which) rather than crashing the app.

                ## 9. No guarantee of uninterrupted enforcement
                Orlune does not guarantee that a rule, schedule, or focus session will block every attempt to open a restricted app, every time, without exception. See "Wellness / Product Disclaimer."

                ## 10. No medical or mental-health treatment claim
                Orlune is a self-directed productivity and digital-wellbeing tool. It is designed to support more intentional digital habits — it is not a medical device, therapeutic product, or treatment for any condition, and makes no claim to diagnose, treat, cure, or prevent addiction, ADHD, anxiety, depression, or any other medical or mental-health condition. If you are experiencing distress related to technology use, consult a qualified professional.

                ## 11. User responsibility for emergency/essential access
                You are responsible for ensuring that blocking rules do not interfere with your access to emergency services, essential communication, or any app you require for safety-critical purposes. Orlune's essential-app allow list exists for this purpose — configuring it correctly for your own needs is your responsibility.

                ## 12. Local data responsibility
                Because all Orlune data is stored only on your device, you are responsible for its safekeeping — including using "Export local data" if you want a backup before uninstalling, switching devices, or deleting all data. [LEGAL ENTITY NAME — TBD] has no copy to restore for you.

                ## 13. Intellectual property
                Orlune's source code, design, and branding are the property of [LEGAL ENTITY NAME — TBD], except for the open-source components described below. See "Free / Open-Source Positioning" in "About Orlune" for the current licensing status of Orlune's own source code.

                ## 14. Open-source components
                Orlune incorporates open-source software under their own licenses — see "Open-Source Licenses" for the current list.

                ## 15. Third-party applications and services
                Orlune's core function involves observing and, when you configure it to, restricting access to other apps on your device. This does not create any relationship between you and those apps' publishers, and Orlune is not responsible for their behavior, content, or terms.

                ## 16. Acceptable use
                Do not use Orlune to monitor or restrict a device without the knowledge and consent of the person who primarily uses it, except where you have a legitimate legal basis to do so (for example, a parent configuring a device used by their own minor child, subject to applicable law). Do not attempt to reverse engineer, decompile, or redistribute Orlune except as permitted by its actual license (see "Open-Source Licenses" and "About Orlune").

                ## 17. Modifications
                [LEGAL ENTITY NAME — TBD] may update these Terms. Continued use of Orlune after an update constitutes acceptance of the revised Terms. Material changes will be reflected in "Legal document version / effective date."

                ## 18. Suspension and termination
                Because Orlune has no account system, there is no account to suspend. This license terminates automatically if you breach these Terms; upon termination, you must stop using the app and remove it from your devices.

                ## 19. Disclaimer of warranties
                Orlune is provided "as is" and "as available." To the maximum extent permitted by [JURISDICTION — TBD] law, [LEGAL ENTITY NAME — TBD] disclaims all warranties, express or implied, including merchantability, fitness for a particular purpose, and non-infringement.

                ## 20. Limitation of liability
                To the maximum extent permitted by law, [LEGAL ENTITY NAME — TBD] is not liable for indirect, incidental, special, or consequential damages arising from use of Orlune, including missed deadlines, lost productivity, or distress arising from blocking behavior working differently than expected.

                ## 21. Indemnity
                [Indemnification clause — pending legal review as to whether one is appropriate for a free, non-commercial local tool of this kind. LEGAL REVIEW REQUIRED — TBD]

                ## 22. Governing law
                These Terms are governed by the laws of [JURISDICTION — TBD], without regard to conflict-of-law principles, pending confirmation by legal counsel.

                ## 23. Dispute resolution
                [Dispute resolution mechanism — arbitration, small-claims carve-out, venue — not yet determined. LEGAL REVIEW REQUIRED — TBD]

                ## 24. Severability
                If any provision of these Terms is found unenforceable, the remaining provisions continue in full force and effect.

                ## 25. Entire agreement
                These Terms, together with the Privacy Policy and the End User License Agreement, constitute the entire agreement between you and [LEGAL ENTITY NAME — TBD] regarding Orlune, superseding any prior agreements on the same subject.

                ## 26. Contact
                [CONTACT EMAIL — TBD]

                ## 27. Effective date
                Not yet effective — no version of these Terms has been published.

                ## 28. Version
                Document set version 0.2.0-draft, matching app version 0.1.0.
            """.trimIndent()
        ),
        LegalDocument(
            id = "eula",
            listTitle = "End User License Agreement",
            body = """
                # End User License Agreement (EULA)

                ## Status
                Draft, pending legal review.

                ## Grant of license
                [LEGAL ENTITY NAME — TBD] grants you a limited, non-exclusive, non-transferable, revocable license to install and run one copy of the Orlune application on devices you own or control, for personal, non-commercial use, subject to this Agreement and the Terms of Service.

                ## Restrictions
                You may not: reverse engineer, decompile, or disassemble the app except to the extent applicable law expressly permits; redistribute, sell, rent, or sublicense the app; remove or alter any proprietary notices; or use the app to build a competing product.

                ## Ownership
                Orlune, including its source code, design, and branding, is and remains the property of [LEGAL ENTITY NAME — TBD]. This Agreement does not transfer any ownership rights to you. See "About Orlune" for the current, undecided status of Orlune's own source-code license — this EULA governs the compiled app you install; it does not by itself make any claim about whether Orlune's source is or is not open source.

                ## Open-source components
                Orlune incorporates open-source software components under their own licenses. See "Open-Source Licenses" for the full list.

                ## Termination
                This license terminates automatically if you breach this Agreement. Upon termination, you must stop using the app and remove it from your devices.

                ## No liability for third-party platforms
                This Agreement governs Orlune only. Your use of the Android operating system and your device manufacturer's software is governed by their own terms, not this Agreement.
            """.trimIndent()
        ),
        LegalDocument(
            id = "data-collection-permissions",
            listTitle = "Data Collection & Permissions",
            body = """
                # Data Collection & Permissions

                ## Principle
                Orlune only requests a permission when a specific, currently-shipped feature needs it. No permission is requested speculatively for a future feature.

                ## Permissions Orlune requests, and exactly why

                ### Usage Access (PACKAGE_USAGE_STATS)
                Lets Orlune read which apps you have opened and for how long, using Android's UsageStatsManager. This is what powers Home, Insights, and rule/limit/schedule enforcement. Granted manually in Android Settings — Android does not allow apps to request this via a normal permission dialog. Orlune reads only currently-installed, launchable apps' usage; it does not read file contents, messages, browsing history, or any in-app content.

                ### Display over other apps (SYSTEM_ALERT_WINDOW)
                Lets Orlune draw the block screen over another app when a limit, schedule, or focus session is active. Granted manually in Android Settings. Without it, Orlune can still measure usage but cannot show the block screen.

                ### Notifications (POST_NOTIFICATIONS)
                Lets Orlune show a persistent notification while its monitoring service is running, so you know it's active in the background. If denied, the monitoring service continues to run and enforce your rules — this permission only affects whether you see the notification.

                ### Foreground service (FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE)
                Required by Android for the background service that watches for rule/schedule/focus-session triggers while the app isn't in the foreground. Not a user-facing permission — it is declared automatically, not requested through a dialog.

                ## App picker visibility (not a runtime permission)
                The app picker (used when choosing which apps a rule or focus session applies to) reads the list of installed, launchable apps using Android's scoped `<queries>` declaration — not the broader `QUERY_ALL_PACKAGES` permission, and not a user-facing grant of any kind. Full technical and policy detail is in `docs/app-visibility-compliance.md` in the source repository.

                ## Permissions Orlune does NOT request
                No INTERNET permission (no network access is even possible). No location, camera, microphone, contacts, SMS, call log, storage-wide access, or QUERY_ALL_PACKAGES. No AccessibilityService — Orlune does not currently use or request accessibility services (see "Security Statement" for what this means for blocking reliability).

                ## What Orlune does not collect
                No account identifiers, no advertising ID, no device fingerprinting beyond what's needed to resolve an installed app's name/icon, no browsing history, no message content, no location.
            """.trimIndent()
        ),
        LegalDocument(
            id = "data-storage-retention",
            listTitle = "Data Storage & Retention",
            body = """
                # Data Storage & Retention

                ## Where data lives
                All Orlune data is stored in a local SQLite database (via Android's Room library), inside Orlune's private app storage on your device. This storage is sandboxed by Android — other apps cannot read it without root access. Cloud backup of this data is explicitly disabled, so it is excluded from Android's automatic device backups and device-to-device transfer.

                ## What is stored
                - Derived app-usage sessions (which app, when it started/ended) computed from Android's Usage Access API
                - Daily usage totals per app
                - Rules, schedules, and focus sessions you create
                - An allow/block list of apps you've explicitly configured
                - Your appearance preference (System/Light/Dark)
                - A small internal marker used to avoid re-processing the same usage events twice

                A few additional local tables exist in the app's database schema for features that are planned but not yet built (see "About Orlune" for current feature status); they are not populated by any feature you can currently use.

                ## Retention
                Orlune does not currently delete data automatically. Usage sessions and daily totals accumulate indefinitely until you delete them yourself. There is no fixed retention window (for example, "30 days") implemented today — if a specific retention period is added in a future version, this document will be updated to describe it, and it will not be described here until it exists in the shipped app.

                ## What removes data
                - Using "Delete all local data" or "Reset application" in the Privacy Center removes everything immediately
                - Uninstalling Orlune removes its private app storage, including this database, per standard Android app-uninstall behavior
                - Individual rules, schedule entries, and allow/block entries can be removed one at a time from their own screens
            """.trimIndent()
        ),
        LegalDocument(
            id = "data-export-deletion",
            listTitle = "Data Export & Deletion",
            body = """
                # Data Export & Deletion

                ## Export
                "Export local data" in the Privacy Center creates a single JSON file containing every table in Orlune's local database — all usage sessions, daily totals, rules, schedules, focus sessions, and preferences — and hands it to Android's system share sheet. You choose where it goes: save it to device storage, send it to yourself, or share it with any app you select. Orlune does not upload this file anywhere itself; it has no server to upload it to.

                ## Deletion
                "Delete all local data" (and "Reset application," which performs the same underlying action) immediately and irreversibly clears every table in Orlune's local database and stops any active monitoring/blocking. There is no undo, no recovery window, and no copy retained anywhere by Orlune. Export first if you want to keep a record.

                ## Partial deletion
                Individual items — a single rule, schedule, allow/block-list entry, or focus session — can be deleted independently from their own screens without clearing everything else.

                ## Uninstalling
                Uninstalling the app removes all of its local data as a standard consequence of Android's app storage model, whether or not you use the in-app delete option first.
            """.trimIndent()
        ),
        LegalDocument(
            id = "security-statement",
            listTitle = "Security Statement",
            body = """
                # Security Statement

                ## Threat model
                Orlune's security model is built around having no network surface and no remote-attacker path: it requests no INTERNET permission, opens no listening sockets, and has no server-side component to compromise. The primary risks it defends against are local — another app on the same device reading its data, or the device's own backup/transfer mechanisms exposing it.

                ## What's in place today
                - No INTERNET permission — the single largest class of app data-security incidents (network interception, server breach, insecure API) does not apply to Orlune
                - Cloud backup and device-transfer of Orlune's data is explicitly disabled
                - Standard Android app sandboxing isolates Orlune's private storage from other apps without root access
                - No third-party SDK is bundled beyond standard AndroidX/Jetpack libraries and Kotlin coroutines — see "Third-Party Notices"
                - App visibility for the app picker uses Android's scoped `<queries>` mechanism, not the broader `QUERY_ALL_PACKAGES` permission — see `docs/app-visibility-compliance.md`

                ## What this does not cover
                - A rooted or compromised device, or physical access to a device someone has already unlocked, is outside what any app-level security model can protect against
                - Orlune has not undergone an independent third-party security audit as of this document's effective date
                - The reliability limits of the blocking mechanism itself (not a security property, but related) are covered separately in "Wellness / Product Disclaimer"

                ## Reporting a security issue
                Security concerns can be reported to [CONTACT EMAIL — TBD]. A formal responsible-disclosure process has not yet been established.
            """.trimIndent()
        ),
        LegalDocument(
            id = "accessibility-statement",
            listTitle = "Accessibility Statement",
            body = """
                # Accessibility Statement

                ## Current status
                Orlune's interface is built with Jetpack Compose using standard Android accessibility semantics (screen-reader labels, standard touch-target components, system font-scaling support) rather than custom-drawn, non-accessible controls. A dedicated accessibility audit — screen-reader walkthrough, contrast verification across all three appearance modes, large-text layout testing — has not yet been completed as of this document's effective date.

                ## What Orlune does NOT use
                Orlune does not use Android's AccessibilityService API for any purpose — not for app-blocking detection, not for anything else. This is a deliberate product decision, not a technical limitation. If this ever changes, it would only ever be an explicitly-disclosed, opt-in upgrade path, never a default or silent dependency, and this document would be updated before that feature shipped.

                ## Known limitations
                - Color contrast in all three appearance modes has not yet been formally verified against accessibility guidelines
                - Custom-drawn visualizations, if added to Insights in the future, will need explicit non-visual alternatives, not yet implemented
                - No testing has been performed with TalkBack or Switch Access as of this document's effective date

                ## Feedback
                Accessibility issues can be reported to [CONTACT EMAIL — TBD].
            """.trimIndent()
        ),
        LegalDocument(
            id = "wellness-disclaimer",
            listTitle = "Wellness / Product Disclaimer",
            body = """
                # Wellness / Product Disclaimer

                ## Not a medical or therapeutic product
                Orlune is a self-directed screen-time tool, designed to support more intentional digital habits. It is not a medical device, therapeutic product, or mental-health treatment, and is not a substitute for professional advice regarding addiction, mental health, or behavioral concerns. Orlune does not claim to cure, treat, diagnose, or prevent addiction, ADHD, anxiety, depression, or any other medical or mental-health condition, and does not claim to medically improve sleep or change brain function. If you are experiencing distress related to technology use, please consult a qualified professional.

                ## Blocking is not guaranteed or unbreakable
                Orlune's app-blocking relies on Android's SYSTEM_ALERT_WINDOW overlay mechanism, checked periodically (currently roughly every few seconds) by a background service — not real-time, kernel-level enforcement. As a result:
                - There can be a short delay between opening a blocked app and the block screen appearing
                - Some Android versions (12+) let an app opt out of being overlaid, which can prevent blocking that specific app
                - Manufacturer-specific background-process management (common on some Android skins) can stop Orlune's monitoring service, regardless of standard battery-optimization exemptions
                - Nothing prevents a determined user from disabling Orlune's permissions or uninstalling it to bypass a block

                Orlune never describes its blocking as "unbreakable," "guaranteed," or "always-on," and this document makes the same limitation explicit.

                ## No outcome guarantee
                Orlune does not guarantee any particular outcome — reduced screen time, improved focus, or behavior change — from using the app. Results depend on your own use of its features.
            """.trimIndent()
        ),
        LegalDocument(
            id = "children-teen-privacy",
            listTitle = "Children & Teen Privacy",
            body = """
                # Children & Teen Privacy

                ## Status
                Draft. Orlune's target audience, age rating, and any child-directed-service determination — under India's DPDP Act Section 9, the US's COPPA, or equivalent rules elsewhere — have not yet been finalized as of this document's effective date. This section must be completed with legal input before release, particularly given Orlune's screen-time/parental-interest subject matter. See `docs/legal-compliance-matrix.md` for the current, per-jurisdiction open questions.

                ## Current design-level facts relevant to this determination
                Orlune has no account system and collects no directly-identifying information (name, email, birthdate) at any point, from any user, of any age — because it has no account system at all. All processing described in "Data Collection & Permissions" happens locally regardless of who is using the device.

                ## What is not yet decided
                - Whether Orlune will be marketed or positioned as suitable for use on a device primarily used by a child or teen
                - Whether a specific children's-privacy compliance statement is required under any applicable law, and if so, its content
                - The app's eventual Google Play content rating

                ## Placeholder statement
                Until the above is finalized, Orlune should not be represented as verified-compliant with any children's privacy law, in any jurisdiction. [LEGAL REVIEW REQUIRED — TBD]
            """.trimIndent()
        ),
        LegalDocument(
            id = "open-source-licenses",
            listTitle = "Open-Source Licenses",
            body = """
                # Open-Source Licenses

                ## Status
                This is a manually-maintained summary, not an automatically generated license report. Before commercial release, this list should be regenerated from the actual dependency tree to guarantee completeness and exact version/license accuracy.

                ## Third-party components Orlune uses
                - Kotlin and Kotlin Coroutines — Apache License 2.0 — JetBrains / Kotlin Foundation
                - AndroidX Jetpack libraries (Core, Lifecycle, Activity, Compose UI, Compose Material 3, Room, WorkManager) — Apache License 2.0 — The Android Open Source Project
                - Material Components icon set (material-icons-extended) — Apache License 2.0 — Google

                Orlune adds no dependency outside the AndroidX/Jetpack and Kotlin ecosystems.

                ## Orlune's own source code license
                This section is about the licenses Orlune *uses*. Whether Orlune's own source code is released under an open-source license is a separate question, covered in "About Orlune" — as of this document's effective date, no LICENSE file exists in the source repository and no open-source license has been chosen. Do not describe Orlune itself as "open source" until that changes.

                ## Full license texts
                Full license texts for the above are the standard Apache License, Version 2.0. This document intentionally does not reproduce the full license text inline; a future version may bundle it or link to an in-app licenses viewer generated from the build.
            """.trimIndent()
        ),
        LegalDocument(
            id = "third-party-notices",
            listTitle = "Third-Party Notices",
            body = """
                # Third-Party Notices

                ## No third-party services
                Orlune integrates no third-party service, SDK, or API that receives data from your device — no analytics platform, no crash reporter, no advertising network, no cloud storage provider, no AI/ML API. This is enforced structurally: the app requests no INTERNET permission, so no such integration could function even if added by mistake.

                ## Third-party code
                The only third-party code in Orlune is the open-source libraries listed in "Open-Source Licenses" — none of which transmit data off-device as configured in this app; they are used purely as local, on-device libraries (UI framework, local database, background task scheduling).

                ## Trademarks
                Any third-party trademarks that may appear in Orlune (for example, "Android," or the names of apps shown in your own usage data via the app picker) belong to their respective owners and are used only descriptively, to identify apps you have installed and chosen to include — not to imply endorsement, affiliation, or sponsorship by those trademark owners.
            """.trimIndent()
        ),
        LegalDocument(
            id = "contact-grievance",
            listTitle = "Contact / Grievance",
            body = """
                # Contact / Grievance

                ## General contact
                [CONTACT EMAIL — TBD]

                ## Privacy or data requests
                Because Orlune stores all data locally on your device and never sends a copy anywhere, most "access/export/delete my data" requests are things you can already do yourself in the Privacy Center — Export Data and Delete All Data act immediately, with nothing held back on any server. For any question this doesn't answer, contact [CONTACT EMAIL — TBD].

                ## Grievance officer / regional contact requirements
                Some jurisdictions (for example, India's IT Rules and DPDP Act, or the EU's GDPR) require a named grievance officer or regional representative with published contact details. This has not yet been designated: [LEGAL ENTITY NAME — TBD] / [GRIEVANCE OFFICER NAME — TBD] / [GRIEVANCE OFFICER CONTACT — TBD].

                ## Response time
                A committed response-time SLA has not yet been established. [RESPONSE TIME COMMITMENT — TBD]
            """.trimIndent()
        ),
        LegalDocument(
            id = "about-orlune",
            listTitle = "About Orlune",
            body = """
                # About Orlune

                ## What Orlune is
                Orlune is a local digital-wellbeing tool designed to help you use your time more intentionally — free, local-only, no account, no ads, no analytics, no AI.

                ## Version and build
                App version 0.1.0 (versionCode 1) — pre-release, under active development. Not yet published to Google Play.

                ## License
                No LICENSE file currently exists in Orlune's source repository, and no open-source license has been selected. Orlune is not currently released under any recognized open-source license — do not describe it as "open source" until a license is actually chosen and published. If Orlune is released under an open-source license in the future, this section will name it precisely, and the Terms of Service will be reviewed for consistency with it.

                ## Privacy model
                No account, no login, no cloud, no backend server, no AI, no advertising SDK, no analytics SDK, no behavioral tracking. Usage information is processed entirely on your device. Full detail is in the Privacy Policy and the rest of this Legal Center.

                ## Open-source information
                Orlune itself is not (yet) open source — see "License" above. It does use open-source third-party components; see "Open-Source Licenses" for the full list.

                ## Legal documents
                All 15 documents in this Legal Center, including this one, are development-time drafts — see "Legal document version / effective date."

                ## What's implemented today
                Usage monitoring, deterministic limit/schedule rule enforcement with a native app picker, app blocking via an overlay, one-time focus sessions, local JSON export, and delete-all-data controls, across System/Light/Dark appearance modes.

                ## What's intentionally not built yet
                Website/VPN blocking, Android's AccessibilityService-based detection, onboarding flow, recurring focus-session scheduling, and any analytics, recommendation, or AI/ML feature — all deliberately deferred, not overlooked.

                ## Feedback
                Settings → Feedback opens your device's own email app, addressed to the Orlune team, with a suggested subject and a blank template you fill in yourself. Orlune does not collect, store, or transmit feedback — it only hands off to whichever email app you already have installed, or tells you plainly if none is available.

                ## Acknowledgements
                Orlune is built from original design, code, and branding. It shares no code, design, or algorithms with any other digital-wellbeing app.

                ## Third-party licenses
                See "Open-Source Licenses" and "Third-Party Notices."
            """.trimIndent()
        ),
        LegalDocument(
            id = "legal-version",
            listTitle = "Legal document version / effective date",
            body = """
                # Legal Document Version & Effective Date

                ## Status of these documents
                Every document in this Legal Center is a draft prepared during development, for internal review — not a finalized, lawyer-reviewed, or published legal document. None of them should be treated as binding until reviewed by qualified legal counsel and formally published with a real effective date.

                ## Version
                Document set version: 0.2.0-draft
                Matches app version: 0.1.0 (versionCode 1)

                ## Effective date
                Not yet effective — no version of these documents has been published. This line will be replaced with a real date once these documents are reviewed and Orlune is released.

                ## Change history
                - 0.2.0-draft — expanded Privacy Policy (23 sections) and Terms of Service (28 sections) to their full requested structure; added app-picker/package-visibility disclosure throughout; clarified Orlune's own source-code license status as undecided (not open source); cross-referenced docs/legal-compliance-matrix.md and docs/google-play-privacy-compliance.md.
                - 0.1.0-draft — initial draft of all 15 documents, written to accurately describe the app's actual local-only implementation as of app version 0.1.0.
            """.trimIndent()
        )
    )

    fun byId(id: String): LegalDocument? = all.firstOrNull { it.id == id }
}
