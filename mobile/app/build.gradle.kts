plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.roofrecon.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.roofrecon.mobile"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Override at build time: ./gradlew assembleDebug -PbackendUrl=https://...
        val backendUrl: String = (project.findProperty("backendUrl") as String?)
            ?: "http://10.0.2.2:3000"
        val djiAppKey: String = (project.findProperty("djiAppKey") as String?) ?: ""
        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
        manifestPlaceholders["DJI_APP_KEY"] = djiAppKey
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        // DJI v5 ships native libs; avoid duplicate-class errors when bundling.
        jniLibs.pickFirsts += setOf("**/libc++_shared.so", "**/libfbjni.so")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // DJI Mobile SDK v5 — replace versions with the latest from
    // https://developer.dji.com/document/mobile-sdk-v5
    implementation("com.dji:dji-sdk-v5-aircraft:5.8.0")
    compileOnly("com.dji:dji-sdk-v5-aircraft-provided:5.8.0")
}
