package com.kmp.setplay.data.repository

import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.CurrentUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val currentUserId: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.id
            else -> null
        }
    }.flowOn(Dispatchers.Default)

    override val currentUser: Flow<CurrentUser?> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.toCurrentUser()
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

/**
 * Flattens Supabase's `UserInfo` into the UI-facing [CurrentUser].
 *
 * Google's ID-token claims land in `userMetadata` as raw JSON, and the key names differ
 * depending on whether the session came from the OAuth redirect flow or the native
 * ID-token flow (Credential Manager / GIS). Both spellings are checked, in order of
 * preference, so the Profile header renders the same on Android and Web.
 */
private fun UserInfo.toCurrentUser(): CurrentUser = CurrentUser(
    id = id,
    email = email?.takeIf { it.isNotBlank() },
    displayName = userMetadata.stringOrNull("full_name", "name"),
    avatarUrl = userMetadata.stringOrNull("avatar_url", "picture"),
    isAnonymous = isAnonymous ?: false
)

/**
 * First non-blank string value among [keys], or null.
 *
 * Uses a plain `as?` cast rather than the `jsonPrimitive` extension, which *throws* on a
 * non-primitive element instead of returning null. Metadata is attacker-adjacent free-form
 * JSON from the identity provider, so a nested object under `name` must not crash the app.
 */
private fun JsonObject?.stringOrNull(vararg keys: String): String? {
    val obj = this ?: return null
    for (key in keys) {
        val primitive = obj[key] as? JsonPrimitive ?: continue
        if (!primitive.isString) continue
        val value = primitive.content
        if (value.isNotBlank()) return value
    }
    return null
}