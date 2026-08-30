---
id: B-41
title: "Right-to-left is canon, three layouts would break, nothing tests it"
status: done
priority: P2
size: S
stage: stage-3-completeness
blocked_by: []
---

# B-41 — Right-to-left is canon, three layouts would break, nothing tests it

Windows Phone supported right-to-left **out of the box**: `FlowDirection` was set from the phone's
culture and an application did nothing to get it. Arabic and Hebrew are therefore something the
original did and this does not — canon rather than an enhancement, and not a deviation to be argued
about.

## Most of it is already free, which is why this is P2 and S

Measured rather than assumed: the library uses `padding(start`/`end` and `horizontal =` throughout
and **no** `left`, `right` or `absolute` variants anywhere. Compose mirrors those for free the moment
a `LayoutDirection` is provided.

What is not free are three places that compute an x offset by hand inside a custom layout — the pivot
header strip's parallax among them. Those are precisely the components whose identity *is* horizontal
movement, so they are the ones where being wrong is most visible and least excusable.

## One of the three needed a change, and the goldens are how that was found

`rtl/pivot`, `rtl/panorama` and `rtl/page header` render the three custom layouts under
`LayoutDirection.Rtl`. The first came back with the page mirrored around a header strip that had
not: the titles still ran left to right and the **selected** one, which belongs at the margin, was
pushed off the far edge instead.

One word. `Placeable.place` is absolute; `placeRelative` is the one that mirrors. The strip's
arithmetic is unchanged and had to be — it describes a strip with a start and an end, and which side
those are on is the layout's business rather than its own.

The other two were free, and both now say so where a reader will meet it rather than here: the page
header uses `padding(start = …)` and a vertical `offset`, and every layer of the panorama is placed
by `Modifier.offset`, which mirrors its x on its own.

**The direction is mirrored in those goldens and the script is not.** An Arabic golden would be an
image of tofu — this library bundles Selawik and Source Sans 3 and neither has an Arabic or Hebrew
face — so it would be testing the font stack, which is [B-07](B-07-font-stack.md)'s job, rather than
the layout, which is this item's. What is asked here is which way the boxes go.

## Acceptance

- ~~AC: a golden of the pivot, the panorama and the page header under `LayoutDirection.Rtl`.~~ Done
  — `rtl/pivot`, `rtl/panorama`, `rtl/page header`, and the first of them is what found the defect.
- ~~AC: the header strip and the panorama travel the correct way, and the page header's asymmetric
  margins mirror.~~ Done, and **travel needed a test rather than a golden**: a still shows a
  position, and "travels the correct way" is a claim about a direction. `MirroredTravelTest` pans
  each of the two and compares the **sign** of what moved between one direction and the other — 53 px
  left in a left-to-right page against 53 px right in a mirrored one. It deliberately asserts nothing
  about the distance, which is the same arithmetic either way and is held elsewhere; the only thing
  mirroring can get wrong is which way. Verified by putting `place` back: both signs go negative.

  *Its witness took two attempts.* The strip lays its titles out three times, so a search by text
  finds three nodes, and every rule for picking "the interesting copy" can pick a **different** copy
  after the strip has moved — which read as a jump of most of the frame. The average over all three
  needs no such rule, because every copy shifts by the same amount as the strip.
- ~~AC: whichever of the three turns out already correct is said so in its KDoc.~~ Done, in
  `KvadrantPageHeader` and `KvadrantPanorama`, each naming what makes it free.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantPivot.kt`
