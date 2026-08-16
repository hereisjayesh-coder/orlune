package com.orlune.app.platform.usage.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orlune.app.data.repository.UsageRepository
import com.orlune.app.platform.usage.UsageAccessPermission

/**
 * Periodic background aggregation (Phase 3) — deliberately not a foreground service
 * or tight polling loop, since nothing in this phase needs blocking-latency response
 * times (see `docs/android-platform-capabilities.md`'s architectural recommendation;
 * that tighter loop is Phase 5/6's concern).
 */
class UsageAggregationWorker(
    context: Context,
    params: WorkerParameters,
    private val usageRepository: UsageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!UsageAccessPermission.isGranted(applicationContext)) {
            // Nothing to do until the user grants Usage Access in Settings — this is
            // an expected idle state, not a failure.
            return Result.success()
        }
        return try {
            usageRepository.processNewEvents()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "usage_aggregation"
    }
}
