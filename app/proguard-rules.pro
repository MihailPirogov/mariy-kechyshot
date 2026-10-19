# --- Narrow replacement for com.yandex.android:mobileads consumer rules (ignored in build.gradle.kts) ---
# Original rules did "-keepnames class yads.** / com.yandex.mobile.ads.** / com.monetization.ads.** { *; }".
-dontwarn yads.**
-dontwarn com.yandex.mobile.ads.**
-dontwarn com.monetization.ads.**
-dontwarn com.yandex.metrica.*
-dontwarn io.appmetrica.analytics.*
-keep class com.monetization.ads.AnalyticsAdsMarker { *; }
-keep class * extends com.monetization.ads.mediation.appopenad.MediatedAppOpenAdAdapter { *; }
-keep class * extends com.monetization.ads.mediation.banner.MediatedBannerAdapter { *; }
-keep class * extends com.monetization.ads.mediation.interstitial.MediatedInterstitialAdapter { *; }
-keep class * extends com.monetization.ads.mediation.rewarded.MediatedRewardedAdapter { *; }
-keep class * extends com.monetization.ads.mediation.nativeads.MediatedNativeAdapter { *; }
-keep class androidx.recyclerview.widget.AdsDivLinearLayoutManager { *; }
-keep class com.monetization.ads.core.divkit.AdsDivGridLayoutManager { *; }
-keepclassmembers class yads.** implements javax.net.ssl.X509TrustManager {
    java.util.List checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String, java.lang.String);
    boolean isSameTrustConfiguration(java.lang.String, java.lang.String);
}
-keepattributes *Annotation*
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.io.**
-dontwarn android.content.res.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class org.xmlpull.** { *; }
-keepclasseswithmembers class com.monetization.ads.exo.source.dash.DashMediaSource$Factory { <init>(*); }
-keepclassmembers class com.monetization.ads.exo.source.dash.offline.DashDownloader { <init>(...); }
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Narrow replacement for io.appmetrica.analytics:analytics consumer rules ---
# Original: "-keep class io.appmetrica.analytics.** { *; }" (no shrink/optimize/obfuscate of ~2300 classes).
# Keep only the public entry points + module/plugin classes loaded by name.
-keep public class io.appmetrica.analytics.AppMetrica { public *; }
-keep class io.appmetrica.analytics.**.ModuleEntryPoint { *; }
-keep class * implements io.appmetrica.analytics.modulesapi.internal.**.ModuleEntryPoint { <init>(); }
-keep class io.appmetrica.analytics.internal.** extends android.app.Service { <init>(); }
-keep class io.appmetrica.analytics.internal.** extends android.content.ContentProvider { <init>(); }
-keep class io.appmetrica.analytics.internal.** extends android.content.BroadcastReceiver { <init>(); }
-keep class com.android.installreferrer.api.* { *; }
-dontwarn com.android.installreferrer.api.*
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient** { *; }
-keep class kotlin.KotlinVersion { *; }
-keep public class com.google.android.gms.appset.AppSet
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}