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
        // The viddik Gradle plugin is not on the plugin portal.
        maven("https://reposilite.kotlin.website/snapshots") {
            content { includeGroupAndSubgroups("ru.workinprogress") }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        // Compose Multiplatform pulls androidx artefacts published only to Google's repository.
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Screenshot testing — https://github.com/youndie/viddik. Filtered, like every third-party
        // repository here: an unfiltered one takes part in resolving *every* dependency, so the day
        // its host is unreachable Gradle disables it and fails artefacts that live elsewhere.
        maven("https://reposilite.kotlin.website/snapshots") {
            mavenContent { includeGroupAndSubgroups("ru.workinprogress") }
        }
    }
}

rootProject.name = "kvadrant-ui"

include(":kvadrant-core")
include(":kvadrant-material-adapter")
// B-34. The previews the documentation site mounts, in a module of their own so that the site and
// the library are not the same artefact.
include(":kvadrant-previews")
include(":sample")
include(":sample-android")
