# Proguard / R8 rules for PoultryApp

# Keep all core data, models, engine, security, and sync packages
-keep class com.example.poultryapp.data.local.** { *; }
-keep class com.example.poultryapp.data.model.** { *; }
-keep class com.example.poultryapp.data.repository.** { *; }
-keep class com.example.poultryapp.data.backup.** { *; }
-keep class com.example.poultryapp.data.sync.** { *; }
-keep class com.example.poultryapp.engine.** { *; }
-keep class com.example.poultryapp.security.** { *; }
-keep class com.example.poultryapp.voice.** { *; }
-keep class com.example.poultryapp.assistant.** { *; }

# Keep Android entry points, receivers, services, and widgets
-keep class com.example.poultryapp.MainActivity { *; }
-keep class com.example.poultryapp.PoultryApplication { *; }
-keep class com.example.poultryapp.receiver.** { *; }
-keep class com.example.poultryapp.service.** { *; }
-keep class com.example.poultryapp.widget.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Preserve Enums for SQLite database and JSON serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public *;
}

# Jetpack Compose and Coroutines safety
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Optimize aggressively
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

