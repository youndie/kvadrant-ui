plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.viddik) apply false
    // Declared here, unapplied, and that is load-bearing rather than tidy: the Compose plugin reads
    // AGP's own extension types to wire up resources, and it can only see them when both plugins
    // land in the same build classloader. Declared in the module alone, Compose fails with
    // NoClassDefFoundError on a class that is demonstrably in AGP's jar.
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.ktlint)
}

// One gate: `./gradlew check` runs the tests and ktlint. A lint that has to be remembered as a
// separate command is a lint that runs on somebody's machine and not on the branch.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint)
    }
}
