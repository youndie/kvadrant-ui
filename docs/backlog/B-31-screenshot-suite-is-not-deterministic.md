---
id: B-31
title: "Six goldens change between two recordings of unchanged source"
status: open
priority: P2
size: M
stage: stage-2-release
blocked_by: []
---

# B-31 — Six goldens change between two recordings of unchanged source

**Not reproduced by CI, and the escalation that said so was a misreading — withdrawn.**

Two runs looked as though they disagreed about thirty core goldens on the same runner image: one
failed two images, the next failed thirty, with nothing changed in that module between them. That is
not what happened. `:kvadrant-core:viddikVerify` **never ran** in the first of them — the adapter's
verify failed first and the build stopped — so the core suite was not green on Linux, it was
unexecuted. Checked by looking for the task in the log rather than for its result.

The numbers say the same thing more plainly: `type/ramp dark` differs by **1673 px** and
`screen/start dark` by **149 px** in *both* runs, to the pixel. Linux is deterministic. What differs
is Linux against macOS, which is a different problem with its own entry below, and this item stays at
P2 with its trigger intact.

*The trigger is unchanged and it is worth restating after a false alarm:* if `make screenshots` ever
names an image, or if two runs **that both executed** disagree, this comes back at P0.

---

*Everything below was written when the flake could not be reproduced.*

**Guarded, not fixed, and dropped to P2 on that basis.** `make screenshots` (`ROUNDS=n`) records the
suite n times and names every image that moved — the check the acceptance criteria asked for. Ten
consecutive recordings of the current suite come back byte-identical, and the script was shown to
work by adding a fixture keyed on `System.nanoTime()`, watching it flag that one image, and removing
it. The mechanism this item suspected is not present either: a toast handed `visible = true` on its
first composition has nothing to animate, and `StartScreen`'s `LaunchedEffect` does nothing when no
tile is pressed.

**Nothing was repaired.** A flake that cannot be reproduced has not been understood, and several
things that could plausibly explain it — the tilt camera becoming a `Dp`, the font family gaining
instanced weights — landed between the observation and the attempt. It stays open at P2 with a
trigger rather than being closed on ten green runs: **if `make screenshots` ever names an image, this
item comes back at P0 and that image's fixture is where to look.**

Everything below is the original finding, unamended, because it is the evidence.

`viddikRecord` twice in a row, with nothing edited in between, and six of the sixty-eight images
come back different: `screen_start_dark`, `screen_start_light`, `screen_start_amber`,
`appbar_start_with_bar`, `appbar_menu_open`, `picker_toast_and_badge`.

Measured on `screen_start_dark`: **147 pixels of 280 000**, inside a single 68 × 177 box, in shades
around the cyan accent — `(31, 162, 226)` in one recording against the accent's exact
`(27, 161, 226)` in the other. Something animated is being captured at an arbitrary phase.

**This is a P0 because of what it does to every other claim.** viddik's tolerance is 0.05% or 16 px;
147 of 280 000 is 0.052%. These fixtures sit *on* the threshold, so they pass and fail at random,
and the first thing anyone learns is that a red screenshot run means nothing. After that a real
regression in those six is invisible.

It also retro-actively weakens claims made from single runs. "Fifteen of the sixteen font goldens
came back byte-identical" ([B-07](B-07-font-stack.md)) was one sample, and is evidence about that
run rather than about the two rendering paths being equivalent. The same goes for every "no golden
moved" in this repository's history, including the one that argued the tilt camera change was inert
at density 1.

- **Find what is animating first, then decide.** The flapping set is not random: every one of those
  six shows the Start screen or the app bar, and `picker_toast_and_badge` contains a toast, which
  slides in from a `LaunchedEffect`. A fixture holding a component that animates on composition is a
  fixture whose capture time decides its pixels.
- **The fix is not a wider tolerance.** Raising it past 0.052% hides a real 0.05% regression, and the
  fixtures would drift again the moment something else animates. Either the fixture holds the
  animation still — the way `TurnstileTest` stops the clock — or the component is captured in a
  state it cannot leave.
- Rejected: recording until it passes. That is the behaviour the flake trains, and it is how a suite
  stops being read.

- AC: `viddikRecord` twice from unchanged source produces byte-identical images, checked by a script
  rather than by eye.
- AC: whatever is animating is named in `docs/research/research-architecture.md` §1.9, because the
  next person adding a fixture needs to know which components cannot go in one unheld.
- AC: the six are re-recorded once the cause is fixed, and the diff at that point is the measure of
  how much drift had accumulated.
- Anchors: `kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/demo/`
