---
id: B-43
title: "The Toolkit was read for metrics and never read for scope"
status: done
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

## The Toolkit's own source is reachable, which changed what could be transcribed

The item was written from the brief's list. The Toolkit itself is on GitHub —
`microsoftarchive/WindowsPhoneToolkit` — so `Microsoft.Phone.Controls.Toolkit.WP8/Themes/Generic.xaml`
and `AutoCompleteBox.cs` are primary sources rather than recollections, and both controls that got
built are transcriptions:

| Fact | Where verified |
|---|---|
| The text box's action target is `Width="84" Height="72"`, transparent, bottom right, holding a `26 × 26` image | `ActionIconBorder` in the `PhoneTextBox` template |
| The autocomplete list's fill is a literal **`Background="White"`** — not a theme brush — with `PhoneTextBoxEditBorderBrush` around it | the `AutoCompleteBox` template's `ListBox` |
| Its `Padding` is `0,8`, a row's `Margin` is `8,7`, the field's `Padding` is `6,0,6,4` | the same template |
| `FilterMode` defaults to `StartsWith`, `MinimumPrefixLength` to **1** | each property's `PropertyMetadata` in `AutoCompleteBox.cs` |

The white sheet is the find. It is the same decision the *focused* field makes — a dark theme's box
goes solid white — and it says what the control is: whatever the page behind it looks like, what you
are typing into is paper. Nothing about it could have been guessed from the dark theme.

Both sourced defaults are asserted by `AutoCompleteBoxTest`, because a default that came from a
document and a default somebody liked look identical in a signature.

**One deviation, named.** The Toolkit puts the suggestion list in a `Popup` so it floats over what
follows; this draws it in the control's own layout, which pushes the rest of a form down. A `Popup`
renders outside the layout it belongs to, where neither a screenshot of the component nor a page
placing it can reach — and a component whose only visible feature lives in a window nothing here can
photograph is one nobody can check. A caller who wants the float puts it in a `Box`.

## Why this is one item and not seven

Because the decision worth making is the list, not the controls. Each of these is a day; choosing
which of them a Metro library is expected to have is the part that needs argument, and splitting it
into seven items would make that argument seven times in seven places.

## Acceptance

- ~~AC: the catalogue gains a section naming what Microsoft shipped that this does not.~~ Done, and
  every row now carries a **verdict** rather than a status: three accepted and unbuilt, three
  rejected with the reason, one deferred whole.
- ~~AC: `AutoCompleteBox` and the text box's action icon exist.~~ Done —
  `KvadrantAutoCompleteBox` and `KvadrantTextBox(actionIcon = …)`, both transcribed from the
  Toolkit's own source rather than from the brief's description of it. The icon is a slot, because
  [D10](../research/research-architecture.md) says no Segoe asset enters this repository.
- ~~AC: `LockablePivot` is settled in one sentence.~~ Done: `KvadrantPivot(swipeEnabled = …)`. The
  Toolkit needed a subclass because Silverlight had no way to turn a `Pivot`'s manipulation off from
  outside it; a pager takes a boolean, and a second component whose only difference is a boolean is
  a second thing to document, test and keep in step.
- ~~AC: the rest are individually accepted or rejected with a reason.~~ Done, in the catalogue where
  a reader meets them rather than here where they would not. `WrapPanel` is rejected because
  `FlowRow` is the same layout and is not ours to re-ship; `HyperlinkButton` because a Metro button
  *is* its border, so a hyperlink is a text style and a click; `ExpanderView`, `HubTile` and
  `MultiselectList` are accepted and unbuilt, each with what it is and why nobody has missed it.
- Anchors: `docs/components.md`, `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/`
