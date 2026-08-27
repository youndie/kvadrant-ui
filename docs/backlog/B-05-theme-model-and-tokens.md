---
id: B-05
title: "KvadrantTheme: colours, typography, metrics, motion"
status: wip
priority: P0
size: M
stage: stage-1-core
blocked_by: [B-04]
---

# B-05 — KvadrantTheme: colours, typography, metrics, motion

**Audited. The theme is built; one acceptance criterion is met differently from how it was written
and one is not met at all.**

`KvadrantColorsTest` does not do what the AC below asks — it never opens `metro-tokens.json` — but
what it does instead is worth more than a literal transcription check for the accents. It pins nine
of the twenty-one by *name* through a luminance computation (`lime, green, teal, cyan, pink, orange,
amber, olive, taupe` are the ones below WCAG AA at their authentic text colour) and pins `yellow` as
the single accent the luminance rule flips to black text. A wrong hex moves a ratio and moves a
name out of that list, so the values are guarded — indirectly, and by a check that also explains
itself. The fear the AC was written against — deriving one palette from the other — is addressed
head-on by `the_light_theme_is_not_an_inversion_of_the_dark_one`.

**What is genuinely unguarded is the semantic slots.** `background`, `chrome`, `inactive`,
`onAccent` and the rest have no value assertion anywhere; only `foreground` and `border` do. A typo
in `chrome` would pass every test in this repository and show up as a screenshot diff *if* a fixture
happens to use it.

Field-by-field against the dump is not reachable while the dump lives outside the repository — that
is [B-06](B-06-token-generator.md) and [B-20](B-20-durable-home-for-the-brief.md), and it is the
reason the AC has stood unmet rather than an oversight.

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
