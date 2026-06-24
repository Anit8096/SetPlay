package com.kmp.setplay.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Stateless sign-in screen. State is owned by [AuthViewModel] and delivered
 * here as a plain [AuthUiState]. All user intent is emitted via [onAction].
 *
 * The screen intentionally knows nothing about navigation — the caller
 * (NavHost in App.kt) observes [AuthUiState.isAuthenticated] and performs
 * the push to HomeScreen when it becomes true.
 */
@Composable
fun SignInScreen(
    state: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors via snackbar and dismiss from state once shown.
    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            onAction(AuthAction.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp)
            ) {
                // ── Brand mark ────────────────────────────────────────────
                Text(
                    text = "SetPlay",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tournament brackets, simplified.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // ── Google sign-in ────────────────────────────────────────
                OutlinedButton(
                    onClick = { onAction(AuthAction.SignInWithGoogle) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedVisibility(state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("Continue with Google")
                }

                // ── Anonymous / guest ─────────────────────────────────────
                TextButton(
                    onClick = { onAction(AuthAction.SignInAnonymously) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue as guest")
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Guest sessions can be upgraded to a full account at any time without losing your data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Upgrade banner shown inside the app when the current user is anonymous.
 * Place this at the top of any screen that benefits from account persistence
 * (e.g., MyTournamentsScreen).
 */
@Composable
fun LinkAccountBanner(
    onLinkGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Save your tournaments",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Link a Google account to access your brackets from any device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onLinkGoogle,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Link Google account")
            }
        }
    }
}