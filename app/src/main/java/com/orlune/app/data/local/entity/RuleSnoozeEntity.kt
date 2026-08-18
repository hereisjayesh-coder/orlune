package com.orlune.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A temporary, per-package suppression of blocking enforcement — the block screen's
 * "Continue +10 min"/"+30 min"/Custom action. Deliberately separate from [RuleEntity]:
 * snoozing never edits, disables, or deletes the rule/schedule/focus session that
 * triggered the block, it only tells
 * [com.orlune.app.data.repository.BlockingRepository] to report ALLOW for
 * [packageName] until [snoozedUntil] (epoch millis) — usage accounting
 * (`UsageRepository.processNewEvents`) runs unconditionally every monitor tick,
 * completely independent of this table, so tracking is never paused by a snooze.
 * Repeated snoozes replace [snoozedUntil] outright (an upsert keyed on
 * [packageName]) rather than accumulating — "+10 min" always means "10 minutes from
 * now," not "10 minutes added to whatever's left."
 */
@Entity(tableName = "rule_snoozes")
data class RuleSnoozeEntity(
    @PrimaryKey val packageName: String,
    val snoozedUntil: Long
)
