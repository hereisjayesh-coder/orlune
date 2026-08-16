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
 */
object BlockingEngine {

    fun decide(
        packageName: String,
        appListEntries: List<AppListEntryEntity>,
        anyRuleTriggered: Boolean
    ): BlockDecision {
        val entryForPackage = appListEntries.firstOrNull { it.packageName == packageName }
        return when (entryForPackage?.listType()) {
            AppListType.ALLOW -> BlockDecision.ALLOW
            AppListType.BLOCK -> BlockDecision.BLOCK
            null -> if (anyRuleTriggered) BlockDecision.BLOCK else BlockDecision.ALLOW
        }
    }
}
