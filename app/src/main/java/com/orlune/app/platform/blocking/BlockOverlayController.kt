package com.orlune.app.platform.blocking

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.orlune.app.MainActivity

/**
 * Draws/removes a full-screen [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * block screen. Plain Android [View]s built programmatically, not Compose — hosting
 * Compose outside an Activity needs a hand-rolled `LifecycleOwner`/
 * `SavedStateRegistryOwner`/`ViewModelStoreOwner`, real complexity this phase doesn't
 * need. Every entry point is fail-safe: a missing overlay permission or a
 * `WindowManager` failure results in "no overlay shown," never a crash.
 *
 * [onSnooze] is the only side effect a snooze button has: it just reports "the user
 * wants [packageName] allowed for N more minutes" to the caller — this class never
 * touches Room or the blocking decision itself (see
 * [com.orlune.app.data.repository.RuleRepository.snooze] /
 * [com.orlune.app.data.repository.BlockingRepository] for where that actually happens).
 * The overlay hides itself immediately after a snooze tap rather than waiting for the
 * next monitor tick (up to [BlockingMonitorService]'s poll interval later) to notice
 * the decision flipped to ALLOW — a few seconds of stale overlay wouldn't feel like a
 * real button press.
 */
class BlockOverlayController(
    private val context: Context,
    private val onSnooze: (packageName: String, minutes: Int) -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun isShowing(): Boolean = overlayView != null

    fun show(packageName: String, blockedAppLabel: String) {
        if (overlayView != null) return
        if (!OverlayPermission.isGranted(context)) return

        val view = buildOverlayView(packageName, blockedAppLabel)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            0,
            PixelFormat.OPAQUE
        )
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // Fail safe: worst case is no overlay drawn this tick, not a crash.
            Log.w(TAG, "failed to add overlay window", e)
        }
    }

    fun hide() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            // View already detached (e.g. permission revoked mid-display) — ignore.
            Log.w(TAG, "failed to remove overlay window", e)
        }
    }

    private fun buildOverlayView(packageName: String, blockedAppLabel: String): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            // This overlay manages its own explicit black/white colors deliberately —
            // opt this whole hierarchy out of the platform's Force Dark remapping
            // (available since API 29, this app's minSdk) so a future system-theme
            // change can't alter colors on a window that isn't backed by an app theme.
            isForceDarkAllowed = false
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        content.addView(
            TextView(context).apply {
                text = "$blockedAppLabel is blocked right now"
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(48, 0, 48, 48)
            }
        )

        content.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                addView(Button(context).apply {
                    text = "+10 min"
                    setOnClickListener { onSnooze(packageName, 10); hide() }
                })
                addView(horizontalSpacer())
                addView(Button(context).apply {
                    text = "+30 min"
                    setOnClickListener { onSnooze(packageName, 30); hide() }
                })
            }
        )

        content.addView(buildCustomSnoozeRow(packageName))

        content.addView(verticalSpacer())
        content.addView(
            Button(context).apply {
                text = "Leave"
                setOnClickListener {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        )

        root.addView(
            content,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        return root
    }

    /** No free-text entry (same reasoning as [com.orlune.app.ui.components.IntStepper]):
     * a bounded +/- stepper around a default, confirmed with its own "Snooze" button. */
    private fun buildCustomSnoozeRow(packageName: String): View {
        var customMinutes = DEFAULT_CUSTOM_SNOOZE_MINUTES
        val valueView = TextView(context).apply {
            text = "$customMinutes min"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0)
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
            addView(Button(context).apply {
                text = "-"
                setOnClickListener {
                    customMinutes = (customMinutes - CUSTOM_SNOOZE_STEP_MINUTES).coerceAtLeast(MIN_CUSTOM_SNOOZE_MINUTES)
                    valueView.text = "$customMinutes min"
                }
            })
            addView(valueView)
            addView(Button(context).apply {
                text = "+"
                setOnClickListener {
                    customMinutes = (customMinutes + CUSTOM_SNOOZE_STEP_MINUTES).coerceAtMost(MAX_CUSTOM_SNOOZE_MINUTES)
                    valueView.text = "$customMinutes min"
                }
            })
            addView(Button(context).apply {
                text = "Snooze"
                setOnClickListener {
                    onSnooze(packageName, customMinutes)
                    hide()
                }
            })
        }
    }

    private fun horizontalSpacer(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(32, 0)
    }

    private fun verticalSpacer(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 32)
    }

    private companion object {
        private const val TAG = "BlockOverlay"
        private const val DEFAULT_CUSTOM_SNOOZE_MINUTES = 15
        private const val CUSTOM_SNOOZE_STEP_MINUTES = 5
        private const val MIN_CUSTOM_SNOOZE_MINUTES = 5
        private const val MAX_CUSTOM_SNOOZE_MINUTES = 120
    }
}
