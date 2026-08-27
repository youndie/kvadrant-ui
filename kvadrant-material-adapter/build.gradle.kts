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
