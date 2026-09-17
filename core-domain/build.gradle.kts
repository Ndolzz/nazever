plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

// core-domain sengaja murni Kotlin (bukan modul Android) supaya:
// - Bisa di-unit-test tanpa emulator/robolectric.
// - Tidak ada dependency ke Android framework di lapisan domain.
// - Bisa dipakai ulang oleh modul lain (app, core-data, semua feature module).

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

kotlin {
    jvmToolchain(17)
}
