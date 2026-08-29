---
id: research-component-coverage
title: "What the component set is still missing"
type: research
status: active
---

# What the component set is still missing

Forty-eight public composables exist. This asks what is *not* there, against three lists rather than
against an impression: the vendored brief's own catalogue of 55, the controls Microsoft actually
shipped, and the behaviours a Metro surface had that are not components at all.

The last group turned out to matter most. **Two of the three most valuable findings here are not
missing components but missing behaviours in the components that exist** — and both were invisible
because nothing in this repository is shaped to notice them.

## 1. Against the brief's own catalogue

| Fact | Where verified |
|---|---|
| The brief lists 55 positions; 48 public composables exist | `reference/metro-compose-brief/05-component-catalog.md`, `docs/components.md` |
| Nine of its positions were never built | table below |

| Brief | What | Priority there | Why it is still absent |
|---|---|---|---|
| N5 | `PageHeader` (Win8) | P2 | The Win8 profile was deferred whole — B-22 |
| N6 | `Hub` (Win8) | P2 | Same |
| N7 | `SemanticZoom` | P3 | Same, and XL |
| T7 | Tile drag-reorder | P3 | Never started |
| C11 | `DatePicker` / `TimePicker` | P2 | **The primitives exist and the component does not** — see below |
| L4 | `AcronymIcon` | P2 | Never started |
| M6 | `ContinuumTransition` | P3 | Only WinJS numbers are known |
| M7 | `EntranceTransition` (Win8) | P2 | Win8 profile |

**C11 is the interesting one.** `KvadrantLoopingSelector` and `KvadrantPickerPage` are the two halves
a date or time picker is assembled from, and both are built, tested and previewed. What is missing is
the assembly: a component that puts three looping selectors on a picker page and returns a date. It
is the smallest piece of remaining P2 work with every metric already published, and it is the one a
consumer will ask for first, because a form needs a date.

## 2. Against what Microsoft shipped

The Silverlight for Windows Phone Toolkit shipped a control set this project has read from but never
inventoried. Seven of its controls appear in neither the brief's catalogue nor here:

| Control | What it is | Worth having? |
|---|---|---|
| `AutoCompleteBox` | text box with a suggestion list | **Yes.** Its type slot is already transcribed — `KvadrantTypography.mediumLarge`'s KDoc names it as one of four controls Microsoft sized up from the page default |
| `ExpanderView` | an item that expands to reveal children, as the Mail app's threads did | Yes, P2 |
| `HubTile` | an animated tile with a title, a message and a notification — distinct from `FlipTile`, which only turns over | Yes, P2 |
| `MultiselectList` | a list with a selection mode and per-row checkboxes | Yes, P2 — and it is where `KvadrantCheckBox` was going to be used |
| `WrapPanel` | non-virtualised wrapping layout | Marginal; Compose's `FlowRow` covers it |
| `LockablePivot` | a pivot whose swipe can be disabled | Marginal, but small |
| `PhoneTextBox` action icon | the text box with a trailing action glyph | Yes — our `KvadrantTextBox` is the plain half of a control that had two |

`HyperlinkButton` is a platform control rather than a Toolkit one, and is also absent. It is a text
button with no border, which is not a variant of `KvadrantButton` — the button *is* its border.

## 3. Against behaviours, which is where the real gaps are

### 3.1 Overscroll: the theme replaces the ripple and leaves the glow

**Windows Phone's lists compressed at their ends**, and this is not folklore: Microsoft added
`HorizontalCompression` and `VerticalCompression` visual state groups to `ScrollViewer` in Windows
Phone 7.1 precisely so applications could react to it. It is as recognisable as the tilt and it is
what a finger meets every time a list runs out.

**This library has no overscroll at all** — `grep` finds the word nowhere in `kvadrant-core`. What
that means in practice is worse than absence: on Android the platform's own *stretch* is still there,
so a Metro list currently ends with an Android gesture. That is exactly the error `KvadrantTheme`
already avoids for presses, where it replaces `LocalIndication` with the tilt rather than leaving the
ripple in place.

