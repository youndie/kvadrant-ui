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

So the choice is between tools nobody here has measured:

- **Robolectric-based, on the JVM** (Roborazzi and relatives). It would run inside `check` with no
  device, which is the whole prize. **The question that decides it is whether a host-side Android
  renderer reproduces `RenderNode.setCameraDistance` at all.** If it does not, the golden is a
  picture of a tilt that never tilted, and the suite would go green over the exact defect it was
  built to catch — which is worse than having no suite, because it also stops anyone looking.
- **Instrumented, on a device or emulator.** Real hwui, therefore real answers, and it cannot run in
  the default `check`. A guard that only runs when somebody remembers to plug a phone in is a guard
  that reports on whichever branch its owner was standing on.

**Start with the measurement, not the tool.** Render one bottom-edge press under the candidate and
solve the camera out of the trapezoid the way `CameraProbeTest` does — 1.0461 on the real device,
1.0487 on the desktop. A candidate that comes back without perspective has answered the question,
and it has answered it in an afternoon rather than after a suite exists.

- AC: the perspective probe is run under the candidate renderer and the number is written down,
  whatever it is.
- AC: if a host-side renderer works, the Android goldens are in `check` and the suite is shown to
  fail on a changed golden rather than assumed to.
- AC: if it does not, that is written into research §1.9 as a confirmed absence, and the Android
  guard becomes an explicit, named, device-only task — with the contributing notes saying so, so a
  green `check` is not read as covering Android.
- Anchors: `kvadrant-core/build.gradle.kts`, `kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/indication/CameraProbeTest.kt`
