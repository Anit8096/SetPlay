package com.kmp.setplay.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmp.setplay.BuildKonfig
import com.kmp.setplay.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.compose.koinInject
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

external interface GoogleCredentialResponse {
    val credential: String
}

external interface GoogleIdConfiguration {
    var client_id: String
    var callback: (GoogleCredentialResponse) -> Unit
    var auto_select: Boolean
}

external interface GoogleAccountsId {
    fun initialize(config: GoogleIdConfiguration)
    fun prompt()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun newGoogleIdConfiguration(): GoogleIdConfiguration = js("({})")
@OptIn(ExperimentalWasmJsInterop::class)
private fun googleAccountsId(): GoogleAccountsId = js("window.google.accounts.id")


private suspend fun requestGoogleIdToken(clientId: String): String? =
    suspendCancellableCoroutine { continuation ->
        val config = newGoogleIdConfiguration()
        config.client_id = clientId
        config.auto_select = false
        config.callback = { response ->
            if (continuation.isActive) continuation.resume(response.credential)
        }
        googleAccountsId().apply {
            initialize(config)
            prompt()
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun GoogleSignInButton(
    isLoading: Boolean,
    onFallback: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier  // no default value on actual
) {
    val authRepository = koinInject<AuthRepository>()
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch {
                try {
                    val idToken = requestGoogleIdToken(BuildKonfig.GOOGLE_WEB_CLIENT_ID)
                    if (idToken != null) {
                        authRepository.signInWithGoogleIdToken(idToken = idToken)
                            .onFailure { e -> onError(e.message ?: "Google sign-in failed") }
                    }
                } catch (e: Throwable) {
                    onFallback()
                }
            }
        },
        enabled = !isLoading,
        modifier = modifier
    ) {
        AnimatedVisibility(isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp)
            )
        }
        Text("Continue with Google")
    }
}