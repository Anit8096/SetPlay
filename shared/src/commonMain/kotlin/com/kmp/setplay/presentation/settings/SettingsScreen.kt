package com.kmp.setplay.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
            SettingsRow(
                icon = Icons.Filled.DarkMode,
                title = "Appearance",
                subtitle = "Light / Dark / System — coming soon"
            )
            SettingsRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Match reminders and announcements — coming soon"
            )

            Spacer(Modifier.size(8.dp))

            Text(
                "SetPlay",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val disabled = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = disabled,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = disabled
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = disabled
                )
            }
        }
    }
}
