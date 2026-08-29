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

## Diagnosed: an AGP Kotlin Multiplatform library cannot package assets at all

The obvious fix is to wire each copy task into its variant's generated assets through AGP's variant
API, which would assign the missing directory *and* put the result in the package. It cannot be
done, and the reason is one line:

```
variant.sources.assets == null      // for androidMain and for androidTest
```

`Sources.getAssets()` exists on the interface and returns **null** for a
`KotlinMultiplatformAndroidVariant`. Confirmed from the other end without any of our own machinery:
a plain `src/androidMain/assets/probe.txt` does not appear in the AAR either. The artefact comes out
with four entries — `R.txt`, `AndroidManifest.xml`, `classes.jar`, `aar-metadata.properties` — and
there is no assets path into it.

So compose-resources is not doing anything wrong that this repository can correct. Its Android
delivery is assets, and this module has no assets.

**Java resources, in contrast, do arrive.** `src/androidMain/resources/probe.txt` lands inside
`classes.jar`, and `sources.resources` is a real `SourceDirectories.Flat`. That is the foundation
for the only fix available on our side.

## Two routes, and neither is a build tweak

1. **Wait for AGP or the Compose plugin.** A KMP Android library that cannot ship assets is an
   upstream gap; compose-resources depends on the thing it does not have. Cheapest, and leaves the
   library broken on Android meanwhile — which is where it is now, silently.
2. **Load the fonts on Android from the classpath instead of from assets.** The bytes can be shipped
   as Java resources, which is verified above, and read by an `androidMain` implementation rather
   than through `Res.font`. That keeps one public API — `kvadrantLatin()` and `kvadrantCyrillic()`
   stay as they are — and confines the platform difference to how the bytes are found. It is the
   opposite of the argument that moved this project to compose-resources in the first place (B-07:
   "one declaration serves every target"), and that argument is worth re-reading before choosing:
   it was made against a *classpath read*, which is what this would restore for one target.

## The first diagnosis of this was wrong, and it is worth knowing why

It concluded the `onVariants` block never runs, because no logging from inside it ever appeared. The
block runs. The build had **reused a configuration-cache entry**, so nothing at configuration time
re-executed and the logging could not appear. The same thing had, minutes earlier, made a probe that
listed the project's extensions print nothing at all.

Any diagnosis of configuration-time behaviour in this repository needs `--no-configuration-cache`,
or it is a diagnosis of a recording rather than of a build.

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
