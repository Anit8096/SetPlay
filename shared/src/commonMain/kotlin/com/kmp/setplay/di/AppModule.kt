package com.kmp.setplay.di

import com.kmp.setplay.data.remote.createSetPlaySupabaseClient
import com.kmp.setplay.data.repository.AuthRepositoryImpl
import com.kmp.setplay.data.repository.TournamentRepositoryImpl
import com.kmp.setplay.domain.repository.AuthRepository
import com.kmp.setplay.domain.repository.TournamentRepository
import com.kmp.setplay.presentation.auth.AuthViewModel
import com.kmp.setplay.presentation.browse.BrowseViewModel
import com.kmp.setplay.presentation.profile.ProfileViewModel
import com.kmp.setplay.presentation.tournament.create.CreateTournamentViewModel
import com.kmp.setplay.presentation.tournament.detail.TournamentDetailViewModel
import com.kmp.setplay.presentation.tournament.join.JoinTournamentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Supabase ──────────────────────────────────────────────────────────────
    single { createSetPlaySupabaseClient() }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<TournamentRepository> {
        TournamentRepositoryImpl(supabase = get(), cache = get())
    }

    single { AuthViewModel(get()) }
    viewModel { BrowseViewModel(tournamentRepository = get(), authRepository = get()) }
    viewModel { ProfileViewModel(authRepository = get()) }
    viewModel { CreateTournamentViewModel(get()) }
    viewModel { params ->
        TournamentDetailViewModel(
            tournamentId = params.get(),
            authRepository = get(),
            tournamentRepository = get()
        )
    }
    viewModel { params ->
        JoinTournamentViewModel(
            initialCode = params.getOrNull(),
            tournamentRepository = get()
        )
    }
}