---
id: B-04
title: "Skeleton: modules, targets, version catalog, CI, snapshot publishing"
status: wip
priority: P0
size: M
stage: stage-1-core
---

# B-04 — Skeleton: modules, targets, version catalog, CI, snapshot publishing

Seven Gradle modules with strictly one-way dependencies — `kvadrant-resources ← kvadrant-core ←
kvadrant-material-adapter ← sample-gallery`, and `kvadrant-icons ← kvadrant-core` — convention
plugins in `build-logic`, targets `androidTarget` / `jvm("desktop")` / three iOS / `wasmJs`, a
version catalog as the only place a coordinate is written, ABI validation, and snapshot publishing.

- **This item also settles the one open question no spike can answer.** [Research
  §1.2](../research/research-architecture.md) established that there is no stable Material 3 in the
  Compose Multiplatform 1.12 line: the newest stable `org.jetbrains.compose.material3:material3` is
  1.9.0 on Jetpack M3 1.4.0, while CMP 1.12.0 ships `1.12.0-alpha03` on Jetpack M3 1.5.0-alpha22.
  Resolve both graphs and see which compiles — option (a) CMP 1.9.x + material3 1.9.0, or option (b)
  CMP 1.12.0 + the alpha.
- **Do not copy the brief's pinning snippet.** `strictly("[1.12.0, 1.13.0)")` on `compose.material3`
  resolves into the alpha line it was written to keep out.
- The rejected alternative is picking on paper from the POMs. `material3:1.9.0` pulls Compose
  runtime artefacts from the 1.9 line, so whether it co-resolves with a CMP 1.12 core is a fact
  about Gradle, not about POM text.
- **Resolving and compiling is not the check.** A mixed Compose graph of exactly this shape has
  been observed to resolve cleanly, compile cleanly and then throw `AbstractMethodError` at render
  time on the first text field ([research §1.2](../research/research-architecture.md)). The
  candidate graph is accepted when something is drawn, not when the build is green.
- Not covered: the adapter's own code, which is [B-14](B-14-material-adapter.md).

**Done so far:** Gradle 9.7.1, a Java 25 toolchain, Kotlin 2.4.10, ktlint (plugin 14.2.0 running
CLI 1.8.0) wired into `check` for every subproject, the shared `.editorconfig`, viddik screenshots
also wired into `check`, and one module — `kvadrant-core` — with the **desktop target only**.
`./gradlew check` is green and the screenshot gate has been shown to fail on a changed golden.
A `sample` module was added with it: a real Compose Desktop application, `./gradlew :sample:run`,
opening at **360×600 dp** — the phone's 480×800 canvas at the canonical 0.75. The size is not
incidental: the Start grid is four fixed columns, so a wider window leaves dead space at the right
rather than stretching, which is the grid being faithful.

**Running it found three defects that every screenshot had missed**, and they are worth listing
because of what they have in common — each is about the state a thing opens in, which a fixture sets
by hand and an application does not:

- **nothing painted the background.** `KvadrantTheme` provides colours and paints nothing;
  `KvadrantPage` paints, but an application rooted on a `KvadrantPivot` — a control, not a page —
  showed the window's own white through everything. Every fixture had painted it explicitly;
- **the pivot opened on the wrong page.** The cyclic pager started at `VIRTUAL_PAGES / 2`, and
  `524288 mod 3` is 2 — so it opened on the third title, looking like an application remembering a
  position it was never given. The start is now rounded down to a whole number of cycles;
- **the previous header showed through the page margin.** The strip extends left of the selected
  header, and nothing clipped it. Fixtures had all been rendered at a position where that did not
  show.

**Left:** the Material adapter module and with it the version question below, publishing, ABI
validation, and the remaining targets.

- AC: `./gradlew build` compiles every declared target from a clean checkout.
- AC: a Material `OutlinedTextField` renders under the adapter on the chosen graph — on Android and
  on desktop — before the graph is written into the catalog.
- AC: the version catalog names each Material coordinate with the Jetpack version it maps to in a
  comment, and no coordinate appears as a string anywhere else.
- AC: the answer to the option (a)/(b) question is written into
  [research §D3](../research/research-architecture.md), with what actually failed if one failed.
- AC: `abiValidation` (or binary-compatibility-validator) runs in CI on every pull request.
- Anchors (to be created): `build-logic/`, `gradle/libs.versions.toml`, `.github/workflows/`
