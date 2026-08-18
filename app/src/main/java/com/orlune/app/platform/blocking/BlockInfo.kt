package com.orlune.app.platform.blocking

import android.graphics.Bitmap
import com.orlune.app.data.repository.BlockingRepository

/**
 * Everything the block screen needs to render, resolved once by the caller
 * ([BlockingMonitorService]) so [BlockOverlayController] itself stays free of
 * Room/PackageManager access — same "resolve once, pass a plain value down" shape as
 * [com.orlune.app.platform.usage.AppDisplayInfo] elsewhere in the codebase. [reason] is
 * the exact same [BlockingRepository.BlockReason] [BlockingRepository.evaluate] already
 * computed for this tick — never re-derived, so the copy shown can never disagree with
 * the actual decision that triggered it.
 */
data class BlockInfo(
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
    val reason: BlockingRepository.BlockReason?
)
