package com.orlune.app.platform.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.orlune.app.OrluneApplication
import com.orlune.app.R
import com.orlune.app.core.domain.rules.BlockDecision
import com.orlune.app.platform.usage.UsageAccessPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground-service polling loop — the platform's own documented mechanism for
 * enforcement without AccessibilityService (`docs/android-platform-capabilities.md`).
 * Only ever runs while needed: started explicitly (debug UI) or automatically at app
 * cold start when a rule already exists (see [OrluneApplication]), and self-stops the
 * moment there's nothing left to enforce or a required permission is gone — never an
 * unconditional forever-loop.
 */
class BlockingMonitorService : Service() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private lateinit var overlayController: BlockOverlayController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlayController = BlockOverlayController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                // FOREGROUND_SERVICE_TYPE_SPECIAL_USE didn't exist before API 34 — passing
                // it on older OS versions can throw "Invalid foreground service type".
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            // Fail safe: if the OS refuses to promote us to foreground for any reason,
            // stop rather than run an unpromoted/likely-to-be-killed service silently.
            stopSelf()
            return START_NOT_STICKY
        }
        startMonitoringLoop()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job.cancel()
        overlayController.hide()
        super.onDestroy()
    }

    private fun startMonitoringLoop() {
        if (job.children.any { it.isActive }) return // already running
        scope.launch {
            val app = application as OrluneApplication
            while (isActive) {
                tick(app)
                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    private suspend fun tick(app: OrluneApplication) {
        try {
            if (!UsageAccessPermission.isGranted(applicationContext)) {
                stopSelf()
                return
            }
            if (app.database.ruleDao().observeAll().first().isEmpty()) {
                stopSelf()
                return
            }

            app.usageRepository.processNewEvents()
            val outcome = app.blockingRepository.evaluate()
            Log.d(TAG, "tick: foreground=${outcome.packageName} decision=${outcome.decision}")

            val packageName = outcome.packageName
            when {
                outcome.decision == BlockDecision.BLOCK && packageName != null -> {
                    val label = app.database.appDao().get(packageName)?.label ?: packageName
                    // WindowManager.addView/removeView require a thread with a Looper.
                    withContext(Dispatchers.Main) { overlayController.show(label) }
                }
                else -> withContext(Dispatchers.Main) { overlayController.hide() }
            }
        } catch (e: Exception) {
            // Fail safe: skip this tick rather than crash the service or leave a stuck overlay.
            Log.w(TAG, "tick failed, skipping", e)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App blocking",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Shows while Orlune is actively enforcing a rule"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Orlune is active")
            .setContentText("Enforcing your app rules")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    companion object {
        private const val TAG = "BlockingMonitor"
        private const val CHANNEL_ID = "blocking_monitor"
        private const val NOTIFICATION_ID = 1001

        /** Within the platform doc's 2-5s target latency window while monitoring is active. */
        private const val POLL_INTERVAL_MILLIS = 3_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, BlockingMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BlockingMonitorService::class.java))
        }
    }
}
