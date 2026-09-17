pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NazeVer"

include(
    ":app",
    ":core-domain",
    ":core-data",
    ":core-security",
    ":core-network"
)

/*
 * Modul yang akan ditambahkan di phase-phase berikutnya (jangan aktifkan
 * sebelum kontennya dibuat, cukup catatan urutan):
 *
 * feature-chat
 * feature-call            (voice + video, WebRTC)
 * feature-media           (upload/download, view-once lifecycle)
 * feature-location
 * feature-mood
 * feature-calendar
 * feature-digital-letter
 * feature-todo
 * feature-music-room
 * feature-animal-companion
 * feature-appearance      (shared wallpaper/font/theme)
 * feature-privacy-center
 * core-realtime           (wrapper di atas Supabase Realtime channel)
 * core-storage             (Android Keystore + EncryptedSharedPreferences + Room)
 * core-notifications
 * core-background          (WorkManager: cleanup, retry, sync)
 */
