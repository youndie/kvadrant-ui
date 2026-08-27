---
id: B-08
title: "TiltIndication in production quality"
status: wip
priority: P0
size: L
stage: stage-1-core
blocked_by: [B-01]
---

# B-08 — TiltIndication in production quality

**The tilt was inverted on one axis for as long as it existed.** Every golden had recorded it,
every measurement of the geometry had confirmed it against the formula, and the formula was right —
what was wrong was that Compose turns the opposite way from Silverlight on **both** axes. It took
someone pressing a tile in a running application to see that the button popped out instead of
pushing in — and then a second report to catch that the first fix had corrected one axis and left
the other, because the corner goldens used to check it cannot separate the two.
[Research §1.6](../research/research-architecture.md) records both the fix and why the check was
wrong. The guard now is four **single-axis** goldens.

**The return is in, and it was the hole in the signature effect.** The press was already instant;
the plane simply snapped back on release, which is the one thing the original does *not* do. It now
holds where it was for **200 ms** and unwinds linearly over **100 ms** — and the pause is the part
that matters, because it is the difference between a tap that feels acknowledged and one that
twitches.

**The timing lives in a pure function**, `tiltReturn(millisSinceRelease)`, and the node keeps none of
its own. That is not tidiness: a still frame cannot show a return, so without something checkable
the whole behaviour would rest on nobody having looked.

**Verified by mutation, after two attempts that proved nothing.** The first mutation did not apply —
the formatter had reshaped the `when` and the edit missed. The second applied and the tests stayed
green, which turned out to be **correct**: replacing the delay comparison with `< 0` is semantically
equivalent, because `coerceIn` clamps the resulting negative elapsed time back to 1. Only the third,
setting `RETURN_DELAY_MILLIS` to 0, changes behaviour — and it fails three of the four tests. A
mutation that leaves the suite green is not automatically a hole in the suite; it can be a mutation
that changes nothing.

The rest of what this item asked for: the formulas from `TiltEffect.cs` and the instant press, both
already in place.

- **Three details nearly every reimplementation gets wrong**, and getting them right is the whole
  point of this item: the press is **not animated** (properties are set instantly per manipulation
  delta, only the return animates over 100 ms after a 200 ms delay); **scale is not used at all** in
  the original, the "pushed in" feeling comes from perspective; a touch in the exact centre gives 0°
  of rotation and the full 25 px of depression, a touch in a corner gives 17.188° and none
  ([research §1.6](../research/research-architecture.md)).
- Compose has no perspective `translationZ`, so depression is emulated with scale. Whatever number
  [B-01](B-01-spike-tilt-indication.md) measured against the 0.975 prediction is recorded in KDoc as
  either a match with the original geometry or as this project's approximation.
- Not covered: page transitions built on the same projection maths, which are
  [B-15](B-15-motion-easing-and-turnstile.md).

- AC: tilt is the default `LocalIndication` under `KvadrantTheme`, or — if the spike said otherwise —
  `Modifier.kvadrantTilt()` is public and `NoIndication` is the default.
- AC: a screenshot test fixes the geometry at five touch points: centre and four corners.
- AC: no frame drops on a 210×210 dp tile at 60 fps on the device the spike used.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/indication/TiltIndication.kt`
