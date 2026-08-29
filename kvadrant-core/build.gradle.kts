import org.gradle.api.tasks.PathSensitivity
import java.util.zip.ZipFile
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.viddik)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.dokka)
}

kotlin {
    jvmToolchain(25)

    // Desktop first, Android second, and the order was the point: the whole library was built and
    // looked at on one renderer before a second one was allowed to have an opinion. Android is here
    // now because a suite that only ever ran on skiko says nothing about the renderer most of this
    // will actually ship on - and because B-25, a real defect, cannot be seen with one of them
    // missing. Everything else still waits for something to run on it (D14).
    jvm("desktop")

    // The third renderer, and the reason it is here rather than waiting with iOS: B-34 wants a page
    // per component with the component running in it, and wasm is how a Compose demo reaches a
    // browser. D14 said targets arrive when something runs on them; this is that something.
    //
    // Nothing guards it. viddik's capture engine is JVM-only, so wasm has no goldens for the same
    // reason Android has none (B-29), and the honest statement is that a green `check` says nothing
    // about how this renders in a browser either.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // A library with no executable binary sounds right and is refused: the Compose plugin needs
        // one so that the Skiko runtime is bundled for the tests that run on this target, and says
        // so by name. Nothing consumes the binary; it exists so the test bundle is complete.
        binaries.executable()
    }

    // **iOS, and it is here because something finally runs on it** — D14's rule, which the desktop,
    // Android and wasm targets each arrived under. `B-07`'s last open criterion was that the bundled
    // font stack is unverified on iOS *because the target does not exist*, and a criterion that can
    // only be closed by adding a target is what "arrives when something runs on it" means.
    //
    // Two targets and not three. `iosX64` is the Intel simulator; nothing here has one, so it would
    // be a target whose failures nobody looks at — the thing D14 exists to prevent. Adding it later
    // is additive and costs a line.
    iosArm64()
    iosSimulatorArm64()

    android {
        // Without this the Android resource pipeline is off and `variant.sources.assets` is null,
        // so compose-resources has nowhere to put the fonts and the artefact ships without them.
        androidResources { enable = true }

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

        // On-device tests. B-29: viddik's capture engine publishes JVM variants only, so Android
        // cannot have goldens inside `check` — and the alternative is not "nothing", it is a
        // number. `AndroidCameraProbeTest` solves the tilt's camera out of a rendered trapezoid on
        // the real renderer, which is a smaller claim than a picture and a claim that can be made.
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // Every public symbol here is currently unpinned: a renamed parameter, a `val` turned into a
    // function, a removed default are all invisible to `check` and all break a consumer at link
    // time. This library has already changed its public signatures twice without noticing —
    // `TiltIndication.cameraDistance` went Float to Dp, and the font functions became @Composable.
    // Both were right; neither arrived as a diff somebody had to approve.
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}

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
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.espresso)
            implementation(libs.androidx.compose.ui.test.manifest)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        // The simulator is a device the way a phone is, and Gradle drives it: `iosSimulatorArm64Test`
        // boots one, installs the test binary and runs it, so this needs no script and no Xcode
        // project. `compose.uiTest` publishes an `iosSimulatorArm64` variant — checked in its module
        // metadata before this line was written.
        //
        // `maybeCreate` and not `by getting`, which fails here with "KotlinSourceSet with name
        // 'iosTest' not found" — the intermediate set the default hierarchy template creates is not
        // there yet at this point in configuration, and the message names the set rather than the
        // ordering. What is asserted rather than assumed is that `src/iosTest` ends up in the iOS
        // test compilation: `IosFontStackTest` runs, and its results are written under
        // `build/test-results/iosSimulatorArm64Test`.
        val iosTest = maybeCreate("iosTest")
        iosTest.dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        // Rendering a real window in a test needs the host's skiko native library, and only
        // `currentOs` brings it in. It must not leak into a published source set: a POM that pins a
        // host-specific skiko artefact is broken for every other host.
        val desktopTest by getting
        desktopTest.dependencies {
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            // Reads the vendored token dump. Test-only on purpose: the dump is the source the
            // palette was transcribed from, not something the library carries at runtime.
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

// B-14's third criterion, as a check rather than a promise: the core must still declare no Material
// dependency once the adapter ships. Asserted over the runtime classpath a *consumer* resolves, not
// over the source, because the way this breaks is transitively — somebody adds a convenience that
// drags material3 in and nothing in the source of this module mentions it.
//
// Material 2 is a different matter and is not checked for: `compose.desktop.currentOs` brings it
// into the test source set, which is between us and skiko rather than between a consumer and this.
val resolvedForConsumers =
    configurations.named("desktopRuntimeClasspath").flatMap { configuration ->
        configuration.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            artifacts.map { it.id.componentIdentifier.displayName }
        }
    }

