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

**Reopened. The first answer was wrong, and the device said so within a day.**

Moving `transformOrigin` to the root's centre is not a shared camera. `graphicsLayer` uses that
property for the projection's centre **and** the rotation's pivot, so it moved the pivot too: an
element away from the middle swings instead of leaning. Measured on a 60 dp bar pressed at the same
point in its own coordinates — **65 px tall at the centre of the screen, 84 px at the top** — and
reported first as a push notification being pressed far too hard.

**The evidence did not survive either, and that is the more useful half.** The comparison fixture
rotated *nine tiles at once*, and the shared version bent them into one sheet that looked
unmistakably like Metro. A press does not rotate nine tiles; it rotates one. The picture answered a
question nobody had asked, and it was convincing enough that neither the arithmetic nor a second
fixture was asked for.

**What the work actually is:** the projection centre and the rotation pivot have to be different
points, which one `graphicsLayer` cannot express. Two nested layers can — an inner one turning the
element about its own centre with the camera distance, an outer one placing the result relative to
the screen's axis. Whether that is worth having is still open, and the next attempt should start by
pressing **one** surface in two places on the screen rather than nine at once.

---

*Everything below is the original argument, unamended, because it is what was believed at the time.*

**Done, and the eye was right both times.** The two stills the item asked for are
`tilt_camera_per_layer` and `tilt_camera_shared`: nine tiles, the same angles, one camera each versus
one camera over the grid. Per element they are nine identically deformed copies of one shape; shared,
the grid bends as a single sheet, the middle barely skewed and the outer tiles leaning progressively.
Displacing the axis by one tile changes 3 505 pixels of a 24 279-pixel tile, so "the difference is
not visible" — the outcome the item allowed for — was not available.

The fix is the axis, not the distance. `TiltNode` sets `transformOrigin` to the root's centre in its
own coordinates, and `KvadrantCamera.Distance` is untouched: **no new number**, so this is canon
rather than a deviation and AC 3 does not apply.

**Nothing existing caught the change, and that is the item's second finding.** Not one test or golden
moved, because every fixture that presses something centres it in the frame — where a shared axis and
an element's own axis are the same point. `SharedCameraTest` is the off-centre fixture that was
missing, and three goldens moved once it existed.

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
