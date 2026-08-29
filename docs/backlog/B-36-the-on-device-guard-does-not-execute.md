---
id: B-36
title: "Android's only guard does not execute, and has not for some time"
status: done
priority: P0
size: M
stage: stage-2-release
blocked_by: []
---

# B-36 — Android's only guard does not execute, and has not for some time

## Not reproducing, and the failure it was hiding turned out to be the real one

With the device connected again, `connectedAndroidDeviceTest` **runs**.
`AndroidCameraProbeTest` passes, so B-29's on-device number is being produced. The "No compose
hierarchies found in the app" failure this item was opened for did not recur, and nothing in this
repository changed that should explain it — the two hypotheses it eliminated are still eliminated,
and no third was applied. A stale install on the device is the likeliest candidate and is a guess.

**So this item stays open at a lower value than it was written with, and its trigger is what
matters:** if that message returns, this is where the two dead ends are already recorded.

What the run did produce is [B-37](B-37-the-android-artefact-ships-without-its-fonts.md) — the AAR
ships no fonts at all — which was invisible for exactly as long as nothing executed on Android.

`./gradlew :kvadrant-core:connectedAndroidDeviceTest` fails on a connected Pixel 6a before it
measures anything:

```
java.lang.IllegalStateException: No compose hierarchies found in the app.
  at androidx.compose.ui.test.TestOwnerKt.getAllSemanticsNodes
  at AndroidCameraProbeTest.kt:82   // the first captureToImage
```

**This is the whole of what Android has.** [B-29](B-29-android-screenshot-coverage.md) decided that
viddik's JVM-only capture engine means Android gets a *number* instead of goldens, and
`AndroidCameraProbeTest` is that number: it solves the tilt's camera out of a trapezoid rendered on
the real renderer. `CLAUDE.md` tells a reader to "run it before trusting anything about the renderer
most of this will ship on". It does not run. A green `check` said nothing about Android by design;
what nobody knew is that the thing standing in for `check` said nothing either.

## What it is not

- **Not the test manifest.** The obvious reading is that
  `androidx.compose.ui:ui-test-manifest` is not being merged into the APK that AGP's Kotlin
  Multiplatform plugin builds for `withDeviceTest`, the way it would be for `androidTest` in a
  classic library module. Checked: the merged manifest at
  `build/intermediates/merged_manifest/androidDeviceTest/` already declares
  `androidx.activity.ComponentActivity`, exported and themed, **without** a hand-written manifest
  being added. Adding one anyway changes nothing — verified by adding it, watching the failure
  repeat, then removing it again rather than leaving a file whose effect could not be shown.
- **Not a version skew.** `androidx.compose.ui:ui-test`, `ui-test-manifest` and `ui-android` all
  resolve to 1.12.0 on the device-test runtime classpath.
- **Not the tilt maths.** The failure is at the first `captureToImage`, before any arithmetic.

## Where to look next

The device is on **Android 17 (API 37)**, which is also the `compileSdk` the version catalog was
forced to by AAR metadata. The two previous Android-infrastructure defects in this repository were
both API-level behaviour changes — Espresso 3.5.0 calling a removed `InputManager.getInstance`, and
a compose-resources copy task with no output directory — so the shape is familiar. The next step is
to find out whether the activity launches *at all*: `adb shell am start` it by hand, or run the
instrumentation with `-e debug false` and read logcat while the test runs, rather than reading the
Compose-side message, which describes a consequence.

## The number B-29 promised had never been kept

Building the record turned this up, and it is the more interesting half. [B-29](B-29-android-screenshot-coverage.md)
decided that Android gets a **number** where the desktop gets goldens, because viddik's capture
engine is JVM-only. `AndroidCameraProbeTest` computes that number — it solves the tilt's camera out
of the drawn trapezoid — and then **compared it against a six per cent window and threw it away**. So
two runs a year apart, on two Android versions, were indistinguishable as long as both passed. A
tolerance says whether a run was acceptable; only the number says what changed.

It is logged now and the record carries it. The first reading, and the thing this item was opened to
make possible:

| Device | Android | Reading |
|---|---|---|
| Pixel 6a | 37 | `solved=1526 declared=1512 density=2.625 trapezoid=806/748` |

Nine tenths of one per cent between what the library asked for and what hwui drew, off a real
trapezoid on a real phone. That is [B-25](B-25-tilt-camera-is-in-inches.md)'s fix holding on the
renderer it was written for.

*And the first version of that line recorded `density=2,625`.* `String.format` follows the **device's**
locale, so a machine-readable line written for a record file comes out with a decimal comma on a
phone set to most of Europe — a number that no longer parses, produced by a passing test. `Locale.ROOT`.

## The mechanism, which is the half that did not need a device

**The cause never reproduced and the investigation is not what closes this.** What does is the
fourth criterion, and building it settled what it could honestly be.

It cannot be a gate. Failing `check` when the recorded run is older than the code would block every
change to the tilt on somebody owning a phone — and a check that cannot be satisfied honestly is one
people satisfy dishonestly, by editing the record. That is worse than no check.

So the mechanism converts silence into **a number that grows**. `make android` drives the guard and
writes a row into [docs/android-device-runs.md](../android-device-runs.md) — date, device, API level,
commit — and **only for a run that passed**, so a row is evidence rather than an intention. Every
`make report`, locally and in CI, then prints how long ago that was and *which of the sources the
claim depends on have moved since*. Absence is invisible; a count with a list of files under it is
not.

What *is* a gate is that the record exists and holds at least one row, because that costs nothing and
stops the file being quietly deleted. It is the only evidence in this repository that anything has
ever executed on Android.

**The honest boundary.** `CameraProbeTest` runs the same press and the same solver on the desktop
inside `check`, so the arithmetic and the geometry are held there whatever happens to the phone. What
only a device can catch is hwui disagreeing with skiko — which is exactly what
[B-25](B-25-tilt-camera-is-in-inches.md) was, and the reason any of this exists.

## Acceptance

- ~~AC: `connectedAndroidDeviceTest` runs `AndroidCameraProbeTest` to a verdict on a real device,
  and the verdict is written down with the device and API level beside it.~~ Done, and with the
  reading rather than only the verdict — see the section above.
  [docs/android-device-runs.md](../android-device-runs.md).
- ~~AC: `AndroidFontStackTest` passes.~~ Done, on the same run: two tests on a Pixel 6a at API 37,
  both green. [B-07](B-07-font-stack.md) already recorded its Android criterion as met on the
  strength of an earlier run; this is the first one that left evidence behind.
- ~~AC: whatever the cause turns out to be is recorded in research.~~ Done, and the entry says the
  cause **was never found**: the failure did not recur, no third hypothesis was applied, and a stale
  install is a guess. Recording a non-reproduction is worth as much as recording a cause here,
  because the next person to meet that message needs to know which two dead ends are already walked.
- ~~AC: something says when this stops running again.~~ Done — `scripts/android_guard.py`, `make
  android`, and a line in `make report`. The section above says why it is a growing number rather
  than a gate.
- Anchors: `kvadrant-core/src/androidDeviceTest/`
