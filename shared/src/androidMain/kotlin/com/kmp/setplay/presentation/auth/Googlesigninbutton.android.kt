package com.kmp.setplay.presentation.auth

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Android actual.
 *
 * NOTE: supabase-kt compose-auth (GoogleAuthButton / rememberSignInWithGoogle)
 * requires the Google Identity Services SDK and additional Gradle setup.
 * If you haven't added that yet, this uses a plain button that triggers
 * the browser OAuth flow via supabase.auth.signInWith(OAuthProvider.Google).
 *
 * To upgrade to native One Tap later:
 * 1. Add `implementation("io.github.jan-tennert.supabase:compose-auth")` to androidMain
 * 2. Add Google Identity Services to your androidApp build.gradle.kts
 * 3. Replace the body below with rememberSignInWithGoogle + GoogleAuthButton
 */
@Composable
actual fun GoogleSignInButton(
    isLoading: Boolean,
    onFallback: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier  // no default value on actual
) {
    val supabase = koinInject<SupabaseClient>()
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch {
                runCatching {
                    supabase.auth.signInWith(Google)
                }.onFailure { e ->
                    onError(e.message ?: "Google sign-in failed")
                }
            }
        },
        enabled = !isLoading,
        modifier = modifier
    ) {
        Text("Continue with Google")
    }
}