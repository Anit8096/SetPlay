package com.kmp.setplay

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kmp.setplay.di.appModule
import com.kmp.setplay.navigation.NavGraph
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@OptIn(ExperimentalSerializationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Preview
fun App() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModule + platformModules())
        }
    ) {
        MaterialExpressiveTheme(
            colorScheme = if (isSystemInDarkTheme()) darkColorScheme()
            else expressiveLightColorScheme()
        ) {
            NavGraph()
        }
    }
}
