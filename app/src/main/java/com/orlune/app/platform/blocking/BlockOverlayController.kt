package com.orlune.app.platform.blocking

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.orlune.app.MainActivity
import com.orlune.app.R
import com.orlune.app.data.repository.BlockingRepository.BlockReason
import com.orlune.app.ui.components.formatDuration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Draws/removes a full-screen [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 * block screen. Plain Android [View]s built programmatically, not Compose — hosting
 * Compose outside an Activity needs a hand-rolled `LifecycleOwner`/
 * `SavedStateRegistryOwner`/`ViewModelStoreOwner`, real complexity this phase doesn't
 * need. Every entry point is fail-safe: a missing overlay permission or a
 * `WindowManager` failure results in "no overlay shown," never a crash.
 *
 * Deliberately deep-red/near-black — visually distinct from the rest of Orlune's
 * black-and-gold language on purpose, the same way a system interruption (a phone's
 * own low-battery or emergency screens) reads as different from normal UI. The copy
 * shown is driven entirely by [BlockInfo.reason] (a real [BlockReason] value computed
 * by [com.orlune.app.data.repository.BlockingRepository.evaluate], never guessed here)
 * so a daily-limit block, a scheduled block, and an active-Focus block each say the
 * true thing, not one generic "app blocked" message.
 *
 * [onSnooze] is the only side effect a snooze button has: it just reports "the user
 * wants [BlockInfo.packageName] allowed for N more minutes" to the caller — this class
 * never touches Room or the blocking decision itself (see
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
    private val density = context.resources.displayMetrics.density

    fun isShowing(): Boolean = overlayView != null

    fun show(info: BlockInfo) {
        if (overlayView != null) return
        if (!OverlayPermission.isGranted(context)) return

        val view = buildOverlayView(info)
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

    private fun dp(value: Int): Int = (value * density).toInt()

    private fun buildOverlayView(info: BlockInfo): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(BG_COLOR)
            // This overlay manages its own explicit deep-red/black colors deliberately
            // — opt this whole hierarchy out of the platform's Force Dark remapping
            // (available since API 29, this app's minSdk) so a future system-theme
            // change can't alter colors on a window that isn't backed by an app theme.
            isForceDarkAllowed = false
            // Generous, non-edge padding rather than a computed system-bar inset: every
            // element here is centered content, never touching the true screen edge,
            // so there's nothing for the status/nav bar to visually collide with —
            // the same reasoning `orluneSafeAreaPadding()` documents for Compose
            // screens, applied by construction instead of an inset listener here since
            // this window isn't part of that Compose tree.
            setPadding(dp(24), dp(48), dp(24), dp(48))
        }

        val scroll = android.widget.ScrollView(context).apply {
            isFillViewport = true
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        content.addView(orluneMark())
        content.addView(verticalSpacer(20))

        val (headline, bodyLines) = copyFor(info)
        content.addView(
            TextView(context).apply {
                text = headline
                setTextColor(TEXT_PRIMARY)
                textSize = 30f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                letterSpacing = 0.02f
            }
        )
        content.addView(verticalSpacer(20))

        content.addView(appRow(info))
        content.addView(verticalSpacer(16))

        bodyLines.forEach { line ->
            content.addView(
                TextView(context).apply {
                    text = line
                    setTextColor(TEXT_MUTED)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(2), dp(8), dp(2))
                }
            )
        }
        content.addView(verticalSpacer(32))

        content.addView(primaryButton("Close ${info.label}") { openOrlune(openFocusFor = null) })
        content.addView(verticalSpacer(12))
        content.addView(secondaryButton("Start Focus") { openOrlune(openFocusFor = info.packageName) })

        // A snooze always exists as an escape hatch, regardless of reason — same
        // underlying override for a limit, a schedule, or a Focus session (see
        // BlockingRepository.evaluate's snooze-overrides-any-BLOCK behavior) — but it's
        // visually secondary to Close/Start Focus, the two "intentional" choices.
        content.addView(verticalSpacer(28))
        content.addView(
            TextView(context).apply {
                text = "Or continue anyway. Your usage will still be counted."
                setTextColor(TEXT_MUTED)
                textSize = 13f
                gravity = Gravity.CENTER
            }
        )
        content.addView(verticalSpacer(10))
        content.addView(snoozeRow(info.packageName))

        val customRow = customSnoozeRow(info.packageName)
        customRow.visibility = View.GONE
        content.addView(verticalSpacer(10))
        content.addView(customToggle(customRow))
        content.addView(customRow)

        scroll.addView(
            content,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        root.addView(scroll, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        return root
    }

    /** The true reason for the block, in the user's own words — see this class's KDoc
     * for why this is never a guess. Falls back to a generic-but-still-honest line only
     * when [BlockInfo.reason] is somehow null (defensive; [BlockingMonitorService] only
     * calls [show] when the last evaluated decision was actually BLOCK). */
    private fun copyFor(info: BlockInfo): Pair<String, List<String>> = when (val reason = info.reason) {
        is BlockReason.DailyLimit -> "LIMIT REACHED" to listOf(
            "You've reached your limit for today.",
            "You've used ${formatDuration(reason.usedSeconds)} of ${info.label} today.",
            "Come back tomorrow, or choose an intentional option below."
        )
        is BlockReason.Focus -> "FOCUS ACTIVE" to listOf(
            "Focus is active until ${formatClockMillis(reason.activeUntilMillis)}.",
            "${info.label} is paused until then."
        )
        is BlockReason.Schedule -> "RESTRICTED NOW" to listOf(
            "${info.label} is restricted right now.",
            "This restriction ends at ${formatClockTime(reason.endTime)}."
        )
        BlockReason.Restricted -> "APP RESTRICTED" to listOf(
            "${info.label} is restricted by your rules."
        )
        null -> "LIMIT REACHED" to listOf("${info.label} is restricted right now.")
    }

    private fun formatClockMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().let(::formatLocalTime)

    /** [hhMm] is [com.orlune.app.data.local.entity.ScheduleEntity.endTime]'s stored
     * "HH:mm" shape — malformed/unparseable input (shouldn't happen; validated at
     * creation) falls back to showing the raw value rather than crashing the overlay. */
    private fun formatClockTime(hhMm: String): String =
        runCatching { formatLocalTime(LocalTime.parse(hhMm)) }.getOrDefault(hhMm)

    private fun formatLocalTime(time: LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    private fun openOrlune(openFocusFor: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (openFocusFor != null) {
                putExtra(MainActivity.EXTRA_OPEN_FOCUS_FOR_PACKAGE, openFocusFor)
            }
        }
        context.startActivity(intent)
        hide()
    }

    private fun orluneMark(): View = ImageView(context).apply {
        setImageResource(R.mipmap.ic_launcher_monochrome)
        setColorFilter(ACCENT)
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
    }

    private fun appRow(info: BlockInfo): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        val icon = info.icon
        if (icon != null) {
            addView(
                ImageView(context).apply {
                    setImageBitmap(icon)
                    layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
                    contentDescription = info.label
                }
            )
            addView(horizontalSpacer(10))
        }
        addView(
            TextView(context).apply {
                text = info.label
                setTextColor(TEXT_PRIMARY)
                textSize = 18f
            }
        )
    }

    private fun primaryButton(label: String, onClick: () -> Unit): View =
        Button(context).apply {
            text = label
            setTextColor(Color.WHITE)
            background = pillDrawable(ACCENT)
            setPadding(dp(24), dp(14), dp(24), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = dp(260)
            }
            setOnClickListener { onClick() }
        }

    private fun secondaryButton(label: String, onClick: () -> Unit): View =
        Button(context).apply {
            text = label
            setTextColor(TEXT_PRIMARY)
            background = pillDrawable(BUTTON_BG, strokeColor = ACCENT)
            setPadding(dp(24), dp(14), dp(24), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = dp(260)
            }
            setOnClickListener { onClick() }
        }

    private fun snoozeRow(packageName: String): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        addView(smallButton("Continue +10 min") { onSnooze(packageName, 10); hide() })
        addView(horizontalSpacer(12))
        addView(smallButton("Continue +30 min") { onSnooze(packageName, 30); hide() })
    }

    private fun customToggle(target: View): View = TextView(context).apply {
        text = "Custom"
        setTextColor(ACCENT)
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setOnClickListener { target.visibility = if (target.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
    }

    private fun smallButton(label: String, onClick: () -> Unit): View =
        Button(context).apply {
            text = label
            setTextColor(TEXT_PRIMARY)
            textSize = 13f
            background = pillDrawable(BUTTON_BG, strokeColor = OUTLINE)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { onClick() }
        }

    /** No free-text entry (same reasoning as [com.orlune.app.ui.components.IntStepper]):
     * a bounded +/- stepper around a default, confirmed with its own "Continue" button. */
    private fun customSnoozeRow(packageName: String): View {
        var customMinutes = DEFAULT_CUSTOM_SNOOZE_MINUTES
        val valueView = TextView(context).apply {
            text = "$customMinutes min"
            setTextColor(TEXT_PRIMARY)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp(16), 0, dp(16), 0)
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
            addView(smallButton("−") {
                customMinutes = (customMinutes - CUSTOM_SNOOZE_STEP_MINUTES).coerceAtLeast(MIN_CUSTOM_SNOOZE_MINUTES)
                valueView.text = "$customMinutes min"
            })
            addView(valueView)
            addView(smallButton("+") {
                customMinutes = (customMinutes + CUSTOM_SNOOZE_STEP_MINUTES).coerceAtMost(MAX_CUSTOM_SNOOZE_MINUTES)
                valueView.text = "$customMinutes min"
            })
            addView(horizontalSpacer(8))
            addView(smallButton("Continue") { onSnooze(packageName, customMinutes); hide() })
        }
    }

    private fun pillDrawable(fillColor: Int, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(28).toFloat()
            setColor(fillColor)
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun horizontalSpacer(dpValue: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(dpValue), 0)
    }

    private fun verticalSpacer(dpValue: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(dpValue))
    }

    private companion object {
        private const val TAG = "BlockOverlay"
        private const val DEFAULT_CUSTOM_SNOOZE_MINUTES = 15
        private const val CUSTOM_SNOOZE_STEP_MINUTES = 5
        private const val MIN_CUSTOM_SNOOZE_MINUTES = 5
        private const val MAX_CUSTOM_SNOOZE_MINUTES = 120

        // Deliberately its own palette, not ui/theme/Color.kt's black-and-gold —
        // see this class's KDoc for why this one screen reads as different on purpose.
        private const val BG_COLOR = 0xFF160505.toInt()
        private const val TEXT_PRIMARY = 0xFFF5EDEA.toInt()
        private const val TEXT_MUTED = 0xFFC49C96.toInt()
        private const val ACCENT = 0xFFD5453A.toInt()
        private const val BUTTON_BG = 0xFF2B1210.toInt()
        private const val OUTLINE = 0xFF4A2624.toInt()
    }
}
