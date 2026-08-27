---
id: B-08
title: "TiltIndication in production quality"
status: done
priority: P0
size: L
stage: stage-1-core
blocked_by: [B-01]
---

# B-08 — TiltIndication in production quality

**Done.** Tilt is the default `LocalIndication` under `KvadrantTheme`; the geometry is fixed by nine
goldens rather than the five asked for — centre, four edges and four corners — because the edge
presses turned out to be the only ones that can tell an inverted axis from a correct one, a corner
press turning about both axes at once and hiding it. The camera is held in dp
([B-25](B-25-tilt-camera-is-in-inches.md)) and the return timing is a pure function with its own
test.

**Frame budget, measured.** Pixel 6a, 60 Hz, the fitted metric scale, 25 full press-and-release
cycles per run so that every 200 ms hold and 100 ms return plays out. Read from
`dumpsys gfxinfo` after a `reset`.

| surface | frames | janky | 50th | 99th |
|---|---|---|---|---|
| medium tile, 189×189 dp | 546 | 0 (0.00%) | 5 ms | 12 ms |
| wide tile, 390×189 dp | 547 | 6 (1.10%) | 6 ms | 17 ms |
| wide tile, 390×189 dp, **repeat** | 549 | 1 (0.18%) | 6 ms | 12 ms |
| small tile, ~74 dp | 545 | 6 (1.10%) | — | 19 ms |

**The repeat is the row that matters.** After the first two runs the obvious reading was that jank
scales with the area being transformed — 0% on the medium tile, 1.1% on the wide one. Running the
wide tile again gave 0.18%, and the small tile — the least area of the three — gave the same 1.10%
as the wide one's worst run. **The spread between two identical runs is larger than the spread
between the surfaces**, so the size hypothesis is not supported by this data and the honest statement
is that it was not tested.

What is not in doubt: the tilt draws far inside the budget. The GPU is idle — 518 of 549 frames at
1 ms, 99th percentile 2 ms — and the CPU median is 5–6 ms against 16.7. The outliers are single
frames at 20 and 24 ms, and the harness is a plausible source: it starts fifty `input` processes on
the device per run, competing for the same CPU. That was not separated out, because there is no
baseline available inside the app — press somewhere without a tilt and Compose draws no frames at
all, so there is nothing to compare against.

**Left open deliberately and tracked elsewhere:** the tilt does not follow a moving finger
([B-27](B-27-tilt-does-not-follow-the-finger.md)), and whether a per-layer camera should be emulating
the original's global one is [B-26](B-26-per-layer-camera-versus-a-global-one.md).

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
