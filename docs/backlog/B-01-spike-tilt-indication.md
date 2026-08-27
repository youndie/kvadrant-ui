---
id: B-01
title: "Can tilt be the default indication, or must the consumer apply it by hand?"
status: done
priority: P0
size: L
stage: stage-0-spikes
---

# B-01 — Can tilt be the default indication, or must the consumer apply it by hand?

**Done. The answer is yes**, and it is what kept `Modifier.kvadrantTilt()` out of every component
signature in the library. `TiltIndication` is an `IndicationNodeFactory` provided as
`LocalIndication` by `KvadrantTheme`, so the tilt arrives on every clickable surface without one
call site mentioning it.

The other three criteria are answered in [research §1.6](../research/research-architecture.md): the
brief's 0.975 prediction was refuted (skiko uses `cameraDistance * 72`), `Press.pressPosition`
arrives in the coordinate space the formula expects, and both are written up there rather than here.

**Frame budget, measured.** Pixel 6a, 60 Hz, the fitted metric scale, 25 full press-and-release
cycles per run so that every 200 ms hold and 100 ms return plays out. Read from
`dumpsys gfxinfo` after a `reset`.

| surface | frames | janky | 50th | 99th |
|---|---|---|---|---|
| medium tile, 189×189 dp | 546 | 0 (0.00%) | 5 ms | 12 ms |
| wide tile, 390×189 dp | 547 | 6 (1.10%) | 6 ms | 17 ms |
| wide tile, 390×189 dp, **repeat** | 549 | 1 (0.18%) | 6 ms | 12 ms |
| small tile, ~74 dp | 545 | 6 (1.10%) | — | 19 ms |

**The repeat is the row that matters.** After the first two runs the obvious reading was that jank
scales with the area being transformed — 0% on the medium tile, 1.1% on the wide one. Running the
wide tile again gave 0.18%, and the small tile — the least area of the three — gave the same 1.10%
as the wide one's worst run. **The spread between two identical runs is larger than the spread
between the surfaces**, so the size hypothesis is not supported by this data and the honest statement
is that it was not tested.

What is not in doubt: the tilt draws far inside the budget. The GPU is idle — 518 of 549 frames at
1 ms, 99th percentile 2 ms — and the CPU median is 5–6 ms against 16.7. The outliers are single
frames at 20 and 24 ms, and the harness is a plausible source: it starts fifty `input` processes on
the device per run, competing for the same CPU. That was not separated out, because there is no
baseline available inside the app — press somewhere without a tilt and Compose draws no frames at
all, so there is nothing to compare against.

The one thing being an indication costs is that the tilt cannot follow a moving finger, which the
original did — [B-27](B-27-tilt-does-not-follow-the-finger.md), and it is a price worth paying.

Tilt is the signature of Metro the way ripple is the signature of Material: the plane rotates
towards the finger instead of a circle growing out of it. For it to be automatic — every clickable
surface tilting without the consumer doing anything — the tilt node has to be a `LayoutModifierNode`
created by an `IndicationNodeFactory`. Reading the sources says that slot is free: `Modifier.indication`
wraps the node in a `DelegatingNode`, `AbstractClickableNode` implements no layout delegate of its
own, and `DelegatingNode.delegate()` recreates the coordinator when a layout kind is delegated
(see [research §1.8](../research/research-architecture.md)). Nothing has been executed.

- **Build a prototype before anything else is estimated.** Every size in this backlog assumes tilt
  is automatic. If it is not, the public API grows an explicit `Modifier.kvadrantTilt()` that the
  consumer has to remember on every surface — a working library, a worse one, and a different set
  of component signatures.
- The rejected shortcut is starting with the explicit modifier "to be safe". That guarantees the
  worse API without ever finding out whether the good one was available.
- Not covered here: production quality, easing of the return, or `BitmapCache`-equivalent
  rasterisation. That is [B-08](B-08-tilt-indication.md).

**Answered on desktop: the slot is free.** A press routed through `Modifier.clickable` renders
pixel-identically to one routed through `Modifier.indication` — 116×116 at the centre, the same
trapezoid at a corner, the same bounding boxes. Tilt can be the default indication and the fallback
API is not needed. Two findings came out with it, both in
[research §1.6](../research/research-architecture.md): the transcribed formula was missing the
definition of `xContrib` and had to be recovered from the original, and the 0.975 prediction below
is **refuted** — Compose's `cameraDistance` is depth in `cameraDistance * 72` pixels, not a density
multiplier, and the real shrink at the default camera is 0.9685.

**Left, and it is no longer just a measurement.** Scoping the remainder turned up a defect:
`cameraDistance` is expressed in **inches**, and the backends convert it differently — skiko at a
fixed 72 px per inch, Android at the display's real dpi
([research §1.6](../research/research-architecture.md)). The same value is 576 px of depth on
desktop and about 3840 px on a 480 dpi phone, so the tilt is roughly six times flatter there; and
`depressionScale()` hard-codes skiko's 72, so the sinking it computes is wrong on Android
specifically.

What remains has moved out of this item, because it is no longer spike work:

- the camera defect is [B-25](B-25-tilt-camera-is-in-inches.md), which is a fix rather than a
  question;
- both it and the frame budget need a target that does not exist, so
  [B-24](B-24-add-the-android-target-next.md) comes first — Android ahead of the plan, because it is
  the only renderer that is not skiko and therefore the only one that can contradict anything
  measured so far;
- wasm and iOS stay where D14 put them. They are on skiko, and a target that agrees with what you
  already have teaches you nothing.

This item stays `wip` until the frame budget is measured; the question it was opened to answer —
whether tilt can be the default indication — is answered and will not reopen.

- AC: a 210×210 dp tile tilts towards the touch point at 60 fps on a mid-range Android device,
  and the same prototype runs on desktop JVM, iOS and wasmJs. *Desktop done; the rest waits for
  those targets to be added (D14).*
- AC: the measured perspective scale is compared against **0.975**, the WinJS `pointerDown` value.
  **Done, and it does not match**: measured 116/120 = 0.967 against 0.9685 computed, at Compose's
  default camera. The depression emulation is this project's invention and says so in KDoc.
- AC: `Press.pressPosition` is confirmed to arrive in the coordinate space the formula expects.
  *Done — the five press points land where the formula puts them, and the four corners are mirror
  images of each other.*
- AC: the answer is written back into [research §1.6](../research/research-architecture.md) at the
  point of divergence if the prototype contradicts it.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/Tilt.kt`,
  `.../indication/TiltIndication.kt`, `kvadrant-core/src/desktopTest/kotlin/.../TiltScreenshots.kt`
