plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.naze.nazever.core.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        // Supabase URL and public anon key are supplied by developers via
        // ~/.gradle/gradle.properties (never committed to the repository).
        // The anon key is public by design; authorization is enforced by RLS.
        buildConfigField("String", "SUPABASE_URL", "\"" + (project.findProperty("SUPABASE_URL") as String? ?: "") + "\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + (project.findProperty("SUPABASE_ANON_KEY") as String? ?: "") + "\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core-security"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
