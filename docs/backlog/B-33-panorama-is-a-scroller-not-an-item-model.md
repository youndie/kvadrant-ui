---
id: B-33
title: "The panorama free-scrolls where the phone snapped to a section"
status: done
priority: P1
size: M
stage: stage-2-controls
---

# B-33 — The panorama free-scrolls where the phone snapped to a section

**Done. The snap was the first half; the title's exit-and-re-entry is the second, and the reason it
had stalled did not survive being looked at again.**

This paragraph used to say the out-and-in had no published duration and no published curve, so
building it would put two more of this project's figures into a control that already carries the
settle's 300 ms. True about the publication, wrong about the consequence: **reusing what the control
already has costs nothing.** The whole transition takes `SETTLE_MILLIS` — the same as the snap that
provoked it — and its two halves are `ExponentialIn6` going out and `ExponentialOut6` coming back,
the settle's own curve and its mirror. No new number, no new curve. Only the split into halves is a
choice, and it is named as one.

**The reading of the source is the part that needed care, and it is written down as an
interpretation.** "During a selection change between `PanoramaItem` controls" could mean *every*
section change, which would throw the title off the screen several times in one pan and is not what
a panorama looked like. What settles it is the sentence it belongs to: the clause begins
**"instead"**, and what it is instead *of* is repeating "when you pan past the edges of the content".
So the transition belongs to the selection change that crosses the edge, which is the fold. If a
device says otherwise, the KDoc names the paragraph to correct.

**And the change turned up a defect nobody had reported.** Measuring the title inside a bounded
Column returned the width of the screen, so a title wider than the viewport *wrapped onto a second
line* instead of overflowing — in the component whose whole point is a title too big for the screen.
The golden had recorded it as correct for as long as the panorama has existed. `unbounded` is the
same fix the background layer already carries a paragraph about, one layer up.

`KvadrantPanorama` is a horizontal scroller with a wraparound. The original is an **item model**,
and two behaviours follow from that which this does not have.

**It snaps.** From `ff941126`, Panorama control architecture: "A vertical orientation for a
`PanoramaItem` control will **snap only to the left side of the screen** during a gesture movement";
a horizontal orientation "will snap to both the left and the right sides of the screen", and unlike
the vertical one "allows for a user to pan around the center contents without snapping to a new
`PanoramaItem` control view". So the resting state of a panorama is a section aligned to the left
margin with the next one peeking, not wherever momentum happened to stop. Ours stops wherever
momentum stops, which is what a person notices first.

**The title does not wrap; it is animated on selection change.** Same source: `PanningTitleLayer`
"does not repeat itself when you pan past the edges of the content. Instead, during a selection
change between `PanoramaItem` controls, it animates out of view in the direction it was previously
moving and animates back into the scene from the other side of the screen." This library draws the
title twice and treats it as a cylinder — a **named deviation**, in the component's KDoc, and the
right thing only while there is no selected item to hang the real behaviour on. With no second copy
and no item model the title jumps at every fold, which is worse.

The two are one change: a selected index, a snapping fling, and the title's slide-out/slide-in keyed
to the index.

## What is already right, and should not be disturbed

- Wraparound itself, and the layers moving in unison with the drag — "A drag or pan performed on any
  layer will cause all three layers to move in unison".
- The items layer tracking the finger 1:1 during the drag: "whatever content is beneath the finger
  at the beginning of the pan remains until the finger is lifted". Snapping happens **on release**,
  not during.
- The parallax rates as periods rather than coefficients, and `PanoramaFoldTest` which holds them.

## Acceptance

- ~~Releasing a drag settles with a section header at the left margin.~~ Done —
  `PanoramaSnapTest`, which asserts the header's position on screen rather than the scroll offset,
  because the offset is computed from the very boundaries under test. Verified by removing the
  fling behaviour and watching it fail.
- ~~A section wider than the screen can be panned across instead of being thrown past.~~ Done —
  such a section gets a second stop, its right edge against the right of the viewport, per
  `ff941126`: a horizontal `PanoramaItem` "will snap to both the left and the right sides of the
  screen" and "allows for a user to pan around the center contents without snapping to a new
  `PanoramaItem` control view". A hard flick still travels past it, which is the same source's
  distinction between a pan and a throw, and the two tests use the two gestures.
- ~~A scenario naming which section is selected after a flick of a given size.~~ Done —
  `PanoramaFlingTest`. A 100 px drag released without a throw falls back to the section it started
  on and released at 500 px/s carries on to the next, which is the naming half.

  The half with teeth is the second test, and **the first version of it was worthless**. The rule
  here is that a release settles on one of the two stops the finger is between, replacing one that
  took the nearest stop to the predicted end of the decay and skipped sections — and nothing had
  held that in place. A guard written for it passed with the old rule restored, because the panorama
  *wraps*: a prediction overshooting by a whole copy lands on another copy of the same section, and
  the header at the margin is the same either way. Most cells of the drag-by-velocity table agree.

  So both rules were swept and the cells where they differ were measured: at a 250 px drag, a
  1500 px/s throw settles on `two` bracketed and on `three` under the old rule — the skipped section
  — and a 3000 px/s throw settles on `two` bracketed and on `one`, having wrapped past everything.
  Those are the assertions, and putting the old rule back fails them.
- ~~The title's KDoc deviation is removed because the behaviour it stands in for exists.~~ Done.
  `PanoramaTitleTest` holds it, and **absence is the assertion**: the old title was drawn three
  times and offset modulo its own width, so some copy of it was on screen at every moment of every
  pan — a frame with an empty title band cannot be produced by that model at all. One measurement
  therefore carries both halves of the claim, that it goes and that it does not repeat, without
  having to find a seam inside a word.

  The second test is the one that took two attempts. It asserts that the transition does **not**
  fire when the panorama merely opens — positioning the scroll into the middle copy goes through the
  same branch — and the first version measured after the panorama had settled, which let the opening
  animation finish before the camera was switched on. Removing the guard failed the *wrap* test and
  left that one green. The second version measures from the first frame, and the threshold is not
  emptiness: the opening transition's extreme still leaves eighteen pixels of ink where a real wrap
  leaves none, so what separates the cases is the two orders of magnitude to the eight thousand the
  title draws at rest.

## Deviation: orientation is derived, not declared

The original carries an explicit `PanoramaItem.Orientation` — `Vertical` snaps to the left only,
`Horizontal` to both edges and "allows for the content to be placed off the screen instead of being
clipped". This library has no such property and infers it: a section measured wider than the
viewport gets both stops. That covers every section the property was *for*, and misses the case of a
narrow section deliberately marked `Horizontal`, which has nothing off screen to pan to anyway.
Worth revisiting if a caller ever needs the distinction; adding the property later is additive.

## Notes

Unknowns that stay unknown until somebody measures a device: how much of the next section peeks, and
the settle animation's duration and curve. Spec §2.3 already lists the peek as absent from every
public source. Whatever is chosen for them is this project's own and ships as parameters.
