plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.arkivanov.decompose.extensions.compose.jetpack"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = deps.versions.jetpackComposeCompiler.get()
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":decompose"))
    implementation(deps.androidx.compose.foundation.foundation)
    implementation(deps.androidx.activity.activityKtx)
    androidTestImplementation(deps.androidx.compose.ui.uiTestJunit4)
    androidTestImplementation(deps.junit.junit)
    androidTestImplementation(deps.androidx.compose.ui.uiTestManifest)
    androidTestImplementation(deps.androidx.test.core)
}
