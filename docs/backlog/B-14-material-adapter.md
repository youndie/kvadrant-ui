---
id: B-14
title: "KvadrantMaterialAdapter and the ripple shim"
status: wip
priority: P0
size: M
stage: stage-2-release
blocked_by: [B-04]
---

# B-14 — KvadrantMaterialAdapter and the ripple shim

**In progress.** `kvadrant-material-adapter` exists on option (b) — the graph question is settled in
[research §1.2](../research/research-architecture.md) and the answer inverted the brief. The adapter
raises a `MaterialTheme` from the surrounding `KvadrantTheme`, and four things came out of building
it that reading could not have given:

- **`Shapes` has eight slots in this version**, not the six this item says: `largeIncreased`,
  `extraLargeIncreased` and `extraExtraLarge` beyond the familiar five. Its five-argument constructor
  still exists and still compiles, leaving three slots rounded, silently.
- **`RectangleShape` cannot go in a `Shapes`** — it takes `CornerBasedShape`, so "no corner" is
  spelled `RoundedCornerShape(0.dp)`.
- **A Material `Button` cannot be squared by theming at all.** `ButtonDefaults.shape` is
  `RoundedCornerShape(50%)` from a token and never reads `MaterialTheme.shapes`; `CardDefaults.shape`
  in the same composition *is* the theme's zero corner. So the adapter works and the button is
  outside its reach — the "shapes forced round" cause of research §1.3, demonstrated on the most
  ordinary component there is, and pinned by a test so that a Material version which starts
  honouring the theme is noticed rather than discovered.
- The `DatePicker` was then measured rather than assumed, and it **does** follow the theme:
  `DatePickerDefaults.shape` under the adapter is a zero corner, the component draws, and the
  selection wears the accent. So the split is real and it is per-component.

**The side-by-side is the deliverable, not the code.** `adapter_pairs_dark/light` puts five Kvadrant
controls above their Material counterparts under the adapter, and it settles the argument by being
looked at:

| Material component | under the adapter |
|---|---|
| `OutlinedTextField` | square, themed — sits beside the Kvadrant text box without jarring |
| `Card` | square, chrome background — fine |
| `Button` | **a pill.** `ButtonDefaults.shape` is a token |
| `Switch` | **rounded track, circular thumb** — nothing like the Metro toggle |
| `Slider` | **rounded track with a gap and a round thumb** |

Two of five survive on theming and three need replacing, which is research §1.3's count stopping
being a number in a table. What the adapter is *for* is the first two rows.

A separate artefact that reads the current `KvadrantTheme` and raises a `MaterialTheme` derived from
it, so a consumer's existing Material widgets stop looking foreign: a `ColorScheme` with
`surfaceTint = Color.Transparent` (this is what kills tonal surfaces), a `Typography` from the WP
ramp, all six `Shapes` slots `RectangleShape`, `LocalRippleConfiguration provides null`, and
`LocalIndication provides TiltIndication`.

- **It is worth having for foreign widgets, not for building a Metro kit.** Of ~22 Material 3
  components, ~4 survive on theming, ~10 need a wrapper and ~11 need replacing outright
  ([research §1.3](../research/research-architecture.md)). The adapter earns its place on date
  pickers and autocompletes a consumer already depends on.
- **The ripple switch is the fragile part of the whole design.** It is a Material API, and the
  Jetpack M3 1.5 alphas are reshaping exactly that corner — which is why this lives in its own
  artefact with its own release cadence rather than in the core.
- Rejected: having `KvadrantTheme` raise `MaterialTheme` itself. Better ergonomics, and it drags the
  entire Material version problem into the core; the ergonomics come back through a one-call
  `KvadrantMaterialTheme { }` shortcut that lives here.
- Which Material line this compiles against is settled in [B-04](B-04-repository-skeleton.md), not
  here, and **not** with the brief's `strictly("[1.12.0, 1.13.0)")` snippet, which resolves into the
  alpha line it was written to keep out ([research §1.2](../research/research-architecture.md)).

- AC met: a Material `DatePicker` inside `KvadrantMaterialAdapter { }` renders flat, rectangular and
  in the current accent — `DatePickerTest`, measured off the pixels and off
  `DatePickerDefaults.shape` rather than inferred from the theme.
- AC met: `adapter_pairs_dark`/`adapter_pairs_light` put five Kvadrant controls beside their Material
  counterparts, inside `check`, with `SuiteNotVacuousTest` so the set cannot silently empty.
- AC met: `noMaterialInTheCore` resolves `kvadrant-core`'s consumer-facing runtime classpath and
  fails on any `material3` component — **checked**, and verified by adding one and watching it fail.
  It asserts nothing about Material 2, which `compose.desktop.currentOs` brings into the core's own
  test source set and which sits between us and skiko rather than between a consumer and the core.
- AC: **no ripple** is still unasserted. `LocalRippleConfiguration provides null` is set and
  compiles; that a press draws no ripple has not been measured, and after the button it is exactly
  the kind of claim this item has already been wrong about once.
- AC: wrappers for the three components the picture shows as foreign — button, switch, slider. That
  is the ~10 category and it is the rest of this item.
- Anchors (to be created): `kvadrant-material-adapter/src/commonMain/kotlin/`
