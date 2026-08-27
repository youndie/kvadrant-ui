---
id: B-15
title: "Motion: the easing catalogue and the turnstile transitions"
status: wip
priority: P1
size: M
stage: stage-2-release
blocked_by: [B-05]
---

# B-15 — Motion: the easing catalogue and the turnstile transitions

**Done:** `KvadrantEasing` — the primary bezier plus the two exponentials, which have no bezier
equivalent and so are the formula — and `KvadrantTurnstile`: −80° in over 350 ms, +50° out over
250 ms, about the left edge. The asymmetry is the original's and is what makes leaving feel quicker
than arriving. `gallery_turnstile_axis.png` holds the geometry still.

**TurnstileFeather is in**, and it is the one this item said was worth spending on: every row
rotates about **one** axis at `centerOfRotationX = -0.2`, off to the left of all of them, so the
list opens like a venetian blind rather than a deck of cards. Rows enter 40 ms apart and leave
50 ms apart. `list_turnstile_feather_axis.png` holds the geometry still, because a still of a
stagger is otherwise indistinguishable from a still of nothing happening.

**A correction:** the turnstile's exit easing is exponential-**in(6)**, not in(15). The 15 belongs
to the ToggleSwitch snap and to Swivel, and using it here made leaving snappier than the original.

**Slide and swivel are in.** Slide's four durations are **not one number** — left and right take
500 ms in and 300 out, up and down 350 and 250; horizontal movement gets more time than vertical,
which reads as the difference between changing place and changing level. Swivel tips around its
**top** edge, in from −45° and out to +90°, and that exit is the one place `exponentialIn(15)`
appears outside the ToggleSwitch, which is exactly why it snaps. Going back is gentler: +60° on
in(6).

**Continuum is not coming to this side, and that is now settled rather than pending.** Its numbers
were looked for in the whole WP8 SDK, in the Windows Phone Toolkit and in the Windows 10 SDK's
`generic.xaml`; all three came back empty. On the phone it was a **shell** transition, never exposed
to applications ([research §1.10](../research/research-architecture.md)). The WinJS version is fully
known and belongs to the desktop branch, where it is now recorded —
[B-22](B-22-win8-branch.md).

**Rotate and roll are in.** All eight rotate variants take **250 ms** whatever the angle — the same
number for a quarter turn and a half, which reads as the turn being one gesture rather than a
distance. Its opacity rides a sine while the transform rides an exponential, so the page fades
evenly while it accelerates. The roll is **two phases**: 0→45° over 300 ms easing out, then 45→90°
over another 300 **linearly**, and the change of curve halfway is its whole character — the page
eases into the turn and then completes it at a constant rate, which reads as something carried
rather than something falling. Opacity is not animated at all.

**Left:** the Windows 8 entrance transition, which belongs to the same branch.

`KvadrantEasing` as tokens — the primary and exit curves plus the exponential-out(6) and
exponential-in(15) the Windows Phone Toolkit uses — and the turnstile page transition: ±80° in,
+50° out, rotating about the left edge, 350 ms and 250 ms.

- **TurnstileFeather is the effect worth spending on**, and it is the one detail reimplementations
  miss: every list item rotates about **one shared axis** running through the vertical centre of the
  screen, not each about its own. That is what makes a list look like it is flying out of the page
  rather than like a stack of independently spinning cards. Stagger is 40/50 ms.
- All the numbers are published — from the Toolkit and from WinJS `Animations.js` — so this is
  transcription, and the risk is in the shared-axis geometry rather than in the timings.
- Not covered: swivel, continuum and the Win8 entrance transition.

- AC: the turnstile plays on page entry and exit in the sample on every target.
- AC: the feather variant demonstrably uses one axis for the whole list, verified by a screenshot at
  mid-animation rather than by reading the code.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/theme/KvadrantMotion.kt`
