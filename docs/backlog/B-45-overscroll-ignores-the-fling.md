---
id: B-45
title: "The overscroll compresses under a finger and not under a fling, which is how a list usually ends"
status: done
priority: P1
size: S
stage: stage-2-release
blocked_by: []
---

# B-45 — The overscroll compresses under a finger and not under a fling

Reported from the demo on a device: the compression [B-38](B-38-the-theme-leaves-the-platform-overscroll.md)
added works when a list is dragged past its end and does nothing when one is flung into it. **A list
is flung into its end far more often than it is dragged into it**, so the case that works is the
rarer one.

## It is one condition, and the comment above it says the opposite

`KvadrantOverscroll.applyToScroll` absorbs what the scroller could not spend:

```kotlin
if (source == NestedScrollSource.UserInput && viewport > 0f) {
    // ... leftover becomes compression ...
}
```

A fling does not arrive as `UserInput`. The scroll deltas an ongoing fling produces are a
`SideEffect`, so the leftover at the boundary is dropped and the compression stays at zero.

Then `applyToFling` reads:

```kotlin
val consumed = performFling(velocity)
// Everything the scroller could not spend goes into the spring, and then the spring returns.
performRelease()
if (consumed != velocity) performRelease()
```

**The comment asserts the exact thing that is missing.** Nothing goes into the spring; `performRelease`
returns immediately on a compression of `0f`, which is what it always finds here, and the second call
guarded by `consumed != velocity` is therefore dead in every case it was written for. A reader
checking whether the fling was handled finds a sentence saying it was.

## What the fix has to establish, and it is not just deleting the condition

- **Where the velocity goes.** The leftover of a fling is a *velocity*, not a distance: absorbing it
  as though it were a delta would compress by whatever the last frame's leftover happened to be,
  which depends on the frame rate. The phone's compression is bounded and the depth should come from
  the speed the list hit the end at.
- **Why the condition was there at all.** It is not obviously wrong: `SideEffect` also covers the
  spring's own unwinding and a nested scroll's propagation, and absorbing those would feed the
  compression back into itself. Whatever replaces it has to say which side effects are the end of a
  fling and which are the overscroll talking to itself.
- **The number is ours.** Windows Phone published no fling-into-the-end depth, so whatever this
  becomes is named in KDoc as this project's, next to `maxCompression` and `resistance` — research
  §1.10.

## What it turned out to be

**The `UserInput` condition was right and stayed.** The item allowed for deleting it and it would
have been the wrong fix: absorbing a decelerating fling's deltas frame by frame makes the depth a
function of the frame rate. What was missing is that the leftover at the stop is a **velocity**, so
`applyToFling` converts it once, and the condition now carries the reason it is there.

**The conversion is a saturating curve, and the obvious alternative is named rather than dropped.**
Multiplying the velocity by a time to reuse the drag's distance arithmetic has no defensible time
constant: short enough to keep a hard fling inside `maxCompression` and every ordinary fling produces
almost nothing; long enough for an ordinary fling to show and everything above it clamps to the same
depth, so the effect stops answering how hard the list was thrown. Both versions pass a test written
at one speed. `maxCompression · (1 − e^(−v ⁄ reference))` is bounded by construction and keeps
varying across the whole range a thumb produces; speed is in viewports per second, which is the
normalisation the drag path already uses and keeps this density-independent without the effect having
to know a density.

## Acceptance

- ~~AC: a list flung into its end compresses, asserted by driving a fling rather than a drag.~~ Done
  — `OverscrollFlingTest`. It throws the list from the **top** rather than starting at the stop:
  starting there means the drag compresses before the fling begins, and the frame at the peak cannot
  say which of the two did it.
- ~~AC: the guard is checked by putting the defect back.~~ Done — with `absorb` removed, both tests
  fail; with the depth replaced by a constant, only the second does. A guard that cannot tell those
  two apart is one test doing one job and claiming two.
- ~~AC: the compression varies with the velocity.~~ Done, at 900 and 4 000 px/s — both past the
  reference and still on the climbing part of the curve. Two speeds chosen further out would both
  saturate and the assertion would hold for a constant.
- ~~AC: the comment describes what the code does, and the dead second `performRelease()` goes.~~
  Done. The old comment is quoted in the KDoc where the fix is, because *what it said* is the
  transferable part: it asserted the missing behaviour, so a reader checking whether the fling was
  handled found a sentence saying it was.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/KvadrantOverscroll.kt`,
  `kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/behaviour/OverscrollFlingTest.kt`
