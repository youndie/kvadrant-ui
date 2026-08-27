---
id: B-22
title: "Is the Windows 8 profile part of this library or a separate one?"
status: question
priority: P3
size: XL
stage: stage-4-win8
---

# B-22 — Is the Windows 8 profile part of this library or a separate one?

The Win8 type ramp, the 20 px baseline grid, the 120/100 px silhouette, `Hub`, `AppBar`/`CommandBar`,
`SemanticZoom` and the settings flyout — a second design profile that shares a name with the first
and disagrees with it.

- **Windows Phone and Windows 8 conflict on things as basic as the order of buttons in a dialog**
  ([research §1.3](../research/research-architecture.md)). They cannot share one theme without one of
  them being wrong, which is the argument for a separate `kvadrant-win8` module rather than flags
  inside the existing components.
- The working hypothesis, and the reason this is a question rather than a plan: the Win8 **tokens**
  (type ramp, grid, silhouette metrics) go into the core from the start, because they are already
  transcribed and cost nothing to carry; the Win8 **components** do not exist until a desktop
  consumer asks for them.
- `SemanticZoom` in particular is an XL on its own, and its transition duration is one of the eight
  unclosed gaps in the specification.
- **This branch already has a numeric source, acquired by accident.** The WinJS and UWP Pivot
  metrics in [research §1.11](../research/research-architecture.md) were gathered while hunting for
  the phone's numbers and turned out to belong to the desktop lineage — Windows 8.1 HTML and the
  XAML line that descends from Windows 8. They are wrong for the phone and right for here: a 48 px
  header strip, 24 px SemiLight headers, a 2 px accent underline, and the full switch animation
  (67 ms out, 333 ms opacity and 767 ms slide in, on `cubic-bezier(0.1, 0.9, 0.2, 1)`).
- **Continuum belongs here too, and only here.** Looking for the phone's version turned up nothing
  in three artefacts — it was a shell transition, never exposed — while WinJS's is complete: four
  directions, each three independent curves, two of them deliberately overshooting so the item
  passes its resting place and returns. The full table is in
  [research §1.11](../research/research-architecture.md).

- AC: a decision — tokens only, full profile, or separate library — recorded in
  [research §3](../research/research-architecture.md) under open question 1, with what made the call.
