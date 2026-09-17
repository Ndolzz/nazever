plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.nazeworks.nazever.security"
    compileSdk = 35

    defaultConfig {
        minSdk = 26 // Android Keystore StrongBox/attestation lebih konsisten di 26+
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
