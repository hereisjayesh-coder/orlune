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
        setContent {
            val themePreference by app.database.themePreferenceDao()
                .observe()
                .collectAsState(initial = ThemePreferenceEntity(themeId = "system"))
            OrluneTheme(themeMode = themePreference?.themeId ?: "system") {
                OrluneRoot(app)
            }
        }
    }
}
