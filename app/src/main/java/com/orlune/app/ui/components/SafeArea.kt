package com.orlune.app.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Status bar + navigation bar + display cutout, deliberately excluding the IME
 * inset (`WindowInsets.safeDrawing` would also chase the keyboard, which isn't what
 * a screen title/content region needs — only its own text field should do that).
 *
 * Required because `MainActivity` calls `enableEdgeToEdge()`: content in this app
 * draws behind the system bars by default, so anything not routed through
 * `Scaffold` (which already applies `WindowInsets.safeDrawing` to its `innerPadding`
 * automatically) must claim this space back itself, or its top/bottom edge renders
 * underneath the status bar / navigation bar. Real bug found on-device (Pixel 7a):
 * onboarding's title text rendered directly behind the status bar clock/icons before
 * this was applied, since `OnboardingSection` is composed as a `Surface` sibling to
 * the tab `Scaffold` in `OrluneRoot.kt`, not inside it.
 *
 * Apply this **once**, at the outermost container of any composable tree rendered
 * outside `Scaffold` (a full-screen overlay, a wizard, a future modal flow) — not
 * inside individual screens within that tree, which would double the padding for
 * composables (like `AppPickerScreen`/`LegalCenterScreen`) that are also reachable
 * from inside `Scaffold`, where the same space is already accounted for.
 */
@Composable
fun Modifier.orluneSafeAreaPadding(): Modifier =
    this.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
