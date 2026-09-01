---
id: B-48
title: "The ink on an accent surface is computed and cannot be chosen, so the contrast policy has only one lever"
status: done
priority: P1
size: XS
stage: stage-3-completeness
blocked_by: []
---

# B-48 — The ink on an accent surface is computed and cannot be chosen

`KvadrantColors.onAccent` is `val onAccent: Color get() = contrastOn(accent)` — a computed property
with nothing behind it. Every other colour in that class is a constructor parameter; this one is a
derivation, so a caller can choose the accent and cannot choose what is written on it.

**The derivation is right and that is not what this item disputes.** `contrastOn` flips at a
luminance of 0.5 because that is where Metro flipped, and white on a cyan tile is the authentic
answer. The default stays the default.

## The policy already allows for this and has one lever where it needs two

[B-11](B-11-accessibility-policy.md) settled the shape: canonical visual by default, higher-contrast
variants opt-in, and the reason given there was that a library which silently fixes Metro is not the
library anyone asked for while one that ignores the problem is unusable in half its intended market.
It shipped `accessible()` / `accessibleAccent()`, which reach WCAG AA by **walking the accent towards
black or white** — that is, by changing the colour.

That is the right lever for a caller whose accent is negotiable. It is the wrong one, and the only
one, for a caller whose accent is not: an application working to a brand or a design kit arrives with
a fixed hex and cannot accept a nudged one. For such a caller the accent stays and the ink is what
has to move, and there is no way to say so.

The numbers for the two accents most likely to meet this, against a filled surface:

| Accent | luminance | `contrastOn` returns | the other ink |
|---|---|---|---|
| `Cyan` `#1BA1E2` | 0.312 | white, 2.90:1 | black, 7.24:1 |
| `Amber` `#F0A30A` | 0.447 | white, 2.11:1 | black, 9.95:1 |

Contrast is symmetric, so these ratios are also the accent's contrast against a white page — a
different problem with the same arithmetic, and the one `accessible()` is built for. This item is
about the first reading only.

## What it changes

`onAccent` becomes a constructor parameter defaulting to `contrastOn(accent)`, carried through
`dark()`, `light()` and `copy()` like every other colour. Nothing changes for a caller who does not
pass it.

- Rejected: **changing the default to the higher-contrast ink.** It would decide for every consumer
  against a transcription this library is right to keep, and it is the failure
  [B-11](B-11-accessibility-policy.md) names — the authentic palette is left exactly as it was, and
  "that last one is what fails if somebody decides to be helpful".
- Rejected: **telling such a caller to use `accessible()`.** It answers a different question. The
  accent it returns is not the accent that was asked for.
- Rejected: **leaving it to the call site.** A consumer can pass a colour to every component that
  fills with the accent, and then the theme is in two places and only one of them is the theme.
- The price: `KvadrantColors` is a `data class` and `abiValidation` is on, so this is a
  binary-incompatible change and arrives as a diff somebody approves. That is the mechanism doing its
  job; two public signatures have moved here unnoticed before, which is why it exists.

## Done

`onAccent` is a constructor parameter defaulting to `contrastOn(accent)`, carried through `dark()`,
`light()` and `copy()`. Every golden is byte-identical, and two tests hold the shape: the default is
still the transcription for all twenty accents, and the accessibility walk never flips the ink — the
question `copy()` carrying a stored value raises, answered by measurement rather than by argument,
because `accessibleAccent` moves the accent *away* from the text on it and so cannot cross the
threshold. The ABI dump moved, which is the mechanism doing what this item said it would.

## Acceptance

- AC: `KvadrantColors.dark(accent = …, onAccent = Color.Black)` compiles and renders, and the same
  through `light()` and `copy()`.
- AC: every existing golden is byte-identical, because the default is unchanged.
- AC: a test asserts the default is still `contrastOn(accent)` for all twenty accents — the thing a
  helpful future edit would break.
- AC: the ABI dump is updated in the same commit as the signature.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantColors.kt`,
  `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantTheme.kt`
