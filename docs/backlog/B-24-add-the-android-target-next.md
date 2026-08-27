---
id: B-24
title: "Add the Android target next, ahead of the plan"
status: open
priority: P0
size: M
stage: stage-1-core
blocked_by: [B-04]
---

# B-24 — Add the Android target next, ahead of the plan

[D14](../research/research-architecture.md) says desktop first and a target arrives when something
runs on it. This item is a deliberate exception with one reason: **Android is the only target that
is not skiko.** iOS, wasm and the desktop all render through Skia, so they corroborate each other
and nothing else; Android goes through `RenderNode` and hwui.

- **Everything measured so far was measured through one renderer.** The tilt geometry, the font
  x-heights and ink parity, every viddik golden — all of it is Skia's answer. That is not a
  criticism of the measurements; it is a statement of what they cover.
- **The first divergence was found before a single Android build.** `cameraDistance` is expressed in
  inches and the two backends convert it differently — a factor of six on a modern phone
  ([research §1.6](../research/research-architecture.md)). It was found by reading, which is the
  cheap way; the second and third will be found by running, which is the expensive way, and later.
- The rejected alternative is holding to the plan and adding Android in stage 2 with iOS and wasm.
  That gets the corroborating renderers first and the contradicting one last, which is the wrong way
  round: a target that agrees with what you already have teaches you nothing.
- Not covered: iOS and wasm. They are on skiko and can wait, exactly as D14 says.

- AC: `kvadrant-core` builds for `androidTarget()` and the sample renders on a device or emulator.
- AC: the existing common tests run on Android as well as desktop.
- AC: screenshot coverage on Android is **stated, not assumed** — viddik covers desktop, and whether
  it can cover Android here is part of this item rather than a hope
  ([B-16](B-16-screenshot-tests.md)).
- Anchors: `kvadrant-core/build.gradle.kts`, `gradle/libs.versions.toml`
