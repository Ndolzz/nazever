plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.naze.nazever"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        applicationId = "com.naze.nazever"
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
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
    implementation(project(":core-domain"))
    implementation(project(":core-data"))
    implementation(project(":core-network"))
    implementation(project(":core-security"))
    testImplementation(libs.junit)
}
