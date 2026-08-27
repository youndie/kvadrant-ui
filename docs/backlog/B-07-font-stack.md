---
id: B-07
title: "Bundle the font stack through compose-resources"
status: wip
priority: P0
size: M
stage: stage-1-core
blocked_by: [B-03]
---

# B-07 — Bundle the font stack through compose-resources

**Half done — the half Android needed.** The six faces moved from `desktopMain/resources` into
`kvadrant-core/src/commonMain/composeResources/font/`, `kvadrantLatin()` and `kvadrantCyrillic()`
are common `@Composable` functions over `org.jetbrains.compose.resources.Font`, and the resource
package is pinned to `io.github.youndie.kvadrant.resources` rather than the `kvadrant_ui.kvadrant_core`
the generator derives from directory names. Fifteen of the sixteen font goldens came back
byte-identical, which is the evidence that the new path renders what the old one did.

**Still open:** there is no `kvadrant-resources` module — the fonts live in `kvadrant-core`, and
whether they should move out is a packaging question nobody has been forced to answer yet. The POM
does not declare the font licence separately from the code licence. iOS and wasm are unverified
because those targets do not exist. And four of the five Metro weights are still uncalibrated: only
SemiLight has its 370.

Five Selawik weights plus **Source Sans 3** ([B-03](B-03-spike-cyrillic-font.md)), bundled in
`kvadrant-resources` and reachable from every target through
`org.jetbrains.compose.resources.Font(Res.font.selawk, weight, style)`.

- **The two are joined by script segmentation, not by `FontFamily` order.** A `FontFamily` list
  selects weight and style variants; it does not fall back on a missing glyph, which was measured
  rather than assumed ([research §1.7](../research/research-architecture.md)). The Cyrillic run is
  rendered at the **same size** as the Latin — the declared x-height ratio suggested scaling and
  the render refuted it — and instanced on Source Sans 3's `wght` axis. **Only the SemiLight slot is
  calibrated so far** — 370, by ink parity. The other four Metro weights need the same treatment,
  and **calibrate against the render, not against OS/2**: both numbers here were wrong when derived
  from declared metrics. The spike's `mixed()` in
  `kvadrant-core/src/desktopTest/kotlin/.../FontStackScreenshots.kt` is the prototype; this item
  makes it public API and moves the fonts out of the test source set.

- **`Font(...)` from compose-resources is `@Composable`**, which propagates: `TextStyle` and
  `KvadrantTypography` have to be composable too, or the font never arrives. This is the detail that
  decides whether `KvadrantTypography.default()` is a plain function or a composable, and it has to
  be decided here rather than discovered while wiring the theme.
- Metro-Compose already bundles Selawik in a shipped Play Store application under MIT code and an
  OFL font, so the licensing pattern has a precedent rather than being an opinion.
- **No Segoe file enters this repository**, including test assets
  ([research §1.7](../research/research-architecture.md)).
- Not covered: the type ramp itself, which is part of [B-05](B-05-theme-model-and-tokens.md).

- AC: Cyrillic and Latin text render with the intended families on Android, desktop, iOS and wasmJs.
- AC: `kvadrant-resources` ships `META-INF/licenses/OFL.txt` and declares the font licence in its POM
  separately from the Apache-2.0 code licence.
- AC: a screenshot test renders the whole ramp in both scripts, so a missing glyph shows up as a
  diff rather than as a bug report.
- Anchors (to be created): `kvadrant-resources/src/commonMain/composeResources/font/`
