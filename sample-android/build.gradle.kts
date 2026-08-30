plugins {
    alias(libs.plugins.androidApplication)
    // No `org.jetbrains.kotlin.android`: since AGP 9 the Android plugin brings Kotlin itself, and
    // applying the old one is a hard error rather than a warning.
    alias(libs.plugins.composeCompiler)
    id("ru.workinprogress.sborka.base")
    id("ru.workinprogress.sborka.lint")
}

android {
    namespace = "io.github.youndie.kvadrant.sample.android"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.github.youndie.kvadrant.sample"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }

    // A demo, installed by `installDebug` onto whatever is plugged in. There is no signing config
    // and no release variant worth the name: what this module exists for is being looked at.
    buildTypes {
        getByName("debug") { isMinifyEnabled = false }
    }
}

dependencies {
    implementation(project(":sample"))
    implementation(project(":kvadrant-core"))
    implementation(libs.androidx.activity.compose)
}
