---
id: B-14
title: "KvadrantMaterialAdapter and the ripple shim"
status: open
priority: P0
size: M
stage: stage-2-release
blocked_by: [B-04]
---

# B-14 — KvadrantMaterialAdapter and the ripple shim

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

- AC: a Material `DatePicker` inside `KvadrantMaterialAdapter { }` renders flat, rectangular, in the
  current accent, with no ripple.
- AC: a screenshot test puts a Kvadrant control and its Material counterpart side by side under the
  adapter.
- AC: the core still declares no Material dependency after this ships — checked, not assumed.
- Anchors (to be created): `kvadrant-material-adapter/src/commonMain/kotlin/`
