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

**The weights are calibrated, and the gap turned out to be a defect rather than a refinement.** The
companion family held a *single* font instanced at 370, so Compose matched it for every requested
weight and the axis never moved: a `SemiBold` heading rendered Latin bold with its Cyrillic at 370
beside it, and Cyrillic could not be made bold at all. It is five entries now, one per Metro weight.

The axis values were measured rather than derived — ink coverage, lit pixels over the area of the
drawn line, Selawik at the Metro weight against Source Sans at each candidate:

| Metro | Selawik | Source Sans `wght` |
|---|---|---|
| Light | W200 | 330 |
| SemiLight | W300 | **370** |
| Normal | W400 | 420 |
| SemiBold | W600 | 640 |
| Bold | W700 | 690 |

**The method's control is that it rediscovers 370**, which B-03 had found by eye — and it does,
exactly, on a ten-step grid. That is why the other four are trusted. They do not sit on a line and no
offset produces them: +130 at Light becomes −10 at Bold, because Source Sans runs relatively heavier
than Selawik at the thin end and lighter at the thick end. A rule fitted to one weight would have
been wrong at the other. `InkParityTest` re-measures each in a window around its value.

Twenty-seven goldens moved with it, most of them body text — Cyrillic at `Normal` had been rendering
at 370 where it should be 420.

**Still open, all of it blocked rather than pending:** there is no `kvadrant-resources` module, and
whether the fonts should move out of `kvadrant-core` is a packaging question that only matters once
something is published — [B-21](B-21-maven-coordinates.md). The POM cannot declare the font licence
separately from the code licence for the same reason: there is no POM. iOS and wasm are unverified
because those targets do not exist (D14), so the criterion naming them cannot be met by working
harder on this item.

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
