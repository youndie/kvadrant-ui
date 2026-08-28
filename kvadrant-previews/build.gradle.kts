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

// The bridge between the registry and the documentation site's generator (B-34).
//
// It runs the compiled registry rather than reading the sources, because the two answer different
// questions: a text scan reports what the file says, and this reports what the module actually
// built. The output is under `build/` and is never committed — a checked-in copy is a second source
// of truth that goes stale silently.
val previewIndex by tasks.registering(JavaExec::class) {
    description = "Write the preview registry to build/preview-index.json for scripts/build_site.py."
    val compilation =
        kotlin.targets
            .getByName("desktop")
            .compilations
            .getByName("main")
    dependsOn(compilation.compileTaskProvider)
    classpath = files(compilation.output.allOutputs, compilation.runtimeDependencyFiles)
    mainClass.set("io.github.youndie.kvadrant.previews.PreviewIndexKt")
    val output = layout.buildDirectory.file("preview-index.json")
    outputs.file(output)
    argumentProviders.add { listOf(output.get().asFile.absolutePath) }
}

// The catalogue's preview column is only verified where the registry has been read, and that is
// here — `make check` runs the same script without a build behind it and says so. Two claims, and
// the smaller one is not allowed to look like the larger.
val checkComponentCatalog by tasks.registering(Exec::class) {
    description = "Fail if docs/components.md no longer matches the sources and the preview registry."
    dependsOn(previewIndex)
    inputs.file(rootProject.layout.projectDirectory.file("docs/components.md"))
    inputs.file(rootProject.layout.projectDirectory.file("scripts/component_catalog.py"))
    inputs.dir(rootProject.layout.projectDirectory.dir("kvadrant-core/src/commonMain"))
    workingDir = rootProject.layout.projectDirectory.asFile
    commandLine("python3", "scripts/component_catalog.py", "--check")
}

tasks.named("check") { dependsOn(checkComponentCatalog) }
