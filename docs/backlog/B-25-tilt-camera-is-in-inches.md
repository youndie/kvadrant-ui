---
id: B-25
title: "The tilt camera is measured in inches, and the backends disagree about it"
status: open
priority: P0
size: S
stage: stage-1-core
blocked_by: [B-24]
---

# B-25 — The tilt camera is measured in inches, and the backends disagree about it

`GraphicsLayerScope.cameraDistance` is in **inches**. skiko converts at a fixed 72 px per inch
(`val depth = cameraDistance * 72f`, under the comment "The camera location is passed in inches, set
in pt"); Android passes the value to `RenderNode` untouched, and hwui converts at the display's real
dpi — Android's own default camera is `1280 × density` px, which is `8 × dpi`, the same 8 inches
`DefaultCameraDistance` names. Full addresses in
[research §1.6](../research/research-architecture.md).

So `TiltIndication(cameraDistance = 8f)` is 576 px of depth on desktop and about 3840 px on a
480 dpi phone: **the same tile tilts roughly six times flatter on Android**. And
`depressionScale()` hard-codes skiko's 72, so the shrink it computes to stand in for sinking is
right on three targets and wrong on the fourth.

- **The fix is to stop speaking skiko's dialect in common code.** Either an `expect` for
  pixels-per-inch, or — better — express the camera in dp, which is a unit the library already
  thinks in, and convert to each platform's `cameraDistance` at the edge.
- The rejected alternative is picking a per-platform constant that makes the two look similar.
  It would work and it would be a magic number in two places that nobody can re-derive; the
  conversion is documented arithmetic, so do the arithmetic.
- **Blocked by [B-24](B-24-add-the-android-target-next.md), and blocked in fact rather than by
  preference:** the acceptance is that the two renderers agree, which cannot be seen with one of
  them missing. The code can be written before that; it cannot be believed.

- AC: a press at the same point on the same tile produces the same rendered geometry on desktop and
  on Android, compared as images rather than as reasoning.
- AC: `depressionScale()` no longer contains the number 72.
- AC: the KDoc on `cameraDistance` says what unit the parameter is in, because the platform API's
  own documentation says "pixels" and is wrong.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt`
