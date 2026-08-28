---
id: B-35
title: "The Cyrillic companion renders differently on Linux, and it is not a flake"
status: open
priority: P0
size: M
stage: stage-2-release
blocked_by: []
---

# B-35 — The Cyrillic companion renders differently on Linux, and it is not a flake

The first CI runs this repository ever had fail on thirty-four goldens, and the failure is **stable**:
`type/ramp dark` differs by 1673 px of 360 000 and `screen/start dark` by 149 px of 280 000, the same
numbers in every run. Linux renders these frames identically to itself and differently from macOS.

**It is the Cyrillic, and the diff shows it directly.** In `type_ramp_dark_DIFF.png` every Latin word
is clean and every occurrence of `образец` is outlined in red. Latin is Selawik, five static faces;
Cyrillic is Source Sans 3 **instanced on its `wght` axis at run time**, and pinning `FontHinting` and
`FontSmoothing` through viddik's `ViddikPlatformTextStyle` fixed the Latin and left this.

- **The outlines are edges, not shapes.** A completely different font would paint the glyph solid in
  a diff; these are red only along the boundary, which is antialiasing of the same outline. So the
  companion is being *found* on both platforms and rasterised differently — consistent with a
  variable instance rather than a static face.
- **What to try first, and it is a measurement rather than a fix.** Render one Cyrillic word at one
  weight through the variable instance and through a static face of the same design, on both
  platforms, and compare. If the static face is stable across them, the axis is the cause and the
  answer is to ship pre-instanced faces rather than instancing at run time.
- Rejected in advance: widening the tolerance. 0.46 % is nine times the current one, and a tolerance
  that admits it admits nearly every regression these images exist to catch.
- Rejected in advance: per-platform golden directories. Two sets of images means two sets to keep
  true, and the second one is only ever looked at by CI — which is how a suite stops being read.

## Acceptance

- AC: the cause is named with a measurement, not with a plausible mechanism.
- AC: `check` passes on Linux and on macOS against **one** set of goldens, or the item closes with a
  written reason why one set is impossible.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/foundation/KvadrantFonts.kt`
