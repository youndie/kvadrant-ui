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

    // The demo in a browser, which is what B-34's documentation site is built out of. `binaries
    // .executable()` is what turns the target from a library into something with an entry point,
    // and `commonWebpackConfig` names the page it is served into.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "sample.js"
            }
        }
        binaries.executable()
    }

    // The demo on the phone the library has never run on. `binaries.executable` is what turns this
    // from a klib into a Mach-O with an entry point — and a `.app` for the simulator is a directory
    // holding that binary and an `Info.plist`, so no Xcode project appears anywhere in this
    // repository. `scripts/ios-sample-app.sh` assembles and installs it.
    //
    // The simulator only: `iosArm64` would be a binary for a device nobody here can install on, and
    // the library's own `iosArm64` target is what a consumer builds against.
    iosSimulatorArm64 {
        binaries.executable {
            entryPoint = "io.github.youndie.kvadrant.sample.ios.main"
        }
    }

    // The demo is a library on Android and an application on the desktop, which looks lopsided and
    // is forced: since AGP 9 the application plugin refuses to sit in a Kotlin Multiplatform module
    // at all. So the shared screen lives here and `:sample-android` is the thin activity that hosts
    // it — the alternative was two copies of the demo, drifting.
    android {
        // The Android resource pipeline is off by default in AGP's Kotlin Multiplatform plugin, and
        // with it off `variant.sources.assets` is null — so compose-resources has nowhere to put
        // anything and the artefact ships without it, green. B-37.
        androidResources { enable = true }

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
            // The system back gesture. `androidx.compose.ui.backhandler.BackHandler` ships in its
            // own artefact rather than in `compose.ui`, and it is multiplatform: the gesture on
            // Android, inert on the desktop, so the shared screen needs no per-platform branch.
            implementation("org.jetbrains.compose.ui:ui-backhandler:${libs.versions.compose.multiplatform.get()}")
        }
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
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
