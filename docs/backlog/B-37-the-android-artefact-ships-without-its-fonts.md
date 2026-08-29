---
id: B-37
title: "The Android artefact ships without its fonts, and the build is green about it"
status: open
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

## Diagnosed: AGP 9.3.2 returns no assets container for a KMP variant

**The Compose plugin is doing everything right.** It logs
`Configure compose resources with KotlinMultiplatformAndroidComponentsExtension`, so its Kotlin
Multiplatform branch runs and its AGP version gate (`>= 8.10`) passes. It then asks the variant for
its assets container to hang the copy task on, and:

```
KmpComponentImpl$KmpSourcesImpl.assets == null      // AGP 9.3.2, androidMain and androidTest
```

There is nothing to attach the task to, so `copyAndroid*ComposeResourcesToAndroidAssets` never
enters any task graph, `outputDirectory` is never assigned, and no assets reach the artefact.
Confirmed from three directions:

- the AAR has four entries and no `assets/`;
- `sample-android`'s APK has `res/` and `resources.arsc` but **no `assets/` at all**, so this is not
  a library-packaging question but a whole-pipeline one;
- a plain `src/androidMain/assets/probe.txt` does not arrive either — nothing of ours is involved.

**This is a version incompatibility, not a configuration mistake**, and it is one this repository was
pinned into rather than chose: Gradle 9.7.1 forces AGP 9.x, AGP 9 forbids `com.android.library` in a
Kotlin Multiplatform module, so the only Android target available here is the one whose `sources`
implementation returns null for assets (research §1.13). The same code on an AGP 8.x line, where
that container exists, would work untouched.

**Java resources do arrive.** `src/androidMain/resources/probe.txt` lands inside `classes.jar`, and
`sources.resources` is a real `SourceDirectories.Flat`. That is the only delivery path this module
currently has.

## Three routes

1. **Establish which AGP line has a non-null assets container and whether this build can sit on it.**
   The cheapest thing to know first, and the answer decides the other two. Gradle 9.7.1 is what
   forces AGP 9; whether AGP 9.x regains it in a later patch is a question for its release notes
   rather than for this repository.
2. **Wait**, and leave the library silently broken on Android meanwhile — which is where it is now.
3. **Load the fonts from the classpath on Android**: ship the bytes as Java resources, read them in
   an `androidMain` implementation, keep `kvadrantLatin()` and `kvadrantCyrillic()` unchanged. Worth
   weighing against B-07's own argument, which moved this project *to* compose-resources precisely
   to stop reading fonts off a classpath — that argument was made about the JVM, and this would
   restore the practice for one target.

## Two wrong diagnoses on the way here, both worth keeping

**"The `onVariants` block never runs."** It runs. The build had reused a configuration-cache entry,
so nothing at configuration time re-executed and the logging that would have said so never appeared —
and the same thing had, minutes earlier, made a probe listing the project's extensions print nothing
at all. **Any diagnosis of configuration-time behaviour here needs `--no-configuration-cache`**, or
it is a diagnosis of a recording rather than of a build.

**"A KMP Android library cannot package assets."** Too broad. It cannot on *this* AGP, and the
mechanism is one null field rather than an absent feature. The difference matters: the first version
of that sentence would have sent the next reader to redesign font loading, when the first thing to
check is a version.

## Acceptance

- AC: the six font files and both OFL texts reach an Android consumer — in `assets/` if route 1,
  inside `classes.jar` if route 2 — checked by unpacking the artefact in a test rather than by
  looking once.
- AC: `AndroidFontStackTest` passes on a device — the bundled companion renders something other than
  the platform's substitution. It is written and currently fails for this reason.
- AC: the same holds for `kvadrant-material-adapter`, which gained an Android target in B-04 and has
  never been unpacked.
- AC: something fails when an artefact loses its resources again. The AAR is built by a task nobody
  reads the output of, so the guard has to be an assertion about the file, not a green build.
- Anchors: `kvadrant-core/build.gradle.kts`, `kvadrant-core/src/androidDeviceTest/`
