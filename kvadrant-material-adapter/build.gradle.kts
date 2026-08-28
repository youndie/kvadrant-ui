plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.viddik)
    alias(libs.plugins.dokka)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    jvmToolchain(25)
    jvm("desktop")

    // The adapter follows the core onto wasm: an island of Metro inside a Material application
    // (B-19) is a thing a browser page has as much right to show as a phone does.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // A library with no executable binary sounds right and is refused: the Compose plugin needs
        // one so that the Skiko runtime is bundled for the tests that run on this target, and says
        // so by name. Nothing consumes the binary; it exists so the test bundle is complete.
        binaries.executable()
    }

    // **The adapter had no Android target, and that is where Material actually runs.** B-04 asks
    // for a Material `OutlinedTextField` rendering under the adapter "on Android and on desktop",
    // and it had been read as a missing test; the module was not built for Android at all, so a
    // consumer on the one platform this interop exists to serve could not depend on it. Adding the
    // target is the half that belongs in `check`. The other half — that it *renders* there — is
    // Android's usual answer and not this gate's: viddik is JVM-only, so a green `check` says
    // nothing about Android (B-29).
    androidLibrary {
        namespace = "io.github.youndie.kvadrant.material"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        // Without this the Android target silently skips `commonTest` — the plugin warns, and a
        // warning is not what should stand between a common suite and one of the renderers.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":kvadrant-core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // The whole point of the experiment: which Material line renders.
            implementation(libs.compose.material3)
        }
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

// The side-by-side is the only place the adapter's claim can be judged, so it is inside the gate
// like the core's suite. Desktop only, and that says nothing about Android — B-29.
viddik {
    verifyOnCheck.set(true)
}
