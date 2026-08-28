---
id: B-35
title: "The Cyrillic companion renders differently on Linux, and it is not a flake"
status: done
priority: P0
size: M
stage: stage-2-release
blocked_by: []
---

# B-35 — The Cyrillic companion renders differently on Linux, and it is not a flake

The first CI runs this repository ever had fail on thirty-four goldens, and the failure is **stable**:
`type/ramp dark` differs by 1673 px of 360 000 and `screen/start dark` by 149 px of 280 000, the same
numbers in every run. Linux renders these frames identically to itself and differently from macOS.

**It is the Cyrillic, and the diff shows it directly.** In `type_ramp_dark_DIFF.png` every Latin word
is clean and every occurrence of `образец` is outlined in red. Latin is Selawik, five static faces;
Cyrillic is Source Sans 3 **instanced on its `wght` axis at run time**, and pinning `FontHinting` and
`FontSmoothing` through viddik's `ViddikPlatformTextStyle` fixed the Latin and left this.

- **The outlines are edges, not shapes.** A completely different font would paint the glyph solid in
  a diff; these are red only along the boundary, which is antialiasing of the same outline. So the
  companion is being *found* on both platforms and rasterised differently — consistent with a
  variable instance rather than a static face.
- **What to try first, and it is a measurement rather than a fix.** Render one Cyrillic word at one
  weight through the variable instance and through a static face of the same design, on both
  platforms, and compare. If the static face is stable across them, the axis is the cause and the
  answer is to ship pre-instanced faces rather than instancing at run time.
- Rejected in advance: widening the tolerance. 0.46 % is nine times the current one, and a tolerance
  that admits it admits nearly every regression these images exist to catch.
- Rejected in advance: per-platform golden directories. Two sets of images means two sets to keep
  true, and the second one is only ever looked at by CI — which is how a suite stops being read.

## Measured, and it is wider than this item said

Two CI runs on different commits produce **the same thirty-five mismatches with the same pixel
counts**, so Linux is deterministic and B-31 stays withdrawn. That is the good half.

The bad half is that this item was written after opening **two** of those thirty-five diffs, and it
generalised from them. Two corrections follow.

**It is not one cause, it is at least two.** The three largest failures are `font stack/selawik
only`, `font stack/selawik then inter` and `font stack/selawik then fira`, at 5.36 % each — and
identically, which is itself the clue. Those fixtures exist to show what happens when the Cyrillic
companion is **absent**: the text falls back to whatever the host has, macOS and the runner have
different answers, and the words come out at different widths. Nothing this item proposes can fix
them, because they are measuring the host on purpose. `CLAUDE.md` already carries the rule they
break — *bundled glyphs still rule out fixtures whose point is a missing font* — and it was written
before these fixtures existed.

**The residue is not "the same glyph, differently antialiased".** That was the reading of two
outlined diffs, and `gallery/controls dark` refutes it: the word `готово` appears in the diff drawn **twice at
two widths**, not as an edge. A different advance is a metric difference, and a metric difference is
not something `FontHinting` and `FontSmoothing` were pinned to fix. Latin does stay clean across
every diff opened so far — `settings`, `inbox`, `controls`, `build server`, `start` carry no red —
so the split between the bundled static faces and the instanced companion still looks like the right
place to dig. It is a narrower claim than the one this item opened with.

**So the first measurement stands but its answer will not cover everything.** Even if pre-instanced
faces make the companion stable, the three `font stack/selawik *` fixtures remain, and they need a
decision rather than a fix: either they stop asserting a picture and start asserting a number, or
the set accepts that a fixture about a missing font has no portable golden.

## Closed, and the fix is not the one this item proposed

**Measured.** A probe rendered one Cyrillic word through five families on both platforms and printed
the ink count and the bounding box of what was drawn. Every box is identical to the pixel:

| case | macOS ink | Linux ink | box |
|---|---|---|---|
| `latin-selawik` | 2150 | 2149 | identical |
| `cyrillic-source-sans-370` | 3019 | 3003 | identical |
| `cyrillic-source-sans-default` | 2238 | 2254 | identical |
| `cyrillic-fira` | 2762 | 2748 | identical |
| `cyrillic-inter` | 3253 | 3251 | identical |
| `cyrillic-fallback` | 3020 | 2555 | **differs** |

That settles three things at once. **Shaping is portable** — the glyphs land in the same places, so
the "drawn twice at two widths" reading of a diff image was wrong; a word whose every edge is
highlighted looks doubled. **The variable axis is not the cause** — the default instance, with no
axis setting at all, differs by the same amount as the instanced one. And the only case whose box
moves is the one with no bundled Cyrillic, where the *host* chooses the font.

**No rasterisation setting closes it.** Aliased rendering differs by fourteen pixels where
antialiased differs by sixteen; full hinting is worse and moves the box. What is left is that the
two rasterisers assign different *values* to the pixels at a glyph's edge.

**No tolerance closes it either, and this is the part worth keeping.** viddik has a per-channel
threshold, which is the right shape for an edge difference, so it was swept over the real goldens:

| channel tolerance | goldens still differing | worst |
|---|---|---|
| 0 | 42 | 5.41 % |
| 2 (viddik's default) | 34 | 5.36 % |
| 4 | 21 | 5.31 % |
| 8 | 16 | 5.22 % |
| 32 | 10 | 4.90 % |

Buying the remainder needs the *percentage* limit at about 1.4 %. Measured against a real
regression — the button's text from SemiBold to Normal — that moves 0.27 % of the pixels, and a 1 dp
change in the tile gap moves 4.9 %. **A tolerance that admits FreeType admits the defect**, which is
the whole argument against loosening it.

**The first sweep said 4 was clean and that was not a result.** It grepped for mismatch lines behind
an `|| true`, so "the comparison passed", "the task was up to date" and "the build failed for
another reason" all printed the same nothing. A four-level threshold reconciling two typefaces
should have been impossible on its face, and checking that is what caught it. The step now reports
Gradle's own status beside the count and reruns the task.

**So the goldens are verified on the operating system that recorded them.** A golden is a picture of
a rasteriser; the Linux runner is not the one that took these. `check.yaml` gains a `screenshots`
job on `macos-latest` and the Linux job excludes `viddikVerify` — a second runner, in exchange for a
gate at full strength rather than a permanently red one.

**Three fixtures could never have been portable and are now a test.** `selawik only`, `selawik then
inter` and `selawik then fira` had one MD5 between them, which *is* the finding: a `FontFamily` list
is not a glyph-fallback chain, so all three drew the host's substitution.
`FontFallbackTest` asserts the same claim as a comparison within one run — three families identical,
a fourth different — and adds the one that was missing: `kvadrantCyrillic()` must render something
other than a family with no Cyrillic, or the companion silently failed to load and the host is
drawing the text.

## Acceptance

- AC: the cause is named with a measurement, not with a plausible mechanism.
- AC: `check` passes on Linux and on macOS against **one** set of goldens, or the item closes with a
  written reason why one set is impossible.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/foundation/KvadrantFonts.kt`
