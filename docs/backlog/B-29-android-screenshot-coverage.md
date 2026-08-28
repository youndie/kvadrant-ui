---
id: B-29
title: "Android has no screenshot coverage, and viddik cannot give it"
status: done
priority: P1
size: L
stage: stage-2-release
blocked_by: []
---

# B-29 — Android has no screenshot coverage, and viddik cannot give it

**Decided: Android stays uncovered by pictures, and gets a number instead.** The second option, in
the item's own words. Growing viddik an Android target is work in another repository and remains the
only path to Android goldens inside `check`; nothing here blocks it, and this does not pretend to
replace it.

`AndroidCameraProbeTest` runs on a real device and solves the tilt's camera out of a rendered
trapezoid — the same arithmetic as the desktop probe, on the renderer that showed B-25. It passes on
a Pixel 6a at API 17. `./gradlew :kvadrant-core:connectedAndroidDeviceTest`, and it is **not** in
`check`: a gate that needs hardware gets skipped, and a skipped gate reads as a green one. `CLAUDE.md`
now says a green `check` covers no Android at all.

**Three things stood between the source set and the device, none of them this library's:**
compose-resources registers `copyAndroidDeviceTestComposeResourcesToAndroidAssets` without an
`outputDirectory` and fails configuration; `compose.uiTest` drags in espresso-core 3.5.0, which calls
`InputManager.getInstance` — removed in Android 17 — and dies with a `NoSuchMethodException` naming
neither Compose nor this project; and `runComposeUiTest` launches an activity that nothing declares
without `ui-test-manifest`. All three are worked around in `kvadrant-core/build.gradle.kts` with the
reason written beside each.

**And the probe found a defect in itself on its first real run.** `PressInteraction.Press` carries a
position in the element's own *pixels*, and both probes wrote the dp number straight in — correct on
the desktop by coincidence, since its density is 1. At 2.625 the same numbers press near the
top-left corner, the tile draws almost flat, and the camera solves to minus thirty-eight thousand
pixels. Fixed in both. A second renderer paid for itself on its first run for the second time.

[B-24](B-24-add-the-android-target-next.md) added the second renderer and
[B-25](B-25-tilt-camera-is-in-inches.md) immediately found a defect that only that renderer shows.
Nothing guards it. The fix was confirmed by a screenshot somebody took by hand, once, off a phone
that happened to be plugged in — which is not a check, it is an anecdote with a timestamp.

**This is not a wiring job, and the reason is worth stating before anyone tries.** viddik publishes
`viddik-annotations` for Android but its capture engine, `viddik-testing-core`, has exactly three
variants — `metadataApiElements`, `jvmApiElements-published`, `jvmRuntimeElements-published`. It is
JVM-only. Read out of the Gradle module metadata at 0.2.0.14, not assumed from the artefact names.

**A second screenshot tool is ruled out.** Not on evidence — on ownership: viddik is this project's
own, and running a Robolectric-based renderer beside it would mean two golden formats, two ways to
record, two things to keep in step with the Compose line, and a second answer to "is the suite
green". The cost of that lands on every future change; the cost of Android being uncovered lands
only where Android differs.

So the options are two, and neither is "pick something off the shelf":

- **viddik grows an Android target.** Its annotations already publish one; the capture engine is
  what is JVM-only. This is work in another repository and it is the only path that ends with
  Android goldens inside `check`.
- **Android stays uncovered, and that is written down as a confirmed absence.** Then the guard for
  anything Android-specific has to be numeric rather than pictorial — `CameraProbeTest` solves a
  camera out of a rendered trapezoid and would work anywhere the geometry can be measured, which is
  a smaller claim than a golden and a claim that can actually be made.

**Either way, start with the measurement.** The probe is one bottom-edge press and the arithmetic
that turns it into a camera distance: 1.0461 on the real device against 1.0487 on the desktop. It
costs an afternoon, it is the thing any tooling decision rests on, and it is worth having before
either option is chosen.

- AC: the decision between the two is recorded with its reason, not left to whoever next needs an
  Android golden.
- AC: if Android stays uncovered, that is written into research §1.9 as a confirmed absence, and the
  contributing notes say so, so a green `check` is never read as covering Android.
- AC: whichever way it goes, at least one Android-specific behaviour is guarded by a number rather
  than by a person having looked once — B-25 is currently guarded by neither.
- Anchors: `kvadrant-core/build.gradle.kts`, `kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/indication/CameraProbeTest.kt`
