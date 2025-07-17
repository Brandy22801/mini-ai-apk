# Preserve Mini AI classes and JSON-related logic
-keep class plugin.** { *; }
-keep class plugin.ksp.** { *; }
-keep class org.json.** { *; }
-keep class com.google.gson.** { *; }

# Preserve WebView for JavaScript execution
-keep class androidx.webkit.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Prevent obfuscation of Mini AI components
-dontoptimize
-dontshrink
-dontwarn plugin.**