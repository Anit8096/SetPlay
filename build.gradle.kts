import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL")
    ?: error("SUPABASE_URL missing from local.properties")
val supabaseAnonKey: String = localProperties.getProperty("SUPABASE_ANON_KEY")
    ?: error("SUPABASE_ANON_KEY missing from local.properties")
val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
    ?: error("GOOGLE_WEB_CLIENT_ID missing from local.properties")

// Expose to all subprojects
extra["supabaseUrl"] = supabaseUrl
extra["supabaseAnonKey"] = supabaseAnonKey
extra["googleWebClientId"] = googleWebClientId


plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.buildkonfig) apply false
}