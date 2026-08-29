---
id: B-43
title: "The Toolkit was read for metrics and never read for scope"
status: open
priority: P2
size: M
stage: stage-3-completeness
blocked_by: []
---

# B-43 — The Toolkit was read for metrics and never read for scope

The Silverlight for Windows Phone Toolkit is one of this project's primary sources — `Generic.xaml`
is quoted in the toggle switch, the list picker and the type ramp. It was mined for numbers and never
listed. Seven of its controls appear in neither the brief's catalogue of 55 nor here:

| Control | What it is | Verdict |
|---|---|---|
| `AutoCompleteBox` | a text box with a suggestion list | **Take it.** Its type slot is *already transcribed* — `KvadrantTypography.mediumLarge`'s KDoc names it as one of four controls Microsoft sized up from the page default, so the library documents a control it does not have |
| `PhoneTextBox` action icon | the text box with a trailing action glyph | **Take it.** `KvadrantTextBox` is the plain half of a control that had two |
| `ExpanderView` | an item that expands to reveal children, as the Mail app's threads did | Take it |
| `HubTile` | an animated tile with title, message and notification — *not* `FlipTile`, which only turns over | Take it |
| `MultiselectList` | a list with a selection mode and per-row check boxes | Take it — it is where `KvadrantCheckBox` was going to be used, and nothing uses it today |
| `LockablePivot` | a pivot whose swipe can be disabled | Small; a parameter on `KvadrantPivot` rather than a component |
| `WrapPanel` | non-virtualised wrapping layout | **Leave it.** Compose's `FlowRow` is the same thing and is not ours to re-ship |

`HyperlinkButton` is a platform control rather than a Toolkit one and is also absent. It is not a
variant of `KvadrantButton`: the button *is* its border, and a hyperlink has none.

## Why this is one item and not seven

Because the decision worth making is the list, not the controls. Each of these is a day; choosing
which of them a Metro library is expected to have is the part that needs argument, and splitting it
into seven items would make that argument seven times in seven places.

## Acceptance

- AC: the catalogue in `docs/components.md` gains a section naming what Microsoft shipped that this
  does not, so the gap is visible from the document a reader actually opens.
- AC: `AutoCompleteBox` and the text box's action icon exist, being the two the library already
  refers to in its own KDoc.
- AC: `LockablePivot` is settled as a parameter or rejected, in one sentence.
- AC: the rest are individually accepted or rejected *with a reason* — "not now" is fine, silence is
  not, because silence is what produced this item.
- Anchors: `docs/components.md`, `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/`
