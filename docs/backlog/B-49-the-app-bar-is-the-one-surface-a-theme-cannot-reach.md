---
id: B-49
title: "The app bar's dimensions live in the component, so a theme cannot state them and scaled() leaves the bar behind"
status: open
priority: P1
size: S
stage: stage-3-completeness
blocked_by: []
---

# B-49 — The app bar's dimensions live in the component, not in the metric set

`KvadrantAppBar.kt` ends with four measurements written as file-private constants:

```kotlin
private val HEIGHT: Dp = 54.dp        // 72 px
private val MINI_HEIGHT: Dp = 22.5.dp // 30 px
private val BUTTON: Dp = 36.dp        // 48 px
private val RING: Dp = 1.125.dp       // 1.5 px
```

plus `KvadrantAppBarGlyphSize`, which is public but a top-level constant rather than a token. Every
other Metro distance in this library is a field of [KvadrantMetrics]. These five are the exception,
and nothing in the file says why.

## The consequence is a defect, and it is this library's own model that names it

`scaled()` exists because "Metro's numbers were drawn for a 480 px phone… on a larger window the 9 dp
page margin that felt right there reads as cramped, and adjusting it alone would break its
relationship with the tile gap" — so the scale is one knob over the whole set, **everything moves
together or nothing does**. `KvadrantMetrics`' own KDoc goes further: the phone's canvas was 480
units wide whatever the screen was, and the device stretched the whole of it.

The app bar is not in the set, so it does not move. A scaled theme grows the page, the type ramp, the
tiles, the pivot and the focus ring around a bar that stays 54 dp with a 36 dp button in it. Two
numbers in the class are deliberately unscaled and each says so in a paragraph — `touchTargetMin`,
because thumbs do not grow, and `tiltDepression`, because the tilt is already proportional. The app
bar is unscaled by omission.

## The numbers are already upstream of the code

`reference/metro-compose-brief/references/metro-tokens.json` carries them:

```
metrics.windowsPhone.appBarHeightPx     = 72
metrics.windowsPhone.appBarMiniHeightPx = 30
metrics.windowsPhone.appBarIconPx       = 48
metrics.windowsPhone.appBarGlyphPx      = 26
```

which is exactly what the component's `// 72 px` comments restate by hand. `generate_tokens.py`
emits colours, accents and font sizes and does not cover metrics at all, so `KvadrantMetrics` is
hand-transcribed from the same file — and these four were transcribed into the component instead.
This is a gap between the token source and the token surface, not a new number.

- Rejected: **scaling them inside the component** from `KvadrantTheme.metrics.scale`. It fixes the
  scaling half and leaves the bar unstateable, so a theme still cannot say what its app bar is.
- Rejected: **extending the generator to metrics** as part of this. It is a larger change with its
  own argument — every other metric is already hand-written and correct — and doing it here would
  hide a five-line move inside it.
- Open while implementing: whether `KvadrantAppBarGlyphSize` is deprecated in favour of the token or
  kept as an alias. It is public, so somebody may be reading it.

## One golden moves, and it is the point rather than a side effect

`app_sample_window` (`SampleWindow.kt`, group `app`, 560×860) is the only fixture rendered through
`KvadrantMetrics().scaled(1.6f)`, and it contains a `KvadrantAppBar`. At 1.6 the bar becomes
86.4 dp, the button 57.6, the ring 1.8 and the glyph 31.2. Every other golden is at scale 1f and must
come out byte-identical.

## Acceptance

- AC: a theme built with the app bar's dimensions in `KvadrantMetrics` moves the bar.
- AC: `scaled(f)` moves the app bar with everything else, asserted in `MetricsScaleTest` beside the
  fields that are deliberately not scaled.
- AC: `app_sample_window` is re-recorded deliberately and named in the commit with the numbers above;
  no other golden moves.
- AC: the defaults are today's values, so an unscaled theme is unchanged.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantAppBar.kt`,
  `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantMetrics.kt`,
  `kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/demo/SampleWindow.kt`
