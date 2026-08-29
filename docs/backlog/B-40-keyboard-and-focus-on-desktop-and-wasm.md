---
id: B-40
title: "Nothing is focusable, on the two targets where a keyboard is the input"
status: open
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

## Acceptance

- AC: the split above is settled and written into research D17 — which of focus order, activation
  keys and a visible focus indicator are default, and which are `remastered`.
- AC: every interactive control is reachable by Tab and activated by space or enter, in the order it
  appears on screen.
- AC: whatever the focus indicator turns out to be, it is transcribed from something. Windows Phone
  has nothing to offer, but **Windows 8 does** — it had keyboard focus visuals, and this library
  already has a Win8 profile deferred rather than rejected ([B-22](B-22-win8-branch.md)). Look there
  before inventing a rectangle.
- AC: a test drives Tab through a preview and asserts the order, because focus order is exactly the
  thing that silently reverses when a layout is refactored.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/`
