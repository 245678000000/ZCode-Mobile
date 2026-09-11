# ZCode Mobile — keep WebView / JS bridge / ML Kit surfaces.

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class app.zcode.mobile.remote.ZCodeWebBridge {
    public *;
}
-keep class app.zcode.mobile.remote.ZCodeWebBridge { *; }

-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

-keep class androidx.datastore.** { *; }
-keep class androidx.security.crypto.** { *; }

-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
