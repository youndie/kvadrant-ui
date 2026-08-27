import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    jvmToolchain(25)

    jvm("desktop")

    // The demo is a library on Android and an application on the desktop, which looks lopsided and
    // is forced: since AGP 9 the application plugin refuses to sit in a Kotlin Multiplatform module
    // at all. So the shared screen lives here and `:sample-android` is the thin activity that hosts
    // it — the alternative was two copies of the demo, drifting.
    androidLibrary {
        namespace = "io.github.youndie.kvadrant.sample"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":kvadrant-core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.youndie.kvadrant.sample.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Kvadrant UI"
        }
    }
}
