package com.orlune.app.feature.privacy.legal

/**
 * [body] uses a small self-parsed subset of markdown (see LegalDocumentBody): lines
 * starting with "# " are the document title, "## " are section headers, "- " are
 * bullet items, blank lines separate paragraphs. No markdown library dependency —
 * deliberately, per this project's no-unnecessary-dependency rule.
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
     * real, confirmed values from legal counsel. */
    const val DOCUMENT_SET_VERSION = "0.1.0-draft"
    const val MATCHES_APP_VERSION = "0.1.0"

    val all: List<LegalDocument> = listOf(
        LegalDocument(
            id = "privacy-policy",
            listTitle = "Privacy Policy",
            body = """
                # Privacy Policy

                ## Status
                This is a draft privacy policy for internal review. It has not been reviewed by a lawyer and is not yet published as Orlune's binding privacy policy. Do not rely on it as a final legal document.

                ## Who this document is about
                Orlune is developed by [LEGAL ENTITY NAME — TBD], a [ENTITY TYPE — TBD] registered in [JURISDICTION — TBD], with a registered address at [REGISTERED ADDRESS — TBD]. Questions about this policy can be sent to [CONTACT EMAIL — TBD].

                ## What Orlune does not do
                - No account, login, or signup of any kind
                - No cloud service or backend server operated by Orlune
                - No advertising SDK, no advertising identifiers collected
                - No analytics SDK, no crash-reporting SDK
                - No behavioral tracking of any kind
                - No artificial intelligence or machine learning processing of your data
                - No sale or sharing of data with any third party, because no data leaves the device through Orlune

                ## What Orlune measures, and where it happens
                Orlune reads Android's on-device Usage Access API (UsageStatsManager) to see which apps you open and for how long. This processing happens entirely on your device, inside the Orlune app process. Orlune does not operate any server, so there is nowhere for this information to be sent even if the app wanted to send it — the installed app does not request Android's INTERNET permission, and cannot make network requests as a result.

                ## What is stored, and where
                See "Data Collection & Permissions" and "Data Storage & Retention" for the full technical detail. In summary: derived usage sessions, daily usage totals, the rules/schedules/focus sessions you create, your appearance preference, and a small internal processing marker are stored in a local database on your device, in Orlune's private app storage. Nothing here is synced, backed up to any cloud service, or accessible to other apps.

                ## Your choices
                You can export a full copy of everything Orlune has stored (see "Data Export & Deletion"), delete all of it at any time from within the app, and revoke any permission from Android Settings at any time. Uninstalling Orlune removes all of its local data from your device.

                ## Limits of this claim
                Orlune's own code does not transmit data anywhere. This document describes what Orlune's software does. It cannot make guarantees about the security of your physical device, your Android OS, or actions outside Orlune's control — for example, a device backup tool you use independently, or physical access to an unlocked device.

                ## Changes to this policy
                If this policy changes after Orlune is released, the new version and its effective date will be shown here, and the version history will be kept in "Legal document version / effective date".
            """.trimIndent()
        ),
        LegalDocument(
            id = "terms-of-service",
            listTitle = "Terms of Service",
            body = """
                # Terms of Service

                ## Status
                Draft, not yet legally reviewed or published. Placeholder terms for internal development use.

                ## Agreement
                By installing and using Orlune, published by [LEGAL ENTITY NAME — TBD], you agree to these terms. If you do not agree, do not use the app.

                ## What Orlune is
                Orlune is a local, offline digital wellbeing application for Android. It helps you observe your own app usage and optionally set limits, schedules, and focus sessions to reduce distracting use. There is no account system — these terms apply to your use of the software itself, not to any service, since Orlune operates no service.

                ## License to use the app
                Subject to these terms, you are granted a personal, non-exclusive, non-transferable, revocable license to install and use Orlune on devices you own or control, for your own personal use.

                ## No warranty
                Orlune is provided "as is." To the maximum extent permitted by [JURISDICTION — TBD] law, Orlune and [LEGAL ENTITY NAME — TBD] disclaim all warranties, express or implied, including fitness for a particular purpose. See "Wellness / Product Disclaimer" for specific limitations around app-blocking reliability.

                ## Limitation of liability
                To the maximum extent permitted by law, [LEGAL ENTITY NAME — TBD] is not liable for indirect, incidental, or consequential damages arising from use of the app, including missed deadlines, lost productivity, or distress arising from blocking behavior working differently than expected.

                ## Changes and termination
                You may stop using Orlune and uninstall it at any time. [LEGAL ENTITY NAME — TBD] may update these terms; continued use after an update constitutes acceptance of the revised terms.

                ## Governing law
                These terms are governed by the laws of [JURISDICTION — TBD], without regard to conflict-of-law principles, pending confirmation by legal counsel.
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
                Orlune, including its source code, design, and branding, is and remains the property of [LEGAL ENTITY NAME — TBD]. This Agreement does not transfer any ownership rights to you.

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
            id = "open-source-licenses",
            listTitle = "Open-Source Licenses",
            body = """
                # Open-Source Licenses

                ## Status
                This is a manually-maintained summary, not an automatically generated license report. Before commercial release, this list should be regenerated from the actual dependency tree to guarantee completeness and exact version/license accuracy.

                ## Major open-source components as of this document's effective date
                - Kotlin and Kotlin Coroutines — Apache License 2.0 — JetBrains / Kotlin Foundation
                - AndroidX Jetpack libraries (Core, Lifecycle, Activity, Compose UI, Compose Material 3, Room, WorkManager) — Apache License 2.0 — The Android Open Source Project
                - Material Components icon set (material-icons-extended) — Apache License 2.0 — Google

                Orlune adds no dependency outside the AndroidX/Jetpack and Kotlin ecosystems.

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
                Any third-party trademarks that may appear in Orlune (for example, "Android," or the names of apps shown in your own usage data) belong to their respective owners and are used only descriptively, to identify the apps you have installed — not to imply endorsement or affiliation.
            """.trimIndent()
        ),
        LegalDocument(
            id = "wellness-disclaimer",
            listTitle = "Wellness / Product Disclaimer",
            body = """
                # Wellness / Product Disclaimer

                ## Not a medical or therapeutic product
                Orlune is a self-directed screen-time tool, not a medical device, therapeutic product, or mental-health treatment. It is not a substitute for professional advice regarding addiction, mental health, or behavioral concerns. If you are experiencing distress related to technology use, please consult a qualified professional.

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
                Draft. Orlune's target audience, age rating, and any child-directed-service determination (for example, under COPPA in the United States, or equivalent rules elsewhere) have not yet been finalized as of this document's effective date. This section must be completed with legal input before release, particularly given Orlune's screen-time/parental-interest subject matter.

                ## Current design-level facts relevant to this determination
                Orlune has no account system and collects no directly-identifying information (name, email, birthdate) at any point, from any user, of any age — because it has no account system at all. All processing described in "Data Collection & Permissions" happens locally regardless of who is using the device.

                ## What is not yet decided
                - Whether Orlune will be marketed or positioned as suitable for use on a device primarily used by a child or teen
                - Whether a specific children's-privacy compliance statement is required, and if so, its content
                - The app's eventual Google Play content rating

                ## Placeholder statement
                Until the above is finalized, Orlune should not be represented as verified-compliant with any children's privacy law. [LEGAL REVIEW REQUIRED — TBD]
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
                Some jurisdictions (for example, India's IT Rules, or the EU's GDPR) require a named grievance officer or regional representative with published contact details. This has not yet been designated: [LEGAL ENTITY NAME — TBD] / [GRIEVANCE OFFICER NAME — TBD] / [GRIEVANCE OFFICER CONTACT — TBD].

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
                Orlune is a free, local-only, no-account Android digital wellbeing app. It helps you understand your own screen time and reduce distracting app usage, entirely on your device — no server, no account, no ads, no analytics, no AI.

                ## Current version
                App version 0.1.0 (versionCode 1) — pre-release, under active development. Not yet published to Google Play.

                ## What's implemented today
                Usage monitoring, deterministic limit/schedule rule enforcement, app blocking via an overlay, one-time focus sessions, local JSON export, and delete-all-data controls, across System/Light/Dark appearance modes.

                ## What's intentionally not built yet
                Website/VPN blocking, Android's AccessibilityService-based detection, a dedicated onboarding flow, recurring focus-session scheduling, and any analytics, recommendation, or AI/ML feature — all deliberately deferred, not overlooked.

                ## Original work
                Orlune is built from original design, code, and branding. It shares no code, design, or algorithms with any other digital-wellbeing app.
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
                Document set version: 0.1.0-draft
                Matches app version: 0.1.0 (versionCode 1)

                ## Effective date
                Not yet effective — no version of these documents has been published. This line will be replaced with a real date once these documents are reviewed and Orlune is released.

                ## Change history
                - 0.1.0-draft — initial draft of all 15 documents, written to accurately describe the app's actual local-only implementation as of app version 0.1.0.
            """.trimIndent()
        )
    )

    fun byId(id: String): LegalDocument? = all.firstOrNull { it.id == id }
}
