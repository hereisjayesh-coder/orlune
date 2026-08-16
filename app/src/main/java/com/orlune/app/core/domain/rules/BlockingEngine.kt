package com.orlune.app.core.domain.rules

import com.orlune.app.data.local.entity.AppListEntryEntity

enum class BlockDecision { ALLOW, BLOCK }

enum class AppListType { BLOCK, ALLOW }

/** Maps [AppListEntryEntity.listType]'s raw "block"/"allow" string to [AppListType]; null if unrecognized. */
fun AppListEntryEntity.listType(): AppListType? = when (listType) {
    "block" -> AppListType.BLOCK
    "allow" -> AppListType.ALLOW
    else -> null
}

/**
 * Combines rule and allow/block-list outcomes into one decision for a single app,
 * per `docs/phase-0-research.md` Section 9 (BlockingEngine: "precedence rules across
 * overlapping active rules").
 *
 * Precedence (a real design decision, not schema-derived): an explicit ALLOW list
 * entry always wins — this is the essential-app exemption mechanism (Section 13) and
 * must override any triggered rule. An explicit BLOCK list entry wins next,
 * regardless of rule state. Otherwise the decision follows whether any rule targeting
 * this app is currently triggered.
 *
 * [AppListEntryEntity]'s primary key is `(packageName, listType)`, so a single package
 * can legally have both a "block" row and an "allow" row at once (e.g. a stale block
 * entry left behind after the app was later marked essential). Precedence is decided
 * by entry *type*, not by which row happens to come first in [appListEntries] — a
 * `firstOrNull` here previously let list order silently override the documented
 * ALLOW-wins precedence.
 */
object BlockingEngine {

    fun decide(
        packageName: String,
        appListEntries: List<AppListEntryEntity>,
        anyRuleTriggered: Boolean
    ): BlockDecision {
        val typesForPackage = appListEntries
            .asSequence()
            .filter { it.packageName == packageName }
            .mapNotNull { it.listType() }
            .toSet()
        return when {
            AppListType.ALLOW in typesForPackage -> BlockDecision.ALLOW
            AppListType.BLOCK in typesForPackage -> BlockDecision.BLOCK
            anyRuleTriggered -> BlockDecision.BLOCK
            else -> BlockDecision.ALLOW
        }
    }
}
