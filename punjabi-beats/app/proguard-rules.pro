# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.punjabibeats.app.**$$serializer { *; }
-keepclassmembers class com.punjabibeats.app.** { *** Companion; }
-keepclasseswithmembers class com.punjabibeats.app.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Media3 ---
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- Retrofit / OkHttp ---
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Guava ---
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**

# --- ZXing ---
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# --- WebView cipher runtime (zemer-cipher pattern) ---
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-keep class com.punjabibeats.app.data.cipher.WebViewScriptRunner$JsBridge { *; }
-keep class com.punjabibeats.app.data.cipher.** { *; }
-dontwarn android.webkit.**
