---
id: B-09
title: "Ten base controls: button, text box, toggle, progress, message box, list item"
status: done
priority: P0
size: L
stage: stage-1-core
blocked_by: [B-05]
---

# B-09 — Ten base controls: button, text box, toggle, progress, message box, list item

**Done so far:** `KvadrantText` (with the script split), `KvadrantSurface`, `KvadrantButton`,
`KvadrantToggleSwitch`, and the indeterminate `KvadrantProgressDots` — five 4×4 px squares on the
published 4.4 s cycle with its three-part non-linear path, which is the second most recognisable
motion Metro has.

Since then: `KvadrantTextBox` (placeholder that vanishes on focus, no internal scroll — both canon
and both written down as such), `KvadrantListItem`, and `KvadrantMessageBox` with **the positive
action on the left**, which is the opposite of Material and of Windows 8, plus a flag for the Win8
order.

Since then: `KvadrantPasswordBox` (the character you typed shows for two seconds or until the next
key, which is `passwordMaskMs` rather than a preference) and `KvadrantProgressBar` — a straight
line with no rounded cap, no gap before the head and no stop indicator, all three of which Material
grew and Metro never had.

**A correction to what this item said.** It claimed the toggle's numbers do not reconcile: track 89,
thumb 28, published travel 69, and 89 − 28 = 61. They do. The thumb's own margin is **-4,0** — it
starts four pixels outside the track and ends four outside on the other side, so 89 − 28 + 4 + 4 is
exactly 69. The geometry was right and the reading was wrong; the code now carries the overhang and
the published travel, with the snap on its exponential-in(15).

`KvadrantCheckBox`, `KvadrantRadioButton` and `KvadrantToggleSwitch` are transcribed from the
phone's own templates — which were found only after they had been built by guesswork and someone
looked at the result. `WPDT_DESIGN_SYSTEM_WINDOWS_XAML` in the same cabinet as everything else holds
the first two; the toggle is the Toolkit's. What the guesses got wrong, and why it mattered, is in
[research §1.12](../research/research-architecture.md) — the short version is that the accent belongs
to the **pressed** state and not the checked one, and the tick is a filled path whose arms have
different weights.

The `Slider` template is in that same file and has not been read yet, so its thumb dimensions are
still this project's and stay parameters.

Since then: `KvadrantListPicker` — **the threshold is five**, which is Microsoft's: up to five
options it unfolds in place over 200 ms, beyond that the phone navigated to a page instead, and the
control reports which rather than deciding for the caller. `KvadrantLoopingSelector` and
`KvadrantPickerPage` — 148×148 px squares with 54 px numbers, tipping in from −50° and leaving by
turning to +90° rather than sliding, because on the phone a picker **was a page** and a picker that
fits in a popover is a different control wearing the name. And `KvadrantToast`, ten seconds, **from
the top** — Material's snackbar rises from the bottom over the content it is about, Metro's comes
down over the status bar, because the bottom of a phone belongs to the application bar.

**Left:** nothing named in this item. What remains of the catalogue is elsewhere.

**One thing a still cannot show, so it is tested instead:** the running dots are an animation, and a
screenshot of them is a screenshot of frame zero. `dotPath()` is a value rather than a literal
buried in the composable, and three tests sample it — that it hits the published thirds at the
published times, that the middle leg is the slow one, and that it never runs backwards.

`KvadrantButton`, `KvadrantTextBox`, `KvadrantPasswordBox`, `ToggleSwitch`, both `ProgressBar`
forms, `MessageBox`, `KvadrantListItem`, `KvadrantSurface`, `KvadrantText`. All of them have
complete published metrics, so this is transcription plus care rather than design.

- **The indeterminate progress bar is the one to get exactly right.** Five 4×4 rectangles running
  across a 4.4 second cycle is the single most recognisable Metro motion after tilt, and an
  approximation of it reads as a knock-off immediately.
- `ToggleSwitch` is the most completely documented control in the specification — 136×95 hit area,
  89×34 track, 28×38 thumb, 69 px travel, 50 ms snap on an exponential-out curve — so it is the
  control that proves the token layer works.
- Two behaviours that look like bugs and are canon, to be written down in KDoc rather than fixed:
  the text box does **not** scroll internally, and its hint disappears the moment it has focus or
  content.
- `MessageBox` takes OK / OKCancel only, with the **positive action on the left** — the opposite of
  Material — and a flag for the Win8 order ([research §1.3](../research/research-architecture.md)).
- Not covered: checkbox, radio button, slider, list picker, context menu and date picker, which are
  phase-3 work.

- AC: each control matches its numbers from `metro-tokens.json` in a screenshot test at dark and
  light × four accents.
- AC: every deliberate deviation from modern expectation carries a KDoc sentence saying it is canon.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/components/`

## Beyond the ten

Two more arrived with them because their behaviour is documented and distinctive:

- **`KvadrantLongList` and `KvadrantJumpList`.** The group letter is **in the accent colour** at
  22 sp SemiLight — that is `LongListSelectorGroupHeaderStyle` in the theme dictionary, not a
  decoration invented here — and the jump list draws letters with nothing behind them in the
  inactive colour rather than hiding them, so the alphabet keeps its shape under a thumb.
- **`KvadrantContextMenuHost`.** The page scales to **0.94 over 420 ms** and stays perfectly sharp.
  Nearly every reimplementation blurs it, because that is what a modern sheet does; there is no blur
  anywhere in the original.
