package com.kmp.setplay.presentation.auth

import android.content.Context
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kmp.setplay.BuildKonfig
import com.kmp.setplay.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.security.MessageDigest
import java.util.UUID

@Composable
actual fun GoogleSignInButton(
    isLoading: Boolean,
    onFallback: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier  // no default value on actual
) {
    val authRepository = koinInject<AuthRepository>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch {
                signInWithCredentialManager(context, authRepository, onError)
            }
        },
        enabled = !isLoading,
        modifier = modifier
    ) {
        Text("Continue with Google")
    }
}

private suspend fun signInWithCredentialManager(
    context: Context,
    authRepository: AuthRepository,
    onError: (String) -> Unit
) {
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it) }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildKonfig.GOOGLE_WEB_CLIENT_ID)
        .setNonce(hashedNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            onError("Unexpected credential type from Credential Manager")
            return
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

        authRepository.signInWithGoogleIdToken(
            idToken = googleIdTokenCredential.idToken,
            rawNonce = rawNonce
        ).onFailure { e ->
            onError(e.message ?: "Google sign-in failed")
        }

    } catch (e: GoogleIdTokenParsingException) {
        onError("Couldn't read Google credential: ${e.message}")
    } catch (e: GetCredentialException) {
        onError(e.message ?: "Google sign-in failed")
    } catch (e: Exception) {
        onError(e.message ?: "Google sign-in failed")
    }
}
