---
id: B-11
title: "Authentic visuals with extended hit areas, and an opt-in contrast palette"
status: done
priority: P1
size: S
stage: stage-1-core
blocked_by: [B-05]
---

# B-11 — Authentic visuals with extended hit areas, and an opt-in contrast palette

**Half done, and the half that is done is the palette.** `KvadrantColors.accessible()` and
`accessibleAccent()` raise every accent to WCAG AA by walking it towards black or white until the
ratio is reached — computed rather than hand-picked, so a caller's own accent gets the same
treatment. Five tests hold it: every accent reaches AA, exactly the nine that needed help moved, the
other eleven are untouched, subtle text reaches AA in both themes, and **the authentic palette is
left exactly as it was** — that last one is what fails if somebody decides to be helpful.
`palette_accents_dark.png` shows both palettes side by side with the ratios on them.

**And now the other half.** `TouchTargetTest` measures every interactive control — button, toggle,
checkbox, radio, slider, text box — through `runComposeUiTest` and asserts each is at least 48 dp
tall. It was **verified by breaking it**: removing the toggle's minimum height turns the test red,
restoring it turns it green. A guard nobody has seen bite is a guard nobody knows is wired.

That closes this item. The visual stays canonical, the hit area is checked, and the contrast palette
is one call away.

Metro breaks three current norms measurably: touch targets at 34 px ≈ 25.5 dp against a 48 dp
expectation; subtle text at ≈2.8:1 in the light theme against WCAG AA's 4.5:1; and the `lime`,
`amber` and `yellow` accents at ≈2.2:1 with either text colour. The policy
([D7](../research/research-architecture.md)) is: canonical visual, hit area always extended to
48 dp with invisible padding, higher-contrast variants opt-in.

- **The split costs nothing** because visual size and touch area are separate properties in Compose,
  and it is the only arrangement that serves both the person who came for authenticity and the
  product that has an accessibility bar. A library that silently fixes Metro is not the library
  anyone asked for; one that ignores the problem is unusable in half its intended market.
- Rejected: a single `strictMetrics` flag controlling all three at once. They are three different
  trade-offs with three different owners, and one switch forces a consumer who wants readable text
  to also give up the visual.
- **The contrast test names the accents that fail**, rather than being a threshold that quietly
  passes. It turned out to be **nine of twenty**, not three, with the phone's own default `cyan`
  among them at 2.90:1 — which is the argument for the opt-in palette being load-bearing rather
  than a courtesy.

- AC: every interactive control reports a ≥48 dp touch area in a semantics test while measuring its
  canonical size in a screenshot test.
- AC: `KvadrantColors.accessible` exists, is opt-in, and is covered by the same screenshot matrix.
  **Done.**
- AC: the contrast test names the failing accents explicitly and fails if another joins them.
  **Done** — and it named nine, not three.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/theme/KvadrantColors.kt`
