# =========================================================================
# NazeVer ProGuard/R8 rules (release build)
# =========================================================================

# Strip semua Log.* call di release build — mencegah kebocoran isi pesan,
# token, atau data sensitif lain lewat Logcat secara tidak sengaja.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}
-keep,includedescriptorclasses class com.nazeworks.nazever.**$$serializer { *; }
-keepclassmembers class com.nazeworks.nazever.** {
    *** Companion;
}

# Ktor / Supabase client (dipakai secara reflektif oleh engine)
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }

# Hilt / Dagger
-dontwarn com.google.errorprone.annotations.**
