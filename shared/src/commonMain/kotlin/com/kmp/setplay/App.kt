package com.kmp.setplay

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.kmp.setplay.data.remote.parseSessionFromUrl
import com.kmp.setplay.di.appModule
import com.kmp.setplay.navigation.NavGraph
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.compose.KoinApplication
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
        LaunchedEffect(Unit) {
            // Web: reads URL fragment and imports session into Supabase.
            // Android: no-op — OAuth redirect is handled in PlatformApp()
            //          which wraps this composable in androidMain.
            parseSessionFromUrl()
        }

        MaterialTheme {
            NavGraph()
        }
    }
}