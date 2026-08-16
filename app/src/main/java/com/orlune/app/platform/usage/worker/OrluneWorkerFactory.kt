package com.orlune.app.platform.usage.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.orlune.app.data.repository.UsageRepository

/**
 * Manual DI for WorkManager Workers (Phase 0 Section 2: manual constructor injection
 * until the object graph outgrows it) — WorkManager can only default-construct
 * no-arg Workers on its own, so a WorkerFactory is the standard way to hand a Worker
 * its real dependencies.
 */
class OrluneWorkerFactory(private val usageRepository: UsageRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        UsageAggregationWorker::class.java.name ->
            UsageAggregationWorker(appContext, workerParameters, usageRepository)
        else -> null
    }
}
