package com.orlune.app

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.orlune.app.data.local.OrluneDatabase
import com.orlune.app.data.repository.UsageRepository
import com.orlune.app.platform.usage.AppLabelResolver
import com.orlune.app.platform.usage.UsageEventReader
import com.orlune.app.platform.usage.worker.OrluneWorkerFactory
import com.orlune.app.platform.usage.worker.UsageAggregationWorker
import java.util.concurrent.TimeUnit

class OrluneApplication : Application(), Configuration.Provider {

    val database: OrluneDatabase by lazy {
        Room.databaseBuilder(this, OrluneDatabase::class.java, OrluneDatabase.DATABASE_NAME).build()
    }

    val usageRepository: UsageRepository by lazy {
        UsageRepository(
            usageEventSource = UsageEventReader(this),
            appLabelSource = AppLabelResolver(this),
            appDao = database.appDao(),
            sessionDao = database.sessionDao(),
            dailyUsageDao = database.dailyUsageDao(),
            userPreferenceDao = database.userPreferenceDao()
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(OrluneWorkerFactory(usageRepository))
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicUsageAggregation()
    }

    /**
     * 15 minutes is WorkManager's minimum periodic interval — there's no
     * blocking-latency requirement in this phase to justify anything tighter (see
     * UsageAggregationWorker). KEEP means an already-enqueued schedule survives app
     * upgrades/reboots without being reset.
     */
    private fun schedulePeriodicUsageAggregation() {
        val request = PeriodicWorkRequestBuilder<UsageAggregationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UsageAggregationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
