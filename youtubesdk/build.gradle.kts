plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "aaravgupta.youtubesdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    publishing {
        singleVariant("release")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.rhino)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    testImplementation(libs.junit4)
}