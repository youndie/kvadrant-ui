---
id: B-03
title: "Decide what Cyrillic text is set in, before any typography code exists"
status: done
priority: P0
size: S
stage: stage-0-spikes
---

# B-03 — Decide what Cyrillic text is set in, before any typography code exists

**Done: Selawik for Latin, Source Sans 3 for Cyrillic, joined per script run — same size,
variable `wght` axis at 370.**
See [D8](../research/research-architecture.md) for the decision and
[research §1.7](../research/research-architecture.md) for the measurements.

Two things came out that the brief did not have:

- **The recommended stack cannot be built.** `FontFamily(Selawik, Inter)` and
  `FontFamily(Selawik, Fira)` render **byte-identically** to `FontFamily(Selawik)` — a `FontFamily`
  list selects weight and style variants, it is not a glyph-fallback chain. The Cyrillic came from
  the host's font manager in all three. The bundled fallback was inert while appearing to work.
- **The nearest static weight is the wrong weight.** Selawik's Semilight sits between Light and
  Regular; Source Sans 3 at 300 renders the Cyrillic a quarter thinner than the Latin by ink
  coverage. Instanced on the `wght` axis at 370 the two runs match (0.989). Compose Desktop honours
  `FontVariation` — verified, three weights gave three renders.
- **The declared x-height lied and the render corrected it.** OS/2 said Source Sans 3 needed
  scaling up by 2.9 %; measured per glyph, that puts the round Cyrillic a pixel over while the flat
  letters already matched. No size compensation is applied.
- **PT Sans is the trap.** It matches Selawik to four decimals on x-height and cap-height, and it
  ships no weight below Regular. Metro's signature is a 72 px SemiLight header, so a companion
  without a Light loses more than a few per cent of x-height ever could.

Evidence is in the repository and re-renderable: eleven goldens under
`kvadrant-core/src/desktopTest/snapshots/font_stack_*.png`, the fonts and their OFL licences under
`kvadrant-core/src/desktopTest/resources/fonts/`.

- **Decide against a picture, not against a table.** The deliverable is the same screen rendered in
  Russian and in English, side by side, in each candidate stack. A rhythm mismatch is the kind of
  thing that is invisible in a comparison of numbers and obvious in a screenshot.
- Candidates and why the others lose: **Selawik → Inter → Noto Sans** (recommended; both OFL, ships
  now, mismatched rhythm); **Inter alone** (one rhythm, but Inter is not Segoe-metric, so every
  number in the type ramp shifts and the specification stops being one); **fork Selawik and draw
  Cyrillic** (correct, and a font project measured in months); **system Segoe on Windows only**
  (three renderings of one UI — kept as a later improvement for Compose Desktop, not as the answer).
- Not covered: bundling mechanics through compose-resources, which is [B-07](B-07-font-stack.md).

- AC: a decision recorded in [D8](../research/research-architecture.md) with the screenshots that
  produced it. **Done.**
- AC: whichever stack wins, the type ramp keeps its numbers from Selawik's metrics, so that a later
  fork does not move the whole scale. **Held** — the ramp is Selawik's; only the Cyrillic run is
  scaled, and by a documented ratio.
