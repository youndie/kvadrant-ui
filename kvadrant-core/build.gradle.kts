import org.gradle.api.tasks.PathSensitivity
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.viddik)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    jvmToolchain(25)

    // Desktop first, Android second, and the order was the point: the whole library was built and
    // looked at on one renderer before a second one was allowed to have an opinion. Android is here
    // now because a suite that only ever ran on skiko says nothing about the renderer most of this
    // will actually ship on - and because B-25, a real defect, cannot be seen with one of them
    // missing. Everything else still waits for something to run on it (D14).
    jvm("desktop")

    androidLibrary {
        namespace = "io.github.youndie.kvadrant"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        // Without this the Android target silently skips `commonTest` — the plugin says so in a
        // warning, and a warning is not what should stand between a common test suite and one of
        // the two renderers it is meant to be describing.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // Fonts are bundled through compose-resources so that one declaration serves every
            // target. The desktop-only `platform.Font("fonts/x.ttf")` it replaces reads a classpath
            // resource, which is a JVM idea and has no meaning on Android or on native.
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // Rendering a real window in a test needs the host's skiko native library, and only
        // `currentOs` brings it in. It must not leak into a published source set: a POM that pins a
        // host-specific skiko artefact is broken for every other host.
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

// `ScreenshotSuiteTest` reads the golden directory, so the task has to say so. Without this Gradle
// sees no input change when a golden is added, renamed or deleted, leaves `desktopTest` UP-TO-DATE,
// and the guard reports the last run's verdict about a set that has since changed — which is how it
// stayed green through two deliberate breakages before anyone noticed it had not run.
tasks.named<Test>("desktopTest") {
    inputs
        .dir(layout.projectDirectory.dir("src/desktopTest/snapshots"))
        .withPropertyName("viddikSnapshots")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// Without this the package is derived from the directory names - `kvadrant_ui.kvadrant_core` - and
// a library would be shipping an identifier that changes when somebody renames a folder. It stays
// internal (the default): the fonts are reached through `kvadrantLatin()`, not by resource id.
compose.resources {
    packageOfResClass = "io.github.youndie.kvadrant.resources"
}

// The screenshot suite has to be part of the one gate. Left out of `check` it becomes a command
// somebody remembers to run, which means it runs on one machine and not on the branch.
viddik {
    verifyOnCheck.set(true)
}
