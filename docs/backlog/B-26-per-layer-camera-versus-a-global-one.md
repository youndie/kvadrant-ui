---
id: B-26
title: "A per-layer camera is not the global one Metro tilted under"
status: open
priority: P1
size: M
stage: stage-1-core
blocked_by: []
---

# B-26 — A per-layer camera is not the global one Metro tilted under

Raised from the demo, twice, by eye: *"как будто сила, насколько элементы вдавливаются, зависит от
их размера"*, and after the measurement said otherwise — *"всё равно как будто на больших карточках
слишком сильно вжимается"*. A perceptual report that survives being told the arithmetic is a
finding, not a misunderstanding.

The arithmetic in [research §1.6, Consequence 4](../research/research-architecture.md) is not in
doubt: the **depression** is one ratio for every size, measured off rendered frames, and
`TiltScaleInvarianceTest` holds it there. What that measurement does not cover is the other half of
the transform.

- `Modifier.graphicsLayer` gives **every element its own camera, at its own centre**, a fixed
  absolute distance away. The original had **one camera over the whole screen** — research §1.6
  already records that its distance was never published.
- Two consequences follow, and neither is visible in a centre press. A large surface subtends more
  of its own camera than a small one, so its rotation gets more perspective — measured at 0.2% of
  drawn area between 100 dp and 160 dp, which is small but is the sign the eye reports. And under a
  global camera an element's tilt depends on **where on the screen it is**, so two identical tiles
  in a grid do not tilt identically; under a per-layer camera they do, exactly.
- The second one is the substantive divergence. It is also the more visible: a grid of tiles is
  where Metro's tilt is seen most, and it is the case the current implementation renders most
  uniformly.

**Do not start by tuning `cameraDistance` until the demo looks right.** That is fitting a constant
to one screen at one window size, and the number would be unre-derivable a week later. Establish
first *which* of the two the eye is reporting — the per-element perspective or the missing
screen-position dependence — by rendering the same tile grid both ways.

- AC: a still of a tile grid, pressed at the same point, under a per-layer camera and under a
  camera shared across the grid, side by side, so the difference can be looked at rather than
  argued about.
- AC: whichever way it goes, the answer is written into research §1.6 as a measurement — including
  "the difference is not visible", which is a legitimate outcome and closes this.
- AC: if a shared camera wins, it is a documented deviation with its own reason, because the
  original's camera distance is unknown and the shared one would be ours.
- Depends on nothing, but overlaps [B-25](B-25-tilt-camera-is-in-inches.md): both touch the same
  parameter, and doing this one first would decide B-25's units for the wrong reason. B-25 fixes a
  defect; this one asks a question.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt`
