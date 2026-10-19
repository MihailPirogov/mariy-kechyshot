import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
// Signing secrets live OUTSIDE the source tree: ../mari-kechyshot-keys/keystore.properties
// (override with env MARI_KEYS_DIR). storeFile in it is resolved relative to that folder.
val keysDir = File(System.getenv("MARI_KEYS_DIR") ?: rootProject.file("../mari-kechyshot-keys").path)
// Feedback e-mail comes from gitignored local.properties: feedback.email=you@example.com
val localProps = Properties().apply { val f = rootProject.file("local.properties"); if (f.exists()) f.inputStream().use { load(it) } }
val feedbackEmail: String = localProps.getProperty("feedback.email") ?: "feedback@example.com"
val ks = Properties().apply { val f = File(keysDir, "keystore.properties"); if (f.exists()) f.inputStream().use { load(it) } }
android {
    buildFeatures { buildConfig = true }
    namespace = "ru.mari.kechyshot"
    compileSdk = 36
    defaultConfig { applicationId = "ru.pirogovlab.mariykechyshot"; minSdk = 26; targetSdk = 36; versionCode = 19; versionName = "1.1.17"; buildConfigField("String", "FEEDBACK_EMAIL", "\"$feedbackEmail\"") }
    signingConfigs { create("release") {
        storeFile = File(keysDir, ks.getProperty("storeFile", "release.jks")).let { if (it.exists()) it else File(ks.getProperty("storeFile", "release.jks")) }; storePassword = ks.getProperty("storePassword"); keyAlias = ks.getProperty("keyAlias"); keyPassword = ks.getProperty("keyPassword") } }
    buildTypes {
        getByName("debug") { buildConfigField("boolean", "NO_ADS", "false") }
        // Screenshot-only variant: ads disabled, debug-signed, separate applicationId. Never shipped.
        create("shots") { initWith(getByName("debug")); isDebuggable = false; applicationIdSuffix = ".shots"; versionNameSuffix = "-shots"; buildConfigField("boolean", "NO_ADS", "true"); matchingFallbacks += listOf("debug") }
        getByName("release") { buildConfigField("boolean", "NO_ADS", "false"); isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"); signingConfig = signingConfigs.getByName("release")
            // The Yandex Ads / AppMetrica consumer rules keep (or keep names of) whole SDK packages (~10k classes),
            // which blocks R8 optimization/obfuscation. Replace them with the narrow rules in proguard-rules.pro.
            optimization { keepRules { ignoreFrom("com.yandex.android:mobileads", "io.appmetrica.analytics:analytics") } } } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.yandex.android:mobileads:8.6.0")
}
