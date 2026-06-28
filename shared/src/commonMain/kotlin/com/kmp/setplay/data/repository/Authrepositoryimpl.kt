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

    override val isAnonymous: Flow<Boolean> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.isAnonymous ?: false
            else -> false
        }
    }

    override suspend fun signInAnonymously(): Result<Unit> = runCatching {
        supabase.auth.signInAnonymously()
    }

    override suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        supabase.auth.signInWith(Google)
    }

    override suspend fun linkGoogle(): Result<Unit> = runCatching {
        supabase.auth.linkIdentity(Google)
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }
}