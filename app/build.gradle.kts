import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localSettings = Properties().apply {
    val settingsFile = rootProject.file("local.properties")
    if (settingsFile.exists()) settingsFile.inputStream().use { load(it) }
}

val mapsApiKey = localSettings.getProperty("MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: ""
val debugApiBaseUrl = localSettings.getProperty("API_BASE_URL_DEBUG")
    ?: localSettings.getProperty("API_BASE_URL")
    ?: System.getenv("API_BASE_URL")
    ?: "http://10.0.2.2:5205/"
val releaseApiBaseUrl = localSettings.getProperty("API_BASE_URL_RELEASE")
    ?: System.getenv("API_BASE_URL_RELEASE")
    ?: ""

android {
    namespace = "com.solargridx.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.solargridx.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            manifestPlaceholders["USES_CLEARTEXT_TRAFFIC"] = "true"
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            manifestPlaceholders["USES_CLEARTEXT_TRAFFIC"] = "false"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

val validateReleaseApiUrl = tasks.register("validateReleaseApiUrl") {
    doLast {
        require(releaseApiBaseUrl.startsWith("https://", ignoreCase = true)) {
            "Set API_BASE_URL_RELEASE to the production HTTPS API base URL before building Release."
        }
    }
}

tasks.configureEach {
    if (name.contains("Release", ignoreCase = true) && name != "validateReleaseApiUrl") {
        dependsOn(validateReleaseApiUrl)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    // SQLite is supplied by Android SDK (android.database.sqlite).
}
