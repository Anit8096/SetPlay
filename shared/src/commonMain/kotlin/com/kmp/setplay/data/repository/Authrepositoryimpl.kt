package com.kmp.setplay.data.repository

import com.kmp.setplay.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val currentUserId: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.id
            else -> null
        }
    }.flowOn(Dispatchers.Default)

    override val isLoggedIn: Flow<Boolean> = supabase.auth.sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }.flowOn(Dispatchers.Default)

    override val isInitializing: Flow<Boolean> = supabase.auth.sessionStatus.map { status ->
        status is SessionStatus.Initializing
    }.flowOn(Dispatchers.Default)

    override val isAnonymous: Flow<Boolean> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.isAnonymous ?: false
            else -> false
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun signInAnonymously(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching { supabase.auth.signInAnonymously() }
    }

    override suspend fun signInWithGoogle(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching { supabase.auth.signInWith(Google) }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String?): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                supabase.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                    if (rawNonce != null) {
                        this.nonce = rawNonce
                    }
                }
            }
        }

    override suspend fun linkGoogle(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            supabase.auth.linkIdentity(Google)
            Unit
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching { supabase.auth.signOut() }
    }
}