The mechanism is the same shape and is available in the version already pinned: Compose Foundation
1.12.0 carries `OverscrollEffect`, `OverscrollFactory` and `LocalOverscrollFactory`, verified by
unpacking `foundation-desktop-1.12.0.jar`. A theme that provides an overscroll factory is one line
beside the one that provides the indication.

### 3.2 Accessibility is touch targets and contrast, and its item is named as if it were more

[B-11](../backlog/B-11-accessibility-policy.md) is titled "accessibility policy" and its acceptance
criteria are a 48 dp touch area, an opt-in contrast palette, and a contrast test. Those are real and
they are done. What the title suggests and the item does not cover: **roles, state descriptions and
labels**. The whole library contains six mentions of `semantics`, `contentDescription` or `Role`, and
one of those is the tilt modifier merging descendants so a click is reported at all.

A screen reader on `KvadrantToggleSwitch` gets a box that is clickable and nothing about it being a
switch or being on. That is not a gap in the policy — it is a gap the policy's name conceals, which
is why it is written here rather than left to be discovered by someone reading the title.

**Closed by [B-39](../backlog/B-39-semantics-beyond-touch-targets.md).** The six controls with a
state now carry it, and no pixel moved. The part worth keeping is the second test: it walks every
preview in the registry and refuses anything pressable that cannot be named, so the *next* control is
covered by existing to be previewed. On its first run it found seven anonymous press targets and four
were in the previews written alongside the fix — a parameter nobody passes is not a fix.

What overstated B-11 turned out to be its **filename** rather than its title: `title:` said
"authentic visuals with extended hit areas, and an opt-in contrast palette", which is exactly what it
did. `B-11-accessibility-policy.md` is what a reader sees in a link. An id is a name, and that one
was making a claim.

### 3.3 Focus and keyboard, on the two targets where they are the input method

Nothing in the library is focusable, handles a key event or requests focus — `grep` finds no
`focusable`, `onKeyEvent` or `FocusRequester` anywhere. That was defensible while this was a phone
library on one renderer. It is not now: the documentation site runs every component in a browser and
`:sample:run` opens on a desktop, and on both of those a keyboard is the primary input.

Windows Phone itself had no keyboard navigation to be faithful to, so **this is a deviation to decide
rather than a piece of canon to restore**, and it belongs behind the same reasoning as `remastered`.

### 3.4 RTL is canon, and three layouts would break

Windows Phone supported right-to-left out of the box: `FlowDirection` was set from the phone's
culture, with no work by the application. So Arabic and Hebrew are not an enhancement here, they are
something the original did and this does not.

The good half, measured: the library uses `padding(start`/`end` and `horizontal =` throughout and
**no** `left`, `right` or `absolute` variants, so most of it mirrors for free. The bad half: three
places compute an x offset by hand in a custom layout — the pivot header strip's parallax among them
— and those are the components whose whole identity is horizontal movement. Nothing tests any of it.

## Consequences

1. ~~**Overscroll is the highest-value single addition**~~ — done,
   [B-38](../backlog/B-38-the-theme-leaves-the-platform-overscroll.md). One factory in the theme,
   every scrolling surface at once, and three numbers that are ours because Microsoft published the
   visual states and none of their storyboards.
2. **An item's *name* overstated it** — and it was the filename, not the title. Corrected in place
   rather than renamed, because a rename breaks every citation.
   [B-39](../backlog/B-39-semantics-beyond-touch-targets.md) is done.
3. **The Toolkit was read for metrics and never read for scope.** Seven controls were transcribed
   from and then not counted.
4. The remaining component work is mostly P2 and mostly small, with one exception worth doing next:
   a date and time picker, whose parts are already built and tested.
