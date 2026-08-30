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
    // The build conventions: the coordinate, the version, the toolchain, the jvm floor, the style,
    // the test platform and the publication. Declared here and applied per module, `apply false`
    // like everything else in this block.
    alias(libs.plugins.sborkaKmp) apply false
    alias(libs.plugins.sborkaLint) apply false
    alias(libs.plugins.sborkaPublish) apply false
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

// The group, the version, the ktlint wiring, the test platform and the publication lived here, in
// two `subprojects { }` blocks. They come from `ru.workinprogress.sborka` now, applied per module,
// with the numbers one line each in `gradle.properties`.
//
// WHAT DID NOT MOVE, and both are about this host rather than about publishing in general.

// **One repository, `/snapshots`, whatever the version says** — and the first version of this
// derived the destination from the version instead, which was a good argument built on an
// assumption nobody had tested ([B-46](docs/backlog/B-46-the-first-release.md)).
//
// The assumption was that this identity may write to both trees. It may not: the first publish that
// ever reached the host was refused with **403** on a PUT under `/releases/`, and 403 is a token
// that authenticated and was turned away, because Reposilite answers 401 for credentials that are
// missing or wrong — measured, both of them. The destination was therefore never a function of the
// version. It is a function of what the token is allowed to do, and that is one path:
// `/snapshots/io/github/youndie`. That is also the conventions' default, so nothing here sets it.
val reposiliteUrl = "https://reposilite.kotlin.website/snapshots"

val kvadrantVersion: String = providers.gradleProperty("version").get()

// Only for the guard below. A `-SNAPSHOT` is *meant* to be overwritten; a fixed version is not,
// wherever it happens to live.
val isSnapshot: Boolean = kvadrantVersion.endsWith("-SNAPSHOT")

// **The host refuses a republish; this only says so in words a person can act on.**
//
// That ordering is the opposite of what this comment claimed a day ago, and the claim was tested
// rather than reasoned away. Publishing `0.1.0` a second time was refused by Reposilite with **409
// Conflict** — in the `snapshots` tree, on a fixed version — so the assertion that "nothing between
// here and the disk says 0.1.0 is immutable except this" was simply false. The tree's name does not
// decide that; the version's shape does, and the host knows the difference between `0.1.0` and
// `0.1.0-SNAPSHOT` as well as we do.
//
// **And that run is also how this check was caught not running.** It sat on the multiplatform
// publication's task alone, on the argument that only that task writes the root POM. True, and
// beside the point: Gradle runs the ten publication tasks in whatever order it likes, and the
// `android` one reached the host first and died on 409 before the guarded task started. Twice now
// this has been attached somewhere that does not execute — first an aggregate task `publish` never
// reaches, then a task that loses a race — which is an argument against *choosing* the place at all.
//
// So: every publication task, each asking about **its own** artefact. `PublishToMavenRepository`
// carries the publication and the repository it is about, read at execution time, so there is no
// task name to match and no artefact id to reconstruct. Whichever task starts first is the one that
// speaks, and it speaks about the thing it was about to overwrite.
//
// A GET rather than a HEAD: a HEAD carries no body, so its status is one nobody had to produce a
// document to justify. A `-SNAPSHOT` is exempt, which is the whole of what a snapshot is.
//
// The repository is called `wip` rather than `reposilite` since the publication came from the shared
// conventions. The name is matched rather than assumed because `publishToMavenLocal` is a
// `PublishToMavenLocal` — a different type, not caught by this — while a second remote repository
// added later would be, and it would be a different host with different rules.
subprojects {
    if (isSnapshot) return@subprojects
    tasks.withType<PublishToMavenRepository>().configureEach {
        doFirst {
            if (repository.name != "wip") return@doFirst
            val artefact = publication.artifactId
            val coordinates = "io/github/youndie/$artefact/$kvadrantVersion/$artefact-$kvadrantVersion.pom"
            val url = java.net.URI("$reposiliteUrl/$coordinates").toURL()
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply { requestMethod = "GET" }
            val code = runCatching { connection.responseCode }.getOrDefault(-1)
            connection.disconnect()
            check(code != 200) {
                "$artefact $kvadrantVersion is already published — $coordinates answers 200, and the " +
                    "host would refuse the upload with 409. Raise `version` in gradle.properties"
            }
        }
    }
}
