package com.kmp.setplay

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kmp.setplay.di.appModule
import com.kmp.setplay.navigation.NavGraph
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@OptIn(ExperimentalSerializationApi::class)
@Composable
@Preview
fun App() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule + platformModules())

        }
    ) {
        MaterialTheme {
            NavGraph()
        }
    }
}
