---
id: B-05
title: "KvadrantTheme: colours, typography, metrics, motion"
status: done
priority: P0
size: M
stage: stage-1-core
blocked_by: [B-04]
---

# B-05 — KvadrantTheme: colours, typography, metrics, motion

**Done.** The field-by-field comparison the criterion asks for now exists — `TokenDumpTest` reads
the vendored `metro-tokens.json` and pins **44 values**: twelve tokens in each theme and all twenty
accents by name. It could not be written before [B-20](B-20-durable-home-for-the-brief.md) put the
dump in the repository, which is why the criterion stood open rather than being forgotten.

**The dump is treated as an oracle only where it says it is.** Each entry carries a `confidence`,
and one is `unverified` — `light.border`, with the dump's own note that it matches the dark value and
needs re-checking. It was re-checked, in the SDK's `ThemeResources`. So the test asserts equality on
`verified` rows and **inequality** on the rest: asserting the whole dump would fail on that row, and
skipping low-confidence rows would let a real transcription error hide behind the flag. Verified
both ways — one accent altered by one bit fails by name, and copying the unverified row back in
fails as "we took the flag instead of the re-check".

The audit below stands as the record of what the check looked like before it existed.

`KvadrantColorsTest` does not do what the AC below asks — it never opens `metro-tokens.json` — but
what it does instead is worth more than a literal transcription check for the accents. It pins nine
of the twenty-one by *name* through a luminance computation (`lime, green, teal, cyan, pink, orange,
amber, olive, taupe` are the ones below WCAG AA at their authentic text colour) and pins `yellow` as
the single accent the luminance rule flips to black text. A wrong hex moves a ratio and moves a
name out of that list, so the values are guarded — indirectly, and by a check that also explains
itself. The fear the AC was written against — deriving one palette from the other — is addressed
head-on by `the_light_theme_is_not_an_inversion_of_the_dark_one`.

**What was genuinely unguarded was the semantic slots** — `background`, `chrome`, `inactive` and the
rest had no value assertion anywhere, and a typo in `chrome` would have passed every test here. That
is what `TokenDumpTest` closes.

**Still not generated, and that is [B-06](B-06-token-generator.md) rather than this.** The constants
are typed by hand and now checked against their source; generating them would remove the typing.
Those are different jobs, and the check is the one that catches drift in either direction.

Four `@Immutable data class`es behind four `internal staticCompositionLocalOf` locals, a public
`KvadrantTheme` accessor object with `@Composable @ReadOnlyComposable` getters, and a scoped
`KvadrantThemeConfiguration` for overriding inside a subtree — the shape
[D9](../research/research-architecture.md) settled, borrowed from `compose-fluent-ui`.

- **Both palettes are transcribed, never derived from each other.** The light theme is not an
  inversion of the dark one: foreground is 87 % black rather than 100 % white, subtle 40 % against
  60 %, disabled 30 % against 40 % ([research §1.5](../research/research-architecture.md)).
  Generating one from the other produces something that reads as "almost Metro", which is the first
  thing a former Lumia owner notices.
- Rejected: `compositionLocalOf { error(...) }`, which is what Metro-Compose does — it turns a
  missing provider into a crash instead of a working default.
- **Settle the suspect number first.** `PhoneBorderColor` comes out identical in both themes in the
  dump, which is more likely an artefact of the dump than a fact. Check it against a second source
  before the token is frozen.
- Not covered: the font family, which is [B-07](B-07-font-stack.md), and the generator that emits
  the constants, which is [B-06](B-06-token-generator.md).

**Done:** the four data classes, the internal static locals, the `KvadrantTheme` accessor and both
palettes transcribed from the SDK's `ThemeResources.xaml`. `PhoneBorderColor` is settled — it does
differ by theme, and the suspicion was a dump artefact. Writing the contrast test turned up a
correction worth more than the code: **nine** accents fail WCAG AA, not three, and `yellow` is the
most legible rather than the least ([research §D7](../research/research-architecture.md)).

**Left:** motion tokens, and the assertion against a vendored copy of the source values.

- AC: `KvadrantTheme { }` compiles on every target and provides working defaults with no arguments.
- AC: a test asserts both palettes against `metro-tokens.json` field by field — not against each
  other.
- AC: the `PhoneBorderColor` question is answered in
  [research §1.5](../research/research-architecture.md), either way. **Done.**
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/theme/KvadrantTheme.kt`
