plugins {
    id("com.android.library") version "9.1.0" apply false
    kotlin("android") version "2.1.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}