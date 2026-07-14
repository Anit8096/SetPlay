package com.kmp.setplay.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Snapshot of the signed-in Supabase user, flattened for the UI.
 *
 * Read straight off the auth session's user metadata (populated by Google on sign-in) —
 * there is deliberately no `profiles` table behind this. If SetPlay ever needs
 * user-editable profile fields, that's the point to introduce one.
 */
data class CurrentUser(
    val id: String,
    val email: String?,
    /** Google's `full_name`/`name` claim, if present. */
    val displayName: String?,
    /** Google's `avatar_url`/`picture` claim, if present. */
    val avatarUrl: String?,
    val isAnonymous: Boolean
) {
    /** Name for the header, falling back through email local-part → "Guest". */
    val resolvedName: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Guest"

    /** Up-to-2-char monogram used when there's no avatar image to show. */
    val initials: String
        get() = resolvedName.trim()
            .split(' ')
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

interface AuthRepository {
    val currentUserId: Flow<String?>
    val isLoggedIn: Flow<Boolean>
    val isAnonymous: Flow<Boolean>
    /** True while Supabase Auth is still restoring/verifying the session from storage. */
    val isInitializing: Flow<Boolean>

    /** The signed-in user's identity + Google metadata, or null when signed out. */
    val currentUser: Flow<CurrentUser?>

    suspend fun signInAnonymously(): Result<Unit>
    suspend fun signInWithGoogle(): Result<Unit>
    // Used by both native paths — Android's Credential Manager and Web's Google
    // Identity Services both end up with a Google-issued ID token; this is the one
    // place that exchanges it with Supabase, regardless of which platform got it.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null): Result<Unit>
    suspend fun linkGoogle(): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
