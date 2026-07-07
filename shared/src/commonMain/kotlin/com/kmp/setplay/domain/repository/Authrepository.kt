package com.kmp.setplay.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>
    val isLoggedIn: Flow<Boolean>
    val isAnonymous: Flow<Boolean>
    /** True while Supabase Auth is still restoring/verifying the session from storage. */
    val isInitializing: Flow<Boolean>

    suspend fun signInAnonymously(): Result<Unit>
    suspend fun signInWithGoogle(): Result<Unit>
    // Used by both native paths — Android's Credential Manager and Web's Google
    // Identity Services both end up with a Google-issued ID token; this is the one
    // place that exchanges it with Supabase, regardless of which platform got it.
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String? = null): Result<Unit>
    suspend fun linkGoogle(): Result<Unit>
    suspend fun signOut(): Result<Unit>
}