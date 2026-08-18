package com.orlune.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.orlune.app.data.local.entity.ThemePreferenceEntity
import com.orlune.app.ui.OrluneRoot
import com.orlune.app.ui.theme.OrluneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as OrluneApplication
        // Read once at cold start only — the block screen's "Start Focus" button is a
        // quick-action into an already-running app, not a deep-link contract this
        // Activity needs to keep re-observing via onNewIntent; see OrluneRoot's use of
        // this value for why a one-shot initial value is an intentional, minimal scope.
        val openFocusForPackage = intent?.getStringExtra(EXTRA_OPEN_FOCUS_FOR_PACKAGE)
        setContent {
            val themePreference by app.database.themePreferenceDao()
                .observe()
                .collectAsState(initial = ThemePreferenceEntity(themeId = "system"))
            OrluneTheme(themeMode = themePreference?.themeId ?: "system") {
                OrluneRoot(app, openFocusForPackage = openFocusForPackage)
            }
        }
    }

    companion object {
        /** String extra: a package name to pre-select in Focus and switch straight to
         * the Focus tab for — set by [com.orlune.app.platform.blocking.BlockOverlayController]'s
         * "Start Focus" button so choosing it from the block screen doesn't dump the
         * user back on Home with an extra manual step. */
        const val EXTRA_OPEN_FOCUS_FOR_PACKAGE = "com.orlune.app.OPEN_FOCUS_FOR_PACKAGE"
    }
}
