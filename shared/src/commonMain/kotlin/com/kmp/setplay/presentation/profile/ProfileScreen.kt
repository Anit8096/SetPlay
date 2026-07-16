package com.kmp.setplay.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kmp.setplay.domain.repository.CurrentUser
import com.kmp.setplay.presentation.auth.AuthAction
import com.kmp.setplay.presentation.common.ContentContainer
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    onAuthAction: (AuthAction) -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val vm: ProfileViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.onAction(ProfileAction.MessageShown)
        }
    }

    fun dispatch(action: ProfileAction) {
        if (vm.onAction(action)) {
            onAuthAction(AuthAction.SignOut)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { _ ->
        ContentContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            val user = state.user

            when {
                state.isLoading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LoadingIndicator()
                }

                user == null -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        "Not signed in",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ProfileHeader(user)

                    PlayerIdRow(
                        userId = user.id,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(user.id))
                            dispatch(ProfileAction.CopyPlayerId)
                        }
                    )

                    // Guests have no durable identity — surfacing the upgrade path here as
                    // well as on Home, since Profile is where people look for account stuff.
                    if (user.isAnonymous) {
                        GuestUpgradeCard(onLinkGoogle = { onAuthAction(AuthAction.LinkGoogle) })
                    }

                    SectionLabel("General")

                    // Material3 segmented list group — 2dp gaps between segments.
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ProfileRow(
                            icon = Icons.Filled.Groups,
                            title = "Clubs & Teams",
                            subtitle = "Coming soon",
                            enabled = false,
                            onClick = {},
                            index = 0,
                            count = 3
                        )
                        ProfileRow(
                            icon = Icons.Filled.Settings,
                            title = "Settings",
                            subtitle = "Theme, account, notifications",
                            onClick = onOpenSettings,
                            index = 1,
                            count = 3
                        )
                        ProfileRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Sign out",
                            destructive = true,
                            showChevron = false,
                            onClick = { dispatch(ProfileAction.SignOutClicked) },
                            index = 2,
                            count = 3
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (state.showSignOutConfirm) {
        SignOutDialog(
            isAnonymous = state.user?.isAnonymous == true,
            onConfirm = { dispatch(ProfileAction.ConfirmSignOut) },
            onDismiss = { dispatch(ProfileAction.DismissSignOutConfirm) }
        )
    }
}

/**
 * Avatar + name + email. The avatar is a monogram, not a network image: there's no
 * image-loading dependency in the project yet, and pulling one in for a single Google
 * profile picture isn't worth the weight. Swap in Coil here if avatars become important.
 */
@Composable
private fun ProfileHeader(user: CurrentUser) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.initials,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.resolvedName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email ?: if (user.isAnonymous) "Guest account" else "No email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The user's Supabase UID, shown truncated with a copy button — used to identify a player. */
@Composable
private fun PlayerIdRow(
    userId: String,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Player ID",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userId.take(8),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy player ID",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GuestUpgradeCard(onLinkGoogle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "You're signed in as a guest",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Link a Google account to keep your tournaments if you sign out or switch devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            TextButton(
                onClick = onLinkGoogle,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Link Google account")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    subtitle: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    showChevron: Boolean = true
) {
    // Disabled visuals (Clubs & Teams "coming soon") come from SegmentedListItem's own
    // disabled color set — no manual alpha math needed anymore. Destructive rows just
    // override the content + leading icon colors with error.
    val colors = if (destructive) {
        ListItemDefaults.segmentedColors(
            contentColor = MaterialTheme.colorScheme.error,
            leadingContentColor = MaterialTheme.colorScheme.error
        )
    } else {
        ListItemDefaults.segmentedColors()
    }

    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        enabled = enabled,
        colors = colors,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = if (showChevron && enabled) {
            {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
        } else null
    ) {
        Text(title, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SignOutDialog(
    isAnonymous: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign out?") },
        text = {
            Text(
                if (isAnonymous) {
                    "You're on a guest account. Signing out permanently deletes your tournaments — " +
                        "link a Google account first to keep them."
                } else {
                    "You can sign back in with Google at any time. Your tournaments will still be here."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sign out", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
