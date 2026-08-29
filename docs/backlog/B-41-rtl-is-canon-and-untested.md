---
id: B-41
title: "Right-to-left is canon, three layouts would break, nothing tests it"
status: open
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

## Acceptance

- AC: a golden of the pivot, the panorama and the page header under `LayoutDirection.Rtl`, so the
  three custom layouts are looked at rather than reasoned about.
- AC: the header strip and the panorama travel the correct way, and the page header's asymmetric
  margins — `12,17,0,28` — mirror.
- AC: whichever of the three turns out already correct is said so in its KDoc, because the next
  person will otherwise re-check all three.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantPivot.kt`
