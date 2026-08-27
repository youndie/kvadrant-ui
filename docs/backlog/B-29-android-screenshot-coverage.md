---
id: B-29
title: "Android has no screenshot coverage, and viddik cannot give it"
status: open
priority: P1
size: L
stage: stage-2-release
blocked_by: []
---

# B-29 — Android has no screenshot coverage, and viddik cannot give it

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
