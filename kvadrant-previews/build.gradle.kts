plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(25)

    // Two targets and no Android one, which is the opposite of every other module here and is
    // deliberate. This module exists to be *looked at*: the desktop target is where a test can
    // render a preview and say whether it composes, and wasm is what the documentation site serves.
    // An Android variant would be a third build of code nothing on Android consumes.
    jvm("desktop")

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "previews.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // `api`, not `implementation`. A preview is a `@Composable` handed to whoever mounts
            // it, and the types in its signature — nothing more than that — come from the core.
            api(project(":kvadrant-core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
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
