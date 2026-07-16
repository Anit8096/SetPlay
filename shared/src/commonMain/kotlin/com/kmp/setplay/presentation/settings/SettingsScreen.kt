package com.kmp.setplay.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.presentation.common.ContentContainer

/**
 * Settings shell.
 *
 * Deliberately thin: the theme toggle and notification prefs both need persistence
 * (DataStore/Settings) plus a theme wrapper around `MaterialTheme` in App.kt, neither of
 * which exists yet — the app currently runs on bare `MaterialTheme {}` defaults. Wiring
 * those is Step 15's job. This exists now so the Profile → Settings row lands somewhere
 * real instead of on a dead route.
 *
 * The TopAppBar/back button are owned by MainAppNavigation's Scaffold via TopBarSpec,
 * so this screen renders content only.
 */
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    ContentContainer(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Material3 segmented list group — 2dp gaps between segments.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Appearance",
                    subtitle = "Light / Dark / System — coming soon",
                    index = 0,
                    count = 2
                )
                SettingsRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Match reminders and announcements — coming soon",
                    index = 1,
                    count = 2
                )
            }

            Spacer(Modifier.size(8.dp))

            Text(
                "SetPlay",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Both rows are Step 15 placeholders, so they render through the disabled state of
 * [SegmentedListItem] (enabled = false handles the greyed-out content automatically).
 * When theme/notification prefs land, flip enabled and wire onClick.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    index: Int,
    count: Int
) {
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        enabled = false,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        },
        supportingContent = { Text(subtitle) }
    ) {
        Text(title, fontWeight = FontWeight.Medium)
    }
}
