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
        // Generated sources are excluded, and it is the generator that decides their shape.
        //
        // ktlint rewrites a method chain onto its own line breaks; a generator emitting that chain
        // then produces a file the formatter immediately edits, and the `--check` that keeps the
        // output honest fails on every clean tree. The alternative is a generator that encodes
        // ktlint's wrapping rules, which is a maintenance trap aimed at a file no person reads.
        //
        // They are still committed and still reviewed as diffs — this exempts them from a
        // formatter, not from a reader.
        // A spec rather than an Ant pattern: this plugin hands the filter absolute paths, and a
        // `**/...` glob silently matches nothing.
        filter {
            exclude { element -> "KvadrantTokens.kt" in element.file.path }
            exclude { element -> "KvadrantIcons.kt" in element.file.path }
        }
    }
}
