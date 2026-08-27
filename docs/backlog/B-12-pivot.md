---
id: B-12
title: "Pivot"
status: done
priority: P0
size: L
stage: stage-2-release
blocked_by: [B-02]
---

# B-12 — Pivot

**A first version exists.** Header strip, page title, oversized 72 px SemiLight headers, the peek,
the parallax and instant selection — `kvadrant-core/src/commonMain/kotlin/.../KvadrantPivot.kt`,
rendered in `pivot_*.png`. Every number is Microsoft's ([research §1.11](../research/research-architecture.md)).

**The parallax is geometry, not a coefficient.** The header strip shifts so the selected header sits
at the page margin, and it follows the pager continuously — so during a swipe the headers travel by
*their own* widths while the pages travel by a full page width. The difference in rate is the
effect, and nothing is tuned to produce it. That also settles the peek: it falls out of the layout,
which is what the `Canvas` in the original does.

**Two defects found by swiping, which no still could have shown.**

The headers moved in visible steps. The cause is arithmetic rather than animation: a cyclic pivot
runs its pager on a virtual page count starting near **2¹⁹**, and `currentPage + offsetFraction`
was computed as a single `Float`. The spacing between representable floats at that magnitude is
**0.0625**, so the fraction quantised to sixteenths. The page is now an `Int` and the fraction its
own `Float`, and they never meet. `PivotPrecisionTest` holds the reason: it interpolates the same
four swipe positions both ways and asserts they collapse one way and stay distinct the other.

Past the last title the strip slid into blank space. A strip that fits its width **does not loop**,
and it therefore must not travel either — it stands still while only the selected colour moves.
Shifting one that has no copies beside it is precisely how you scroll into nothing.

**Headers are tappable**, and go to the nearest page carrying that title — so tapping the one
peeking at the right edge goes forwards rather than most of the way round the cycle. A strip of
large words that looks tappable and is not is worse than one that does not look tappable at all.

**Pivot pages scroll, and every page shows its own content.** Neither was true, and this item said
both were — the edit that was supposed to add them **never landed**, and the claim was written down
anyway. Two consequences went unnoticed for several rounds: a page taller than the screen was cut
off by whatever sat below it, and `content` was handed the pager's **virtual** page index instead of
0..n-1, so every `when (page)` in a caller fell through to its else branch and all three pages of the
sample rendered the same thing.

`PivotPageTest` now holds both, and both were checked by breaking them. The scroll assertion took
three attempts to become real: `performScrollTo` finds the pager as a scrollable ancestor and
quietly succeeds whether or not anything scrolls, so the test passed with the scroll removed. It
measures the node's position before and after instead, on content tall enough that it cannot fit by
accident.

**The strip does not loop when it fits.** MSDN says headers are drawn until they exceed the width of
the control and, if there are too few to fill it, *do not loop*. That rule was recorded in the
research and not implemented until the sample application made the difference visible.

**Wrap-around is in.** The pager runs on a large virtual page count started in the middle with the
index taken modulo the real one, and the header strip carries three copies of the titles so there is
always one to either side — `pivot_wrap_around.png` catches the seam mid-swipe, and there isn't one.
A pager that clamps while the headers pretend otherwise is the version that feels wrong without
anyone being able to say why.

Horizontal pages with cyclic headers, at most four pages, and the next title peeking in from the
right edge. This is the component the library exists for, and no open implementation of it exists in
Compose, in Android, or in `compose-fluent-ui`, where it stands unticked in the navigation checklist
([research §1.1](../research/research-architecture.md)).

- **`TabRow` + `HorizontalPager` is not an approximation of Pivot, it is a different component.**
  Pivot's titles are set in a large type size, the inactive ones use the `inactive` token, and they
  move at a different rate from the content underneath — the header parallax is the effect, not a
  decoration on top of it.
- Whatever [B-02](B-02-spike-pivot-metrics.md) returns decides whether this ships as a reproduction
  or as an interpretation. Either way every recovered or invented number is a parameter of the
  public API, and KDoc says which kind it is.
- Semantics — not metrics — can be read out of the WinJS `Pivot` sources, which are Microsoft's own
  implementation of the same control under Apache-2.0.
- Not covered: Panorama, which shares the family and none of the mechanics
  ([B-17](B-17-panorama.md)).

- AC: four pages, cyclic swipe, header parallax, and the peek of the next title.
- AC: every number that came from this project rather than from Microsoft is exposed as a parameter
  and marked in KDoc.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/components/KvadrantPivot.kt`
