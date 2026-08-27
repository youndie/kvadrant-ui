---
id: B-32
title: "Twelve platform templates sit unread while the controls above them are guesses"
status: done
priority: P1
size: M
stage: stage-2-controls
---

# B-32 — Twelve platform templates sit unread while the controls above them are guesses

**Done, and the count was the misleading part.** Of the eleven unread `TargetType`s this library has
components over exactly two — `Slider` and `PasswordBox` — and the second turned out to be
`TextBox` with one number changed. Both were transcribed; the other nine have nothing above them to
correct and are read when something needs them. What the item was really about is the hit rate, and
it was three for three: every template opened this session contradicted the code above it.

The SDK's design-time `System.Windows.xaml` — the file named in research §1.12, the one Blend's
"Edit a Copy" reads — carries the phone's default template for seventeen `TargetType`s:

`Button`, `ButtonBase`, `CheckBox`, `ContentControl`, `HyperlinkButton`, `ListBox`, `ListBoxItem`,
`PasswordBox`, `ProgressBar`, `RadioButton`, `RepeatButton`, `ScrollBar`, `ScrollViewer`, `Slider`,
`TextBox`, `Thumb`, `ToggleButton`.

Five have been read: `CheckBox`, `RadioButton`, `ProgressBar`, `TextBox` under §1.12, and
`ButtonBase`/`Button` under this item's predecessor. The rest have not, and the components built
over them are reconstructions — `KvadrantSlider`, `KvadrantPasswordBox`, `KvadrantListItem`, the
scroll bar, the hyperlink.

**"Read" is not a property of a control, it is a property of a paragraph.** `ProgressBar` was on
the read list and its indeterminate half was wrong in four ways: both exponentials had the wrong
exponent, the last one had the wrong *direction*, the template's `Padding` was missing, and the
per-dot `Opacity` storyboard — two discrete keyframes that switch a dot off the moment it lands —
had not been implemented at all, so five dots stood against the right-hand edge for over a second
of every 4.4. The determinate half was wrong too: its track is the accent at a tenth, not a neutral.
Whoever ticks a control off this list should say which *states* were transcribed, not which control.

**The point is not that the reconstructions are wrong.** It is that nobody knows, and finding out is
a text search in a file already on disk. The button was assumed right for the same reason all of
these are — the shape is simple — and it was wrong about its type ramp, its padding, its hit area
and a state it did not have.

## What the two said

| | as built | template |
|---|---|---|
| slider thumb | a 9×24 foreground block, "this project's own" | `PhoneSimpleThumb` is `<Rectangle Fill="Transparent"/>`; the thumb is 1 px wide with `ScaleX="32"` — a handle for the finger, nothing to see |
| slider track | 4 px, the progress bar's line | **`Height="12"`**, three times thicker, `Margin="0,22,0,50"` inside `PhoneHorizontalMargin` |
| slider track colour | `PhoneInactiveBrush` | `PhoneContrastBackgroundBrush` at `Opacity="0.2"` — within a hair of the same thing, now derived rather than coincidental |
| slider disabled | did not exist | track opacity to 0.1, fill to `PhoneDisabledBrush` |
| text box fill | transparent | **`PhoneTextBoxBrush`** — 75 % white on a dark page, 15 % black on a light one. A Metro field is a light box with dark text in *both* themes |
| text box focus | border to the accent | `PhoneTextBoxEditBorderBrush`, which is the page's foreground; the accent has no part in it |
| text box ink | the page's foreground | `PhoneTextBoxForegroundBrush`, dark on that fill either way — a new token, because `contrastForeground` inverts to white-on-white in the light theme |
| text box geometry | 6/6 padding, no overhang | border inside `PhoneTouchTargetOverhang`, `Padding="2"` plus an inner margin of `1,2` — and `3,2` for the password box, the only structural difference between the two templates |

## Acceptance

- Each control above either cites the template it was transcribed from, or carries a KDoc line
  saying the template was read and deliberately departed from, with the reason.
- Numbers that survive as this project's own move to parameters of the public API, per D5.
- Any correction that changes a golden re-records it in the same commit as the code.

## Notes

The file is not in this repository and does not come into it: it is Microsoft shared-source, and
[D11](../research/research-architecture.md#d11-apache-20-for-code-sil-ofl-11-for-the-resources-module-declared-separately)
keeps the tree to Apache-2.0 and OFL. It is evidence to read, like the toolkit sources, and what
lands here is the transcribed number with its citation.

Extraction: the cabinets are LZX. `cabextract` reads them; `bsdtar` does not, and a hand-rolled
MSZIP reader produces a file of exactly the right length full of noise.
