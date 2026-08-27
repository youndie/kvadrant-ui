---
id: B-25
title: "The tilt camera is measured in pixels while the geometry is in dp"
status: done
priority: P0
size: S
stage: stage-1-core
blocked_by: []
---

# B-25 — The tilt camera is measured in pixels while the geometry is in dp

> **The title changed because the item's premise was wrong.** It was written as "the backends
> disagree", from reading. They do not: measured with the same geometry, the desktop solves to a
> 588 px camera and a Pixel 6a to 593 px — a tenth of a percent apart. Full working in
> [research §1.6](../research/research-architecture.md). The prescribed fix survived the refutation
> unchanged, which is luck rather than vindication.

**Mostly done.** `TiltIndication.cameraDistance` is a `Dp`, `depressionScale` no longer contains the
number 72, and `CameraProbeTest` solves the camera out of a rendered trapezoid so the unit cannot
quietly stop reaching the layer. Every desktop golden is untouched, which is the point: at density 1
the new default is the old camera exactly.

**Confirmed on the device**, both halves, with the same 497 px tile the defect was measured on:

| | before | after | desktop |
|---|---|---|---|
| centre press, drawn / nominal | 0.9225 | **0.9678** | 0.9685 |
| bottom-edge press, top / bottom | 1.1292 at 497 px | **1.0461** | 1.0487 predicted for the same 189 dp |

The second row is the one that matters and it is easy to misread: the two "before" and "after"
numbers are not comparable to each other, because before the fix a 497 px tile faced a 576 px camera
and after it faces a 1512 px one. What is comparable is the right-hand pair — the phone's 497 px
tile *is* 189 dp, and 189 dp on the desktop draws a trapezoid of 1.0487 against the phone's measured
1.0461. **The same design in dp now projects the same on both**, which is what the item was for.

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
- **Unblocked, and half of it is now measured rather than reasoned.** B-24 landed and the demo runs
  on a Pixel 6a at 420 dpi. The same tile, pressed dead centre — pure depression, no rotation —
  draws **413 px at rest and 381 px pressed, a ratio of 0.9225**, against the desktop's 0.9685.
  `depth / (depth + depression)` with the hard-coded 576 px depth and a depression of
  `18.75 dp × 2.625 = 49.2 px` predicts **0.9213**. The measurement lands within a pixel of edge
  antialiasing of the arithmetic, which means the mechanism is not in doubt: the depression is
  converted to pixels and the depth is not, so **the deeper the screen's density, the deeper a press
  sinks** — a phone-shaped bug that no desktop test can see.
  The rotation half of this item is still unmeasured: what the six-times figure predicts is a
  flatter *perspective*, and a centre press has none.

- AC met: a press at the same point on the same tile produces the same rendered geometry on both,
  compared as images — 0.9678 against 0.9685, and 1.0461 against 1.0487.
- **AC not met, and left open deliberately:** the measurement is from one device. One device confirms
  an arithmetic prediction; two would confirm that the arithmetic is the whole story. The desktop
  half is guarded by `CameraProbeTest`; the Android half is a screenshot somebody took by hand once,
  which is [B-16](B-16-screenshot-tests.md) and is why B-16 is the next thing worth doing.
- AC: `depressionScale()` no longer contains the number 72.
- AC: the KDoc on `cameraDistance` says what unit the parameter is in, because the platform API's
  own documentation says "pixels" and is wrong.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt`
