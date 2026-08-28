---
id: B-19
title: "Reverse adapter and AdaptiveWidget"
status: done
priority: P1
size: M
stage: stage-3-completeness
blocked_by: [B-14]
---

# B-19 — Reverse adapter and AdaptiveWidget

**Done.** `ColorScheme.toKvadrantColors()` takes the host's `primary` as the accent and its
`background`'s luminance as dark-against-light; `KvadrantIsland` raises a theme from it;
`AdaptiveWidget(kvadrant, material)` picks a branch. `reverse_island_in_material` is the frame: a
Material host with a Metro island in it, where the same `AdaptiveWidget` call renders a square button
inside the island and a pill outside.

**The seam is asserted, not described.** `ReverseAdapterTest` requires the island's background to be
`#FF000000` while the host's is a tinted near-black — the island does not meet the host half way, and
that is D6 rather than an oversight. The first version of the fixture painted no background at all,
so it produced a frame that agreed with the KDoc's sentences and not with its claim; the island paints
its own surface now, which is also what a real one would do.

**`AdaptiveWidget` needed something the theme did not have.** Every local in `KvadrantTheme` has a
working default — that is what lets a component render in a test with no theme — so reading any of
them says nothing about whether a theme is above you. `KvadrantTheme.present` is a local that is
false unless a theme provided it, and it is the only honest basis for the branch.

`KvadrantColors.fromMaterial()` for embedding a Kvadrant island in a Material application, and
`AdaptiveWidget(kvadrant = {}, material = {})` on the model `compose-cupertino` proved — the only
working precedent for a second design language beside Material in a KMP project
([research §1.1](../research/research-architecture.md)).

- **The reverse direction is inherently partial, and that is recorded rather than fixed.** Only the
  accent carries across; the background is forced to absolute black or white because that is what
  Metro is, so a Kvadrant island inside a Material screen has a visible seam at its edge. A consumer
  should learn that from KDoc, not from a screenshot review.
- The value is mostly demonstrative — it is what makes a "drop Metro into your existing app" example
  possible — which is why it is P1 and lands after the forward adapter has been used in anger.

- AC: a Material sample screen with a Kvadrant section renders with the host's accent.
- AC: KDoc states the seam and why it is not a bug.
- Anchors (to be created): `kvadrant-material-adapter/src/commonMain/kotlin/`
