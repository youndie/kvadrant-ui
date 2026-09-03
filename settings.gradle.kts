pluginManagement {
    repositories {
        // The Android Gradle plugin is published only here. Filtered like every other third-party
        // repository in this file - an unfiltered one takes part in resolving *every* plugin, and
        // the day its host is unreachable Gradle disables it and fails plugins that live elsewhere.
        google {
            content {
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("androidx")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // The viddik Gradle plugin is not on the plugin portal, and neither are the build
        // conventions. This has to be spelled out by hand: `pluginManagement` is evaluated before
        // any settings plugin is applied — including the sborka one, which is fetched through it.
        maven("https://reposilite.kotlin.website/snapshots") {
            content { includeGroupAndSubgroups("ru.workinprogress") }
        }
    }
}

plugins {
    // Where dependencies are looked for, and it is the same list this file used to spell out:
    // google() and mavenCentral() with their group filters, and the Reposilite viddik and the
    // toolkits are published to — filtered there too, because an unfiltered repository takes part in
    // resolving *every* dependency, so the day its host is unreachable Gradle disables it and fails
    // artefacts that live elsewhere.
    //
    // It also brings the shared `.editorconfig` check, which is the other half of pinning the
    // formatter's version.
    id("ru.workinprogress.sborka.settings") version "0.1.0.23"
}

rootProject.name = "kvadrant-ui"

include(":kvadrant-core")
include(":kvadrant-material-adapter")
// B-34. The previews the documentation site mounts, in a module of their own so that the site and
// the library are not the same artefact.
include(":kvadrant-previews")
include(":sample")
include(":sample-android")
