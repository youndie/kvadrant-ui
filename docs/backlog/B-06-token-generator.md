---
id: B-06
title: "Generate the token constants from metro-tokens.json instead of typing them"
status: done
priority: P0
size: S/M
stage: stage-1-core
blocked_by: [B-04]
---

# B-06 — Generate the token constants from metro-tokens.json instead of typing them

**Done.** `./gradlew generateKvadrantTokens` writes `KvadrantTokens.kt` from the vendored dump;
`check` runs the same script in `--check` mode, so a dump edited without regenerating is a red build
rather than a silent divergence. The generated file is committed — a consumer reading the sources
should see the constants and a reviewer should see the diff. Scope is the twelve colour tokens per
theme, the twenty accents and the eight type sizes: what something reads, and nothing else, because
a token emitted before anything uses it is the same failure with more lines.

**Two things it found on the first run, which is the whole argument for the item.**

The dump's `light.border` is `#BFFFFFFF` — the dark value repeated — marked `unverified` with a note
in Russian to re-check it. Generating it faithfully broke `KvadrantColorsTest`, which had been
written from the SDK's `ThemeResources` and knew better. The dump is vendored evidence and evidence
does not get amended, so the correction lives in the generator's `CORRECTIONS` table **together with
the value it was written against**: if the dump is ever fixed, the generator stops rather than
letting a stale override sit on top of a corrected source.

And `18.667 × 0.75` is `14.00025`, not `14`. The dump stores decimal approximations of exact thirds,
and carrying the error through put every glyph a fraction of a pixel off its grid — visible in two
palette goldens, which is how it was found. The generator rounds, and refuses to round anything
further than a hundredth of an sp, so a genuinely fractional size would stop the build instead of
being flattened.

With both handled, the whole suite of sixty-seven goldens is unchanged: the generator reproduces
what was typed, which is the only evidence that it is faithful.

The specification is several hundred numbers: 13 colour tokens × 2 themes, 20 accents with computed
luminance and contrast, two type ramps, page and tile metrics, motion curves and durations, tilt
formulas. All of it already exists as a machine-readable dump
([research §1.4](../research/research-architecture.md)).

- **Typing them by hand is several hundred chances to be wrong in a way no test catches**, because
  the test would be typed from the same source by the same person on the same afternoon. A generator
  makes the JSON the single source and the test a comparison against it rather than a restatement
  of it.
- The confidence marks travel with the values: a number Microsoft published (✅), one derived
  arithmetically (🟡) and one this project invented (❌) must be distinguishable in the KDoc a
  consumer reads, not only in the research document.
- Rejected: reading the JSON at runtime. It is build-time data; shipping a parser and a resource to
  a consumer to learn a constant is worse in every dimension.
- Vendor the JSON into this repository — the build must not reach outside it, and the brief's
  current location is not durable ([B-20](B-20-durable-home-for-the-brief.md)).

- AC: `./gradlew generateKvadrantTokens` produces the Kotlin sources, and a check fails if the
  committed output differs from what the JSON produces.
- AC: every generated constant carries its confidence mark in KDoc.
- Anchors (to be created): `build-logic/`, `kvadrant-core/src/commonMain/kotlin/theme/`
