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
    // Applied to the root as well as to the modules: in Dokka 2 the root's `dokkaGenerate` is the
    // aggregator, and the modules it aggregates are named as `dokka` dependencies below.
    alias(libs.plugins.dokka)
}

// The API reference for the site (B-34). Only the two published modules are in it — `sample` is a
// demo and `kvadrant-previews` is the documentation's own scaffolding, and a reference listing
// either would be documenting things nobody can depend on.
dependencies {
    dokka(project(":kvadrant-core"))
    dokka(project(":kvadrant-material-adapter"))
}

// One gate: `./gradlew check` runs the tests and ktlint. A lint that has to be remembered as a
// separate command is a lint that runs on somebody's machine and not on the branch.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint)
    }
}

// Publishing. B-21 answered: `io.github.youndie`, to the WIP Reposilite's snapshots, which is the
// same host `settings.gradle.kts` already resolves viddik from — no new infrastructure and no new
// secret in the tree. Maven Central and the Central Portal are not the destination today; when they
// are, this block gains a second repository rather than losing this one.
//
// Two modules publish and two do not. The samples are applications: publishing one is how a
// consumer ends up with a demo on their classpath.
val publishableModules = setOf("kvadrant-core", "kvadrant-material-adapter")

subprojects {
    if (name !in publishableModules) return@subprojects
    apply(plugin = "maven-publish")

    group = "io.github.youndie"
    // Snapshots, deliberately. A published coordinate cannot be renamed, only deprecated and
    // republished, and nothing here has been used by anyone yet.
    version = "0.1.0-SNAPSHOT"

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "reposiliteSnapshots"
                url = uri("https://reposilite.kotlin.website/snapshots")
                credentials {
                    // Never in the tree. A property for a workstation, an environment variable for
                    // anything automated, and a publish that finds neither fails **at
                    // configuration**, naming `credentials.username`, before it has built or sent
                    // anything. Measured by running the publish without them.
                    username =
                        providers.gradleProperty("REPOSILITE_USER").orNull
                            ?: System.getenv("REPOSILITE_USER")
                    password =
                        providers.gradleProperty("REPOSILITE_SECRET").orNull
                            ?: System.getenv("REPOSILITE_SECRET")
                }
            }
        }

        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set("Kvadrant UI")
                description.set(
                    "A Metro (Windows Phone 8) component library for Compose Multiplatform.",
                )
                url.set("https://github.com/youndie/kvadrant-ui")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        comments.set("Covers the code.")
                    }
                    // B-07's remaining criterion. The fonts bundled in `kvadrant-core` are under a
                    // different licence from the code, and a consumer's licence tooling reads the
                    // POM rather than the jar. Declaring one licence for an artefact that ships two
                    // is the kind of omission nobody notices until it is somebody's legal problem.
                    license {
                        name.set("SIL Open Font License 1.1")
                        url.set("https://openfontlicense.org/documents/OFL.txt")
                        comments.set(
                            "Covers the bundled Selawik and Source Sans 3 faces, not the code. " +
                                "The full text ships in the artefact as Selawik-OFL.txt and " +
                                "SourceSans3-OFL.txt.",
                        )
                    }
                }
                developers {
                    developer {
                        id.set("youndie")
                        url.set("https://github.com/youndie")
                    }
                }
                scm {
                    url.set("https://github.com/youndie/kvadrant-ui")
                }
            }
        }
    }

    // The snapshots repository takes snapshots. Reposilite would reject a release anyway, but it
    // would reject it after the build had spent its time and with a message about HTTP rather than
    // about versions.
    tasks.matching { it.name == "publishAllPublicationsToReposiliteSnapshotsRepository" }.configureEach {
        val declared = version.toString()
        doFirst {
            check(declared.endsWith("-SNAPSHOT")) {
                "version $declared is not a snapshot, and this repository is the snapshots one"
            }
        }
    }
}
