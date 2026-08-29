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

**The licence ships, and the criterion below reads as though it does not.** `Selawik-OFL.txt` and
`SourceSans3-OFL.txt` are in `commonMain/composeResources/files/`, each the full OFL 1.1 with its own
copyright line, and both land in the jar beside the fonts — verified by unzipping
`kvadrant-core-desktop.jar`. What is missing is only the *path* the criterion names,
`META-INF/licenses/OFL.txt`, which is a packaging convention and belongs with the module and the POM
rather than here. A third copy of the same text was written and thrown away instead: the two files
that exist carry the copyright each font's `name` table declares, and one merged file would carry
neither.

**A screenshot of the whole ramp in both scripts exists** — `TypeRampScreenshots.kt`, dark and light —
so that criterion is met.

**The POM declares the font licence separately, which was the other half of that criterion.** Now
that [B-21](B-21-maven-coordinates.md) is answered there is a POM: `kvadrant-core` lists Apache-2.0
for the code and SIL OFL 1.1 for the bundled faces, each with a `comments` saying what it covers, and
the OFL entry names the two files that ship the full text. A consumer's licence tooling reads the POM
rather than the jar, and one licence on an artefact that ships two is the kind of omission nobody
notices until it is somebody's legal problem.

**The resources module is folded in, and that is now a decision.** [B-21](B-21-maven-coordinates.md)
is answered and two artefacts publish; a third carrying six font files, versioned and released in
step with the rest for no benefit a consumer could name, is not worth having. It splits the day
something wants the fonts without the components — the same reasoning [B-18](B-18-icon-set.md)
reached about the icons, on the same evening, which is either consistency or a bias and is written
down so the next reader can decide which.

**What is left is one criterion and it cannot be worked harder at.** iOS and wasm are unverified
because those targets do not exist ([D14](../research/research-architecture.md)). This item stays
open holding exactly that, and closes when a target does.

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

- AC met on desktop and wasm, unmet on Android and iOS, and the three reasons differ. Desktop is the
  golden suite. wasm is the documentation site, where every component page renders Cyrillic through
  the bundled companion — that is a demonstration rather than an assertion, and it is the first
  thing anybody has looked at on that target. **Android is not unverified — it is false**, and the check that says so has now run.
  `AndroidFontStackTest` on a Pixel 6a: `kvadrantLatin()`, `kvadrantCyrillic()` and
  `FontFamily.SansSerif` render the same string to the same pixel, because the AAR contains no fonts
  at all — a manifest, a classes.jar and nothing else. See
  [B-37](B-37-the-android-artefact-ships-without-its-fonts.md), which is where the remaining work
  is. iOS has no target yet (D14).

  This item is finished *for the desktop and wasm artefacts*, which is less than it claimed. The
  fonts are bundled through compose-resources, the licences ship in the jar — verified by unpacking
  it — and the ramp is guarded in both scripts. "One declaration serves every target" was the
  argument for compose-resources over the classpath read it replaced, and on Android it does not.
- AC met, at a different path than this asked for. The POM declares SIL OFL 1.1 beside Apache-2.0
  with a comment saying which covers what, and the artefact carries both texts — verified by
  unpacking `kvadrant-core-desktop-0.1.0-SNAPSHOT.jar`, which holds `Selawik-OFL.txt` and
  `SourceSans3-OFL.txt` next to the six font files. They sit under `composeResources/…/files/`
  rather than `META-INF/licenses/`, because compose-resources decides that path and there is no
  separate `kvadrant-resources` module — the fonts ship inside the core.
- AC met: `type/ramp dark` and `type/ramp light` render every named style in both scripts, so a
  missing glyph is a diff. They are also the two goldens that led B-35 to the difference between two
  rasterisers, which is the same sensitivity working.
- Anchors (to be created): `kvadrant-resources/src/commonMain/composeResources/font/`