val noMaterialInTheCore by tasks.registering {
    // A `Provider`, not the configuration itself: a task that closes over a `Configuration` cannot
    // be serialised for the configuration cache and fails with a null field at execution time.
    val components = resolvedForConsumers
    inputs.property("components", components)
    doLast {
        val offenders = components.get().filter { "material3" in it }
        check(offenders.isEmpty()) {
            "kvadrant-core resolves Material 3, which it must not: ${offenders.joinToString()}"
        }
    }
}

tasks.named("check") { dependsOn(noMaterialInTheCore) }

// B-06. The specification is several hundred numbers, and typing them is several hundred chances to
// be wrong in a way no test catches - a test written from the same source, by the same person, on
// the same afternoon restates the mistake rather than finding it. So the vendored dump is the source
// and `KvadrantTokens.kt` is its output.
//
// Two tasks rather than one: `generateKvadrantTokens` writes the file, and `check` runs the same
// script in `--check` mode so that a dump edited without regenerating is a red build rather than a
// silent divergence. The generated file is committed - a consumer reading the sources should see the
// constants, and a reviewer should see the diff.
val tokensScript = layout.projectDirectory.file("../scripts/generate_tokens.py")
val tokensJson =
    layout.projectDirectory.file("../reference/metro-compose-brief/references/metro-tokens.json")
val tokensOutput =
    layout.projectDirectory.file("src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantTokens.kt")
val repositoryRoot = rootProject.layout.projectDirectory

val generateKvadrantTokens by tasks.registering(Exec::class) {
    description = "Regenerate KvadrantTokens.kt from the vendored metro-tokens.json."
    inputs.file(tokensScript)
    inputs.file(tokensJson)
    outputs.file(tokensOutput)
    workingDir = repositoryRoot.asFile
    commandLine("python3", "scripts/generate_tokens.py")
}

val checkKvadrantTokens by tasks.registering(Exec::class) {
    description = "Fail if KvadrantTokens.kt no longer matches the JSON it is generated from."
    inputs.file(tokensScript)
    inputs.file(tokensJson)
    inputs.file(tokensOutput)
    workingDir = repositoryRoot.asFile
    commandLine("python3", "scripts/generate_tokens.py", "--check")
}

tasks.named("check") { dependsOn(checkKvadrantTokens) }

// B-18, the half of it that is a programmer's job. Nothing in the icon set can be copied — Segoe
// MDL2 Assets and Segoe Fluent Icons are proprietary and no open Metro-styled equivalent exists —
// so every glyph is drawn here, and transcribing path data by hand is how a wrong shape gets into a
// set and survives review looking like a decision.
val iconsScript = layout.projectDirectory.file("../scripts/generate_icons.py")
val iconsSource = layout.projectDirectory.dir("../icons/svg")
val iconsOutput =
    layout.projectDirectory.file("src/commonMain/kotlin/io/github/youndie/kvadrant/icons/KvadrantIcons.kt")

val generateKvadrantIcons by tasks.registering(Exec::class) {
    description = "Regenerate KvadrantIcons.kt from the SVG drawings in icons/svg."
    inputs.file(iconsScript)
    inputs.dir(iconsSource).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(iconsOutput)
    workingDir = repositoryRoot.asFile
    commandLine("python3", "scripts/generate_icons.py")
}

