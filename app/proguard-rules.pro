# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Optimization and obfuscation options
# Enable optimizations so R8 can aggressively shrink and obfuscate
-allowaccessmodification
-repackageclasses ''
-dontusemixedcaseclassnames
-overloadaggressively
-useuniqueclassmembernames
-renamesourcefileattribute ''
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Remove logging completely
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Android basics
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Kotlin & Coroutines specifics
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# OkHttp & Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
}

# Keep local models & data classes from being obfuscated (critical for Gson/Moshi)
-keep class com.enox.enoxpay.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Update <methods>;
}

# Compose
-keep class androidx.compose.** { *; }

# Obfuscate everything else strongly
-flattenpackagehierarchy ''
