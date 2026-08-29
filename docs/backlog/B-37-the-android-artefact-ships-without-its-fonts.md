---
id: B-37
title: "The Android artefact ships without its fonts, and the build is green about it"
status: done
priority: P0
size: M
stage: stage-2-release
blocked_by: []
---

# B-37 — The Android artefact ships without its fonts, and the build is green about it

`kvadrant-core.aar` contains a manifest, a `classes.jar` and **nothing else**. No `assets/`, no
`composeResources/`, no `.ttf`. A consumer taking this library on Android gets `kvadrantLatin()` and
`kvadrantCyrillic()` resolving to nothing, and the platform quietly substituting its own face.

**Measured on a Pixel 6a**, which is how it was found rather than reasoned about. The same string
rendered through three different families comes back to the same pixel:

| family | "settings" | "настройки" |
|---|---|---|
| `kvadrantLatin()` | 6195 | 7965 |
| `kvadrantCyrillic()` | — | 7965 |
| `FontFamily.SansSerif` | 6195 | 7965 |

The family argument has no effect at all, because none of the three is present. On the desktop the
same probe separates every family by hundreds of pixels.

## Why nothing caught it

The compose-resources plugin registers a `copyAndroid<Variant>ComposeResourcesToAndroidAssets` task
per Android variant and leaves `outputDirectory` unset on both. Invoked directly, each fails at
configuration:

```
property 'outputDirectory' doesn't have a configured value
```

**Neither is in `assemble`'s task graph.** So the AAR is built without ever running one, the build is
green, and the artefact is missing the thing B-07 exists to put in it.

Two guards should have caught this and could not. `check` has never executed anything on Android —
that is B-29's decision and it is honest about it. And the device suite, which is the substitute,
had its own copy of the same defect *worked around*: the task's output was pointed at an empty
directory so the tests could start, under a comment reading "the device test source set has no
resources of its own — it renders shapes, not text". True when the only device test solved a
trapezoid; false the moment one measured a font, and by then the comment read as a reason rather
than as an assumption.

## Fixed: the Android resource pipeline is off by default

```kotlin
kotlin {
    android {
        androidResources { enable = true }
    }
}
```

That is the whole of it. AGP's Kotlin Multiplatform plugin ships with Android resources **disabled**,
and with them disabled `variant.sources.assets` is null. The Compose plugin does everything right —
it logs `Configure compose resources with KotlinMultiplatformAndroidComponentsExtension`, its version
gate passes, it asks for the assets container to hang its copy task on, and gets nothing. So the task
never enters a graph, `outputDirectory` is never assigned, and the artefact ships without the fonts,
green.

With the line in place: `sources.assets` is a `LayeredSourceDirectoriesImpl`, the AAR carries six
`.ttf` files and both OFL texts under `assets/composeResources/`, `sample-android`'s APK carries them
too, and `AndroidFontStackTest` passes on a Pixel 6a — the bundled companion renders something other
than the platform's substitution, which is the assertion that was failing.

Applied to all three modules with an Android target: `kvadrant-core`, `kvadrant-material-adapter`
and `sample`. The workaround that used to point the device-test copy task at an empty directory is
**gone**; with the pipeline on, that task is wired properly and the device suite runs without it.

`androidLibrary { }` went with it — it is deprecated in favour of `android { }`, and the deprecation
is an error in a Kotlin script rather than a warning.

## The guard

`androidArtefactCarriesItsFonts` unpacks the AAR in `check` and counts what is inside: six fonts, two
licence texts. It exists because **nothing else could have caught this**. `check` is desktop-only by
B-29's decision, and the on-device test that would have noticed needed the very fonts that were
missing. Verified by turning `androidResources` back off and watching it say *"the AAR carries 0 font
files, not 6"*.

## Three wrong diagnoses on the way, all kept

1. **"The `onVariants` block never runs."** It runs. The build had reused a configuration-cache
   entry, so nothing at configuration time re-executed and the logging that would have said so never
   appeared. **Any diagnosis of configuration-time behaviour here needs `--no-configuration-cache`**,
   or it is a diagnosis of a recording.
2. **"A KMP Android library cannot package assets."** It can. The container is null only because the
   pipeline that creates it is switched off.
3. **"It is a version incompatibility this repository was pinned into."** It is not. The same null
   appears on AGP 9.2.0 and 9.3.2, and on Compose 1.11.1 and 1.12.0 — all four combinations were
   measured. Downgrading would have changed nothing and cost the toolchain.

Each of the three was stated with evidence and each was wrong, which is worth more than the fix: the
common thread is that a *missing* thing produces silence, and silence was read three times as
information about where the problem was.

## Acceptance

- ~~AC: the six font files and both OFL texts reach an Android consumer, checked by unpacking the
  artefact.~~ Done — `androidArtefactCarriesItsFonts`, in `check`.
- ~~AC: `AndroidFontStackTest` passes on a device.~~ Done, on a Pixel 6a.
- ~~AC: the same holds for `kvadrant-material-adapter`.~~ The same line is applied there and in
  `sample`; the adapter bundles no resources of its own, so there is nothing to count in it.
- ~~AC: something fails when an artefact loses its resources again.~~ Done, and verified by
  reinstating the defect.
- Anchors: `kvadrant-core/build.gradle.kts`, `kvadrant-core/src/androidDeviceTest/`
