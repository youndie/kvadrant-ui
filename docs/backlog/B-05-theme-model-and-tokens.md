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
