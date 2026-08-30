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

// Publishing. B-21 answered: `io.github.youndie`, to the WIP Reposilite, which is the same host
// `settings.gradle.kts` already resolves viddik from — no new infrastructure and no new secret in
// the tree. Maven Central and the Central Portal are not the destination today; when they are, this
// block gains a second repository rather than losing this one.
//
// Two modules publish and two do not. The samples are applications: publishing one is how a
// consumer ends up with a demo on their classpath.
val publishableModules = setOf("kvadrant-core", "kvadrant-material-adapter")

// **One repository, `/snapshots`, whatever the version says** — and the first version of this
// derived the destination from the version instead, which was a good argument built on an
// assumption nobody had tested ([B-46](docs/backlog/B-46-the-first-release.md)).
//
// The assumption was that this identity may write to both trees. It may not: the first publish that
// ever reached the host was refused with **403** on a PUT under `/releases/`, and 403 is a token
// that authenticated and was turned away, because Reposilite answers 401 for credentials that are
// missing or wrong — measured, both of them. The destination was therefore never a function of the
// version. It is a function of what the token is allowed to do, and that is one path:
// `/snapshots/io/github/youndie`.
//
// **The name of that path is the host's, not a claim about these artefacts.** `0.1.0` is a fixed
// coordinate that will not move under anybody, and it lives there because that is where this
// project may write — which is also how the neighbours on this host use it: `io.github.youndie:
// form-core` has ninety fixed versions in the same tree and not one `-SNAPSHOT` among them.
val REPOSITORY = "https://reposilite.kotlin.website/snapshots"

val kvadrantVersion: String = providers.gradleProperty("kvadrant.version").get()

// Only for the guard below. A `-SNAPSHOT` is *meant* to be overwritten; a fixed version is not,
// wherever it happens to live.
val isSnapshot: Boolean = kvadrantVersion.endsWith("-SNAPSHOT")

subprojects {
    if (name !in publishableModules) return@subprojects
    apply(plugin = "maven-publish")

    group = "io.github.youndie"
    // In `gradle.properties`, because the README quotes it too and a version that moves in one
    // place and not the other hands a reader a coordinate that does not resolve.
    version = kvadrantVersion

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "reposilite"
                url = uri(REPOSITORY)
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

    // **A fixed version is published once, and this refuses the second time** — which now matters
    // *more* than it did, not less. The repository is called snapshots and holds fixed versions, so
    // the one thing that used to prevent an accidental overwrite — the host refusing a redeploy to
    // its releases tree — is not in play. Nothing between here and the disk says `0.1.0` is
    // immutable except this.
    //
    // A GET rather than a HEAD, deliberately: a HEAD carries no body, so a server that answers one
    // by inventing a status tells you nothing you can check, and this asks for the POM itself.
    // A `-SNAPSHOT` is exempt, which is the whole of what a snapshot is.
    if (!isSnapshot) {
        // The module's own name, captured here: inside `configureEach` the receiver is the task and
        // `name` would be the task's.
        val artefact = name
        // **On the multiplatform publication's own task, and the first version of this hung it on
        // `publishAllPublicationsToReposiliteRepository` — which `./gradlew publish` never runs.**
        // Gradle's `publish` depends on the individual `publish<Pub>PublicationTo<Repo>Repository`
        // tasks directly; the `All` task is a separate aggregate that nothing in the publish path
        // reaches. `./gradlew publish --dry-run` lists ten tasks and that is not one of them, so the
        // guard had never executed once, including on the run that reached the host.
        //
        // This one and not all ten, because the check has to be about something **only this task
        // writes**. The root POM is exactly that — the per-target publications write
        // `-android`, `-desktop` and the rest — so no sibling can trip it inside the same run by
        // uploading first, which is what checking one shared address from ten tasks would do.
        tasks.matching { it.name == "publishKotlinMultiplatformPublicationToReposiliteRepository" }.configureEach {
            // The root module of the KMP publication. Its per-target siblings — `-jvm`, `-android`
            // — go up in the same publish, so one of them existing means all of them do.
            val coordinates = "io/github/youndie/$artefact/$kvadrantVersion/$artefact-$kvadrantVersion.pom"
            doFirst {
                val url = java.net.URI("$REPOSITORY/$coordinates").toURL()
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply { requestMethod = "GET" }
                val code = runCatching { connection.responseCode }.getOrDefault(-1)
                connection.disconnect()
                check(code != 200) {
                    "$artefact $kvadrantVersion is already published — $coordinates answers 200. A fixed " +
                        "version is not republished: raise `kvadrant.version` in gradle.properties"
                }
            }
        }
    }
}