val checkKvadrantIcons by tasks.registering(Exec::class) {
    description = "Fail if KvadrantIcons.kt no longer matches the drawings it is generated from."
    inputs.file(iconsScript)
    inputs.dir(iconsSource).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(iconsOutput)
    workingDir = repositoryRoot.asFile
    commandLine("python3", "scripts/generate_icons.py", "--check")
}

tasks.named("check") { dependsOn(checkKvadrantIcons) }

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

/** viddik's own default, restated here so the property override has something to fall back to. */
val defaultChannelTolerance = 2

// The screenshot suite has to be part of the one gate. Left out of `check` it becomes a command
// somebody remembers to run, which means it runs on one machine and not on the branch.
viddik {
    verifyOnCheck.set(true)
    // Left at viddik's default. B-35 measured what raising it buys and the answer is: not enough
    // to matter and too much to keep. On the Linux runner 34 goldens differ at 2, 21 at 4, 16 at 8
    // and still 10 at 32 — and closing the remainder would mean taking the *percentage* limit from
    // 0.05 % to about 1.4 %, which was measured to forgive a real regression: changing the button's
    // text from SemiBold to Normal moves 0.27 % of the pixels. A tolerance that admits the
    // difference between two rasterisers admits that too.
    //
    // Overridable so the sweep can be run again after a Compose or skiko bump:
    //   ./gradlew :kvadrant-core:viddikVerify --rerun-tasks -PviddikChannelTolerance=N
    channelTolerance.set(
        providers.gradleProperty("viddikChannelTolerance").map(String::toInt).orElse(defaultChannelTolerance),
    )
}

// **The artefact is unpacked and checked, because losing its resources is silent** (B-37).
//
// The Android resource pipeline is off by default in AGP's Kotlin Multiplatform plugin. With it off
// `variant.sources.assets` is null, compose-resources has nowhere to put the fonts, its copy task
// never enters a task graph, and the AAR comes out with a manifest, a classes.jar and nothing else —
// green. A consumer then gets `kvadrantLatin()` resolving to nothing and the platform substituting
// its own face, which reads as a slightly different design decision rather than a missing asset.
//
// Nothing in `check` could see that: the suite is desktop-only by B-29's decision, and the on-device
// test that would have caught it needed the very fonts that were missing. So the guard is an
// assertion about the file.

/** Five Selawik faces and the Source Sans variable. */
val expectedFonts = 6

/** Selawik's and Source Sans 3's, both required by the POM's own licence declaration. */
val expectedLicences = 2

val androidArtefactCarriesItsFonts by tasks.registering {
    description = "Fail if the AAR has lost the bundled fonts."
    val aar = layout.buildDirectory.file("outputs/aar/kvadrant-core.aar")
    // Copied into locals: a `doLast` that reads a script-level property captures the script object,
    // which the configuration cache refuses to serialise and says so only when it tries.
    val wantedFonts = expectedFonts
    val wantedLicences = expectedLicences
    dependsOn("assemble")
    inputs.file(aar)
    doLast {
        val entries =
            ZipFile(aar.get().asFile).use { zip ->
                zip
                    .entries()
                    .asSequence()
                    .map { entry -> entry.name }
                    .toList()
            }
        val fonts = entries.filter { it.endsWith(".ttf") }
        val licences = entries.filter { it.endsWith("-OFL.txt") }
        check(fonts.size == wantedFonts) {
            "the AAR carries ${fonts.size} font files, not $wantedFonts — an Android consumer " +
                "would get the platform's own face and no error: $entries"
        }
        check(licences.size == wantedLicences) {
            "the AAR carries ${licences.size} OFL texts, not $wantedLicences, so it ships fonts " +
                "without the licence that permits it"
        }
    }
}

tasks.named("check") { dependsOn(androidArtefactCarriesItsFonts) }
