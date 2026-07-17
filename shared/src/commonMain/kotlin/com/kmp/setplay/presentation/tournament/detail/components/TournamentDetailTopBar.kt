package com.kmp.setplay.presentation.tournament.detail.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailAction
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailUiState

/**
 * Top app bar actions (Share code / organizer overflow menu) for the tournament detail
 * screen. Rendered by MainAppNavigation's shared Scaffold topBar `actions` slot rather
 * than by TournamentDetailScreen itself, since titles and navigation/back handling for
 * the whole main app are centralized there.
 */
@Composable
fun TournamentDetailTopBarActions(
    state: TournamentDetailUiState,
    onAction: (TournamentDetailAction) -> Unit
) {
    if (!state.isOrganizer) return

    IconButton(onClick = { onAction(TournamentDetailAction.ShowShareCode) }) {
        Icon(Icons.Filled.Share, contentDescription = "Share code")
    }
    OrganizerOverflowMenu(
        isOwner = state.organizerRole != null,
        isPrivate = state.tournament?.isPublic == false,
        onRename = { onAction(TournamentDetailAction.ShowRenameDialog) },
        onShowAccessList = { onAction(TournamentDetailAction.ShowAccessList) },
        onEnd = { onAction(TournamentDetailAction.RequestEndTournament) },
        onDelete = { onAction(TournamentDetailAction.RequestDeleteTournament) }
    )
}

@Composable
private fun OrganizerOverflowMenu(
    isOwner: Boolean,
    isPrivate: Boolean,
    onRename: () -> Unit,
    onShowAccessList: () -> Unit,
    onEnd: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Rename tournament") },
            leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null) },
            onClick = { expanded = false; onRename() }
        )
        if (isPrivate) {
            DropdownMenuItem(
                text = { Text("Access list") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = { expanded = false; onShowAccessList() }
            )
        }
        if (isOwner) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("End tournament", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onEnd() }
            )
            DropdownMenuItem(
                text = { Text("Delete tournament", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}
