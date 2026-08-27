---
id: B-27
title: "The tilt is fixed at touch-down where the original followed the finger"
status: open
priority: P2
size: S
stage: stage-1-core
blocked_by: []
---

# B-27 — The tilt is fixed at touch-down where the original followed the finger

`TiltEffect.cs` calls `ApplyTiltEffect` from **three** handlers, not one:
`TiltEffect_PointerPressed`, `TiltEffect_PointerMoved`, and the timer path. So while the finger
slides across a tile, the plane keeps leaning towards wherever the finger now is — a press that
starts in the middle and drags to a corner ends up at the corner's full 17.19°.

Ours leans once, at touch-down, and stays there. The reason is structural rather than an oversight:
`TiltIndication` is an `IndicationNodeFactory` reading an `InteractionSource`, and that source
carries `PressInteraction.Press` / `Release` / `Cancel` and **no motion at all** — the press position
arrives once, in the `Press`, and there is no later event to update it from.

- **This is the price of [D-tilt-as-default](../research/research-architecture.md#16-tilt-is-fully-specified-and-three-details-are-usually-got-wrong)**, and the price was worth paying: being an indication is what makes the tilt
  arrive on every clickable surface without a single component signature mentioning it. Trading that
  back for finger-tracking would be a bad deal.
- **So the fix is additive, not a replacement.** A `Modifier.kvadrantTilt()` that owns its own
  `pointerInput` can track motion, and a surface that wants the exact original behaviour opts into
  it while everything else keeps the indication. That modifier was the rejected fallback of
  [B-01](B-01-spike-tilt-indication.md); this is the one case where it earns its keep.
- The rejected alternative is a custom `Interaction` subtype carrying motion, emitted by every
  clickable. It works and it means every component in the library has to remember to emit it, which
  is exactly the "one forgotten call site" failure the indication design was chosen to avoid.
- **How visible is this actually?** Unmeasured. A tap is short and mostly still, so this may be
  invisible outside a deliberate drag — and if it is, the honest outcome is closing this item with
  that written down rather than building the modifier. Measure before building.

- AC: a still comparison — press a tile's centre, drag to its corner, and compare the rendered
  geometry against a press that starts at the corner. They must match, or the difference is named.
- AC: whichever way it goes, the answer lands in research §1.6, including "not worth fixing".
- AC: if the modifier is built, it is canon rather than an improvement — the original tracked the
  finger — so it is **not** gated by [B-28](B-28-remastered-flag.md). Restoring lost behaviour and
  adding new behaviour are different things and the flag must not blur them.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt`
