---
id: B-06
title: "Generate the token constants from metro-tokens.json instead of typing them"
status: open
priority: P0
size: S/M
stage: stage-1-core
blocked_by: [B-04]
---

# B-06 — Generate the token constants from metro-tokens.json instead of typing them

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
