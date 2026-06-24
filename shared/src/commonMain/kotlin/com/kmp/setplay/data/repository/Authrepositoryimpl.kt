package com.kmp.setplay.data.repository

import com.kmp.setplay.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val currentUserId: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.id
            else -> null
        }
    }

    override val isLoggedIn: Flow<Boolean> = supabase.auth.sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }

    override suspend fun signInAnonymously(): Result<Unit> = runCatching {
        supabase.auth.signInAnonymously()
    }

    override suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        // OAuthProvider.signIn triggers the platform OAuth flow.
        // On Android with compose-auth, this is handled via the GoogleAuthButton
        // composable in the UI layer — this function covers the Web path and
        // any direct SDK usage.
        supabase.auth.signInWith(Google)
    }

    override suspend fun linkGoogle(): Result<Unit> = runCatching {
        // Links the existing anonymous session to a Google identity.
        // The user keeps all tournament data they created while anonymous.
        supabase.auth.linkIdentity(Google)
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }
}