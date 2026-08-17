package com.orlune.app.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.orlune.app.feature.privacy.LegalCenterScreen
import com.orlune.app.feature.privacy.LegalDocumentScreen
import com.orlune.app.feature.privacy.PrivacyCenterScreen

private sealed class SettingsDestination {
    data object Root : SettingsDestination()
    data object PrivacyCenter : SettingsDestination()
    data object LegalCenter : SettingsDestination()
    data class LegalDocument(val id: String) : SettingsDestination()
}

/**
 * Owns Settings' sub-navigation (Root -> Privacy Center -> Legal Center -> a single
 * document) as a manual back-stack, matching this app's existing no-framework
 * navigation style (OrluneTab's simple enum switch) rather than pulling in
 * androidx.navigation-compose for what's still a small, linear stack.
 */
@Composable
fun SettingsSection(
    modifier: Modifier,
    themeMode: String,
    usageAccessGranted: Boolean,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    onThemeChange: (String) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onExport: () -> Unit,
    onDeleteRequest: () -> Unit,
    onResetRequest: () -> Unit,
    sessionCount: Int,
    dailyUsageCount: Int,
    ruleCount: Int,
    focusSessionCount: Int,
    knownAppCount: Int
) {
    val backStack = remember { mutableStateListOf<SettingsDestination>(SettingsDestination.Root) }
    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }

    when (val destination = backStack.last()) {
        SettingsDestination.Root -> SettingsScreen(
            modifier = modifier,
            themeMode = themeMode,
            usageAccessGranted = usageAccessGranted,
            overlayGranted = overlayGranted,
            notificationGranted = notificationGranted,
            onThemeChange = onThemeChange,
            onOpenUsageAccess = onOpenUsageAccess,
            onOpenOverlay = onOpenOverlay,
            onExport = onExport,
            onDeleteRequest = onDeleteRequest,
            onOpenPrivacyCenter = { backStack.add(SettingsDestination.PrivacyCenter) }
        )
        SettingsDestination.PrivacyCenter -> PrivacyCenterScreen(
            modifier = modifier,
            onBack = { backStack.removeAt(backStack.lastIndex) },
            sessionCount = sessionCount,
            dailyUsageCount = dailyUsageCount,
            ruleCount = ruleCount,
            focusSessionCount = focusSessionCount,
            knownAppCount = knownAppCount,
            usageAccessGranted = usageAccessGranted,
            overlayGranted = overlayGranted,
            notificationGranted = notificationGranted,
            onOpenUsageAccess = onOpenUsageAccess,
            onOpenOverlay = onOpenOverlay,
            onExport = onExport,
            onDeleteAllRequest = onDeleteRequest,
            onResetRequest = onResetRequest,
            onOpenLegalCenter = { backStack.add(SettingsDestination.LegalCenter) }
        )
        SettingsDestination.LegalCenter -> LegalCenterScreen(
            modifier = modifier,
            onBack = { backStack.removeAt(backStack.lastIndex) },
            onOpenDocument = { id -> backStack.add(SettingsDestination.LegalDocument(id)) }
        )
        is SettingsDestination.LegalDocument -> LegalDocumentScreen(
            modifier = modifier,
            documentId = destination.id,
            onBack = { backStack.removeAt(backStack.lastIndex) }
        )
    }
}
