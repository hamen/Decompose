plugins {
    id("kotlin-multiplatform")
    id("com.android.library")
}

android {
    namespace = "com.arkivanov.decompose"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
}

kotlin {
    android()
    jvm()
}
