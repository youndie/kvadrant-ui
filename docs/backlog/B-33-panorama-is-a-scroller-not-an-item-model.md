---
id: B-33
title: "The panorama free-scrolls where the phone snapped to a section"
status: open
priority: P1
size: M
stage: stage-2-controls
---

# B-33 — The panorama free-scrolls where the phone snapped to a section

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

- Releasing a drag settles with a section header at the left margin.
- A scenario naming which section is selected after a flick of a given size.
- The title's KDoc deviation is removed because the behaviour it stands in for exists.

## Notes

Unknowns that stay unknown until somebody measures a device: how much of the next section peeks, and
the settle animation's duration and curve. Spec §2.3 already lists the peek as absent from every
public source. Whatever is chosen for them is this project's own and ships as parameters.
