plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.estatenestora.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.estatenestora.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts += "**/libtdjson.so"
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.location)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    
    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Coil Image Loader
    implementation(libs.coil.compose)

    // MapLibre GL Native — 100% open source (BSD-2), free forever, no API key
    // required for the SDK itself. Paired with OpenFreeMap's free, unlimited
    // vector tile hosting (see MapLocationPickerScreen) instead of a metered
    // provider like Mapbox/Google Maps, so the map picker has no usage cap
    // or billing account to manage.
    implementation(libs.maplibre.android.sdk)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // TDLib (MTProto) — lets the app act as a real Telegram *user*, not the bot,
    // so it can send/receive via Dev1 Bot without the getUpdates 409 / self-echo
    // problems described in TDLIB_SETUP.md. See that file before relying on this
    // coordinate — it's a community JitPack build, not an official artifact, and
    // its availability/version should be verified against https://jitpack.io/#tdlibx/td
    // before you build. If it's stale, fall back to the manual integration path
    // documented in the same file (prebuilt libtdjson.so + TdApi.java/Client.java
    // copied straight from https://github.com/tdlib/td).
    implementation("com.github.tdlibx:td:latest.release")

    debugImplementation(libs.androidx.ui.tooling)
}
