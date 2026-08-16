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
 */
class BlockOverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun isShowing(): Boolean = overlayView != null

    fun show(blockedAppLabel: String) {
        if (overlayView != null) return
        if (!OverlayPermission.isGranted(context)) return

        val view = buildOverlayView(blockedAppLabel)
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

    private fun buildOverlayView(blockedAppLabel: String): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
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
            Button(context).apply {
                text = "Go to Orlune"
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

    private companion object {
        private const val TAG = "BlockOverlay"
    }
}
