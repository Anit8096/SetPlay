import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room3)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildkonfig)
}

val supabaseUrl: String = rootProject.extra["supabaseUrl"] as String
val supabaseAnonKey: String = rootProject.extra["supabaseAnonKey"] as String
val googleWebClientId: String = rootProject.extra["googleWebClientId"] as String

kotlin {
    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "com.kmp.setplay.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.bundled)

            // QR scanning — Android only
            implementation(libs.mlkit.barcode)

            // Native Google Sign-In — Credential Manager
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Navigation3
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)

            // Material3 Adaptive — window size classes + list-detail scene strategy for Nav3
            implementation(libs.jetbrains.material3.adaptive)
            implementation(libs.jetbrains.material3.adaptive.layout)
            implementation(libs.jetbrains.material3.adaptive.navigation3)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            // Kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            // Supabase
            api(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.functions)
            implementation(libs.supabase.compose.auth)

            // Room
            implementation(libs.room.runtime)

            // QR generation
            implementation(libs.qrose)

            // Material Icons Extended
            implementation(libs.compose.material.icons.extended)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        webMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.wrappers.browser)
        }
        wasmJsMain.dependencies {}
    }
}

// Room schema output
room3 {
    schemaDirectory("$projectDir/schemas")
}

// KSP targets — Room compiler needs to run on every platform
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
}

buildkonfig {
    packageName = "com.kmp.setplay"

    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_ANON_KEY", supabaseAnonKey)
        buildConfigField(STRING, "GOOGLE_WEB_CLIENT_ID", googleWebClientId)
    }
}
