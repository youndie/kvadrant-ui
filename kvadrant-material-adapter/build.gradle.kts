plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.viddik)
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
