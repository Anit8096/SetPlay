package com.kmp.setplay.di

import com.kmp.setplay.data.local.db.SetPlayDatabase
import com.kmp.setplay.data.local.db.getDatabaseBuilder
import com.kmp.setplay.data.remote.createSetPlaySupabaseClient
import com.kmp.setplay.data.repository.AuthRepositoryImpl
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.presentation.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Supabase ──────────────────────────────────────────────────────────────
    single { createSetPlaySupabaseClient() }

    // ── Room Database ─────────────────────────────────────────────────────────
    single<SetPlayDatabase> {
        getDatabaseBuilder().build()
    }

    // ── DAOs ──────────────────────────────────────────────────────────────────
    single { get<SetPlayDatabase>().tournamentDao() }
    single { get<SetPlayDatabase>().teamDao() }
    single { get<SetPlayDatabase>().playerDao() }
    single { get<SetPlayDatabase>().roundDao() }
    single { get<SetPlayDatabase>().matchDao() }
    single { get<SetPlayDatabase>().standingDao() }
    single { get<SetPlayDatabase>().announcementDao() }
    single { get<SetPlayDatabase>().deviceTokenDao() }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<AuthRepository> { AuthRepositoryImpl(get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { AuthViewModel(get()) }
}