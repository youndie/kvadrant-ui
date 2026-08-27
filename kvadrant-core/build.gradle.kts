plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.viddik)
}

kotlin {
    jvmToolchain(25)

    // Desktop first. The other targets are added when there is something to run on them; each one
    // added early is a target whose failures nobody looks at.
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
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

// The screenshot suite has to be part of the one gate. Left out of `check` it becomes a command
// somebody remembers to run, which means it runs on one machine and not on the branch.
viddik {
    verifyOnCheck.set(true)
}
