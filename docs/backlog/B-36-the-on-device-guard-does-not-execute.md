---
id: B-36
title: "Android's only guard does not execute, and has not for some time"
status: open
priority: P0
size: M
stage: stage-2-release
blocked_by: []
---

# B-36 — Android's only guard does not execute, and has not for some time

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

## Acceptance

- AC: `connectedAndroidDeviceTest` runs `AndroidCameraProbeTest` to a verdict on a real device, and
  the verdict is written down with the device and API level beside it — a number measured on one
  Android version is a claim about that version.
- AC: `AndroidFontStackTest` — written and unrunnable today for the same reason — passes, which is
  what closes [B-07](B-07-font-stack.md)'s remaining criterion.
- AC: whatever the cause turns out to be is recorded in research, because two of the three
  Android-infrastructure defects here have been API-level behaviour changes and the fourth will be
  read against them.
- AC: something says when this stops running again. A guard that is outside `check` on purpose is a
  guard whose silence looks exactly like success, and that is how this went unnoticed.
- Anchors: `kvadrant-core/src/androidDeviceTest/`
