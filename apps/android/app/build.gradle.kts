plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

import java.util.Properties

android {
    namespace = "ru.dobrovichek.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.dobrovichek.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // You can override these via apps/android/local.properties:
        // REQUEST_BASE_URL=http://<your-pc-ip>:8083/
        // IDENTITY_BASE_URL=http://<your-pc-ip>:8081/
        buildConfigField("String", "BASE_URL", "\"${loadLocalProperty("REQUEST_BASE_URL").ifBlank { "http://10.0.2.2:8083/" }}\"")
        buildConfigField("String", "IDENTITY_BASE_URL", "\"${loadLocalProperty("IDENTITY_BASE_URL").ifBlank { "http://10.0.2.2:8081/" }}\"")
        buildConfigField("String", "USER_BASE_URL", "\"${loadLocalProperty("USER_BASE_URL").ifBlank { "http://10.0.2.2:8082/" }}\"")
        buildConfigField("String", "MAPKIT_API_KEY", "\"${loadLocalProperty("MAPKIT_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

fun loadLocalProperty(key: String): String {
    val propertiesFile = rootProject.file("local.properties")
    if (!propertiesFile.exists()) return ""
    val properties = Properties()
    propertiesFile.inputStream().use { properties.load(it) }
    return properties.getProperty(key, "")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")

    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    implementation("com.yandex.android:maps.mobile:4.33.1-lite")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
