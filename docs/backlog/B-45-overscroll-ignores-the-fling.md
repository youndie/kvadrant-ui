---
id: B-45
title: "The overscroll compresses under a finger and not under a fling, which is how a list usually ends"
status: open
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

## Acceptance

- AC: a list flung into its end compresses, asserted by driving a fling rather than a drag —
  `OverscrollCompressionTest` drags, which is why this shipped.
- AC: the guard is checked by putting the `UserInput` condition back and watching it fail. A test
  that passes before the fix is a test of something else.
- AC: the compression a fling produces varies with the velocity it arrived at, asserted at two
  speeds, because a bound that ignores speed would pass a test written at one.
- AC: the comment above `applyToFling` describes what the code does, and the dead second
  `performRelease()` goes.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/KvadrantOverscroll.kt`,
  `kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/behaviour/OverscrollCompressionTest.kt`
