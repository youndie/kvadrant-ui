---
id: B-40
title: "Nothing is focusable, on the two targets where a keyboard is the input"
status: done
priority: P1
size: M
stage: stage-2-release
blocked_by: []
---

# B-40 — Nothing is focusable, on the two targets where a keyboard is the input

No `focusable`, no `onKeyEvent`, no `FocusRequester` anywhere in `kvadrant-core`. Tab moves nothing;
space and enter activate nothing; a text box takes focus only because `BasicTextField` brings its own.

That was defensible while this was a phone library on one renderer. It stopped being defensible when
the documentation site started running every component in a browser and `:sample:run` started opening
a window — **on both of those a keyboard is the primary input**, and the site is the first thing a
prospective consumer touches.

## This is a deviation, and it has to be argued rather than assumed

Windows Phone had no keyboard navigation. There is no canon to restore here and nothing to transcribe
— which puts it in the same category as everything else this library does that the phone did not, and
therefore behind the reasoning `remastered` exists for.

**But not necessarily behind the flag.** The flag is for things that change how a component *looks*
or *moves*, so that a fidelity claim stays falsifiable by looking. A focus ring changes appearance and
belongs there; being reachable by Tab does not, and gating it would mean shipping a library whose
default is unusable without a touchscreen on two of its three targets. Deciding that split is the
first half of this item, and it should be decided before any of it is written.

## What it turned out to be

**The first paragraph of this item is half wrong, and the wrong half is the loud half.** `grep`
found no `focusable` and no `onKeyEvent` in `kvadrant-core`, and the conclusion drawn from that —
that Tab moved nothing and space activated nothing — did not survive being run. `Modifier.clickable`,
`toggleable` and `selectable` each bring `Modifier.focusable` and their own space/enter handling, so
every control built on one of the three had the whole of it already. An absent call is not an absent
behaviour when the behaviour arrives inside somebody else's modifier.

**What was genuinely unreachable was `kvadrantTilt`** — this project's own gesture, whose KDoc said
in as many words that keyboard activation and focus were still `clickable`'s job and that a surface
needing them should keep `clickable`. The only surface that uses it is `KvadrantTile`, the tile is
what the library is *for*, and "use something else" means "give up the finger-tracking the modifier
exists for". A sentence, not a plan; it is now a `focusable` and a key handler over the same
interaction source.

**The ring is the decision, and the input mode is what makes it affordable.** Windows 8's template
draws its dotted rectangle from `Focused` and leaves `PointerFocused` empty. Compose has no such
pair of states — but on desktop `clickable` takes focus on click, so a ring drawn on focus alone
would have sat around every tile a mouse had ever touched *and* appeared in every pressed golden in
the screenshot suite. `InputModeManager` is the fact under Microsoft's two states, and gating on it
is that template's condition in this framework's vocabulary. Research
[D19](../research/research-architecture.md).

## Acceptance

- ~~AC: the split above is settled and written into research.~~ Done — [D19](../research/research-architecture.md), and the answer is that **all three are default**, with the
  indicator conditional on the keyboard being in use rather than on the flag. `remastered` gates
  what changes how a component looks, and by that letter the ring belongs behind it; by its purpose
  it does not, because gating it ships a default in which Tab moves an invisible cursor. The
  conflict dissolves once the ring cannot appear without a keyboard, which a Windows Phone did not
  have.
- ~~AC: every interactive control is reachable by Tab and activated by space or enter, in the order
  it appears on screen.~~ Done. Reachability is guarded across the **whole preview registry** by
  `PressableNodesAreReachableTest`, so the next component is covered by having a preview at all; the
  order and the activation keys by `KeyboardNavigationTest`. Activation for the `clickable` family
  is the framework's and is asserted rather than reimplemented; for the tile it is this library's
  and is asserted directly.
- ~~AC: the focus indicator is transcribed from something.~~ Done — the WinRT XAML 8.1 `Button`
  template, and it is a better find than expected: **two** rectangles a dash apart, black and white
  in every theme, which is why the ring is legible on any background without choosing a colour.
  [B-22](B-22-win8-branch.md) put the Win8 *profile* out of scope and that still holds — one
  template read for one visual is not a second design profile.
- ~~AC: a test drives Tab through a preview and asserts the order.~~ Done, and it asserts against
  the **measured** positions rather than against its own list, so a layout that reverses while the
  focus order does not is what fails. A `Row` inside the `Column` is deliberate: a single column
  cannot tell an order that follows the screen from one that follows the composition.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/KvadrantFocusRing.kt`,
  `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt`,
  `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/KvadrantTiltModifier.kt`
