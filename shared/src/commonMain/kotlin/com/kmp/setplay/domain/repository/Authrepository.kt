package com.kmp.setplay.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>

    val isLoggedIn: Flow<Boolean>

    suspend fun signInAnonymously(): Result<Unit>

    suspend fun signInWithGoogle(): Result<Unit>

    suspend fun linkGoogle(): Result<Unit>

    suspend fun signOut(): Result<Unit>
}