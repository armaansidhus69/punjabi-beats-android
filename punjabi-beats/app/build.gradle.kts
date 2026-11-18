plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.punjabibeats.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.punjabibeats.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "1.1.4"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "BASE_URL", "\"https://punjabi-beats-api.vercel.app\"")
        buildConfigField("String", "JAM_WS_URL", "\"wss://punjabi-beats-api.vercel.app/api/jam-ws\"")
        buildConfigField("String", "JIOSAAVN_BASE_URL", "\"https://saavn.dev/\"")
        buildConfigField("String", "LRCLIB_BASE_URL", "\"https://lrclib.net/\"")
        buildConfigField("String", "AUDIUS_APP_NAME", "\"PunjabiBeats\"")
        buildConfigField("String", "CIPHER_CONFIG_URL", "\"https://punjabi-beats-api.vercel.app/api/player-configs\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE.md",
                "/META-INF/LICENSE-notice.md"
            )
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Zen engine — search + stream resolution + channel transport, no backend API
    implementation(project(":zenmusic"))
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Media3 playback engine
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)
    implementation(libs.media3.datasource)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Images
    implementation(libs.coil.compose)
    implementation(libs.coil.base)

    // Palette (album-art color extraction)
    implementation(libs.androidx.palette)

    // Guava ListenableFuture bridge for MediaSession callbacks
    implementation(libs.guava)

    // QR codes (generate with core, scan with embedded)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
