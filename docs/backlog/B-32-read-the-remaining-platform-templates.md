---
id: B-32
title: "Twelve platform templates sit unread while the controls above them are guesses"
status: open
priority: P1
size: M
stage: stage-2-controls
---

# B-32 — Twelve platform templates sit unread while the controls above them are guesses

The SDK's design-time `System.Windows.xaml` — the file named in research §1.12, the one Blend's
"Edit a Copy" reads — carries the phone's default template for seventeen `TargetType`s:

`Button`, `ButtonBase`, `CheckBox`, `ContentControl`, `HyperlinkButton`, `ListBox`, `ListBoxItem`,
`PasswordBox`, `ProgressBar`, `RadioButton`, `RepeatButton`, `ScrollBar`, `ScrollViewer`, `Slider`,
`TextBox`, `Thumb`, `ToggleButton`.

Five have been read: `CheckBox`, `RadioButton`, `ProgressBar`, `TextBox` under §1.12, and
`ButtonBase`/`Button` under this item's predecessor. The rest have not, and the components built
over them are reconstructions — `KvadrantSlider`, `KvadrantPasswordBox`, `KvadrantListItem`, the
scroll bar, the hyperlink.

**The point is not that the reconstructions are wrong.** It is that nobody knows, and finding out is
a text search in a file already on disk. The button was assumed right for the same reason all of
these are — the shape is simple — and it was wrong about its type ramp, its padding, its hit area
and a state it did not have.

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
