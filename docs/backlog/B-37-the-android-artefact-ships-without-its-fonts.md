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

## What was tried

Wiring both tasks into their variant's generated assets through AGP's variant API —
`androidComponents.onVariants { variant.sources.assets?.addGeneratedSourceDirectory(...) }` — which
would assign the missing directory *and* put the result in the package. **The block never runs.** No
logging from inside it reaches the build output, in a module using AGP's Kotlin Multiplatform
library plugin. Whether `androidComponents` is the wrong entry point there, or the variants are
produced by a different mechanism, is the first thing to establish.

## Acceptance

- AC: `kvadrant-core.aar` contains the six font files and both OFL texts, checked by unpacking it in
  a test rather than by looking once.
- AC: `AndroidFontStackTest` passes on a device — the bundled companion renders something other than
  the platform's substitution. It is written and currently fails for this reason.
- AC: the same holds for `kvadrant-material-adapter`, which gained an Android target in B-04 and has
  never been unpacked.
- AC: something fails when an artefact loses its resources again. The AAR is built by a task nobody
  reads the output of, so the guard has to be an assertion about the file, not a green build.
- Anchors: `kvadrant-core/build.gradle.kts`, `kvadrant-core/src/androidDeviceTest/`
