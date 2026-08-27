---
id: B-19
title: "Reverse adapter and AdaptiveWidget"
status: open
priority: P1
size: M
stage: stage-3-completeness
blocked_by: [B-14]
---

# B-19 — Reverse adapter and AdaptiveWidget

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
