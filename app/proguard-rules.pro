# Preserve line numbers for readable crash stack traces, but hide the original
# source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Room ────────────────────────────────────────────────────────────────────
# The only genuine reflection point: Room instantiates the generated database
# implementation by name. Entities/DAOs are reached through generated code and
# need no keeps of their own (KSP codegen, no reflection).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ─── Kotlin / coroutines ─────────────────────────────────────────────────────
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# ─── Google Play Billing (keep API models defensively) ───────────────────────
-keep class com.android.billingclient.api.** { *; }

# ─── Strip logging from release builds ───────────────────────────────────────
# Nothing sensitive is currently logged, but this guarantees billing debug
# messages and stack traces never ship to production logcat.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# NOTE (audit 2026-07): the previous Retrofit/OkHttp/Moshi keep block and the
# blanket `-keep class com.example.data.** { *; }` were removed together with the
# unused networking dependencies — the blanket keep was exempting the entire
# business layer (billing, trial, scoring) from shrinking and obfuscation.
