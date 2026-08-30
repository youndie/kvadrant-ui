---
id: research-remastered-lane
title: "Is there a remastered version to build?"
type: research
status: active
date: 2026-08-30
---

# Is there a remastered version to build?

The question asked: an **enhanced** version of what exists — more animation, improvements, more
components. The library already has a place for exactly that,
`KvadrantTheme(remastered = true)`, decided in [D17](research-architecture.md) and built in
[B-28](../backlog/B-28-remastered-flag.md), off by default so that the fidelity claim stays
falsifiable.

So this is not "should the flag exist" — it does. It is "**is there a second library behind it, and
is the flag the mechanism that gets us there**". Measured, the answer separates into three parts,
and only one of the three is about the flag at all.

Nothing here is scheduled and no backlog item is opened.

---

## 1. Verified facts

### 1.1 The flag is public, frozen, and read by nothing

| Fact | Where verified |
|---|---|
| `getRemastered` is in the pinned public ABI | `kvadrant-core/api/desktop/kvadrant-core.api:592` |
| `KvadrantTheme` provides `LocalKvadrantRemastered` and passes the value to exactly one thing: `animatePress` on the indication | `KvadrantTheme.kt:110` and `:115` |
| **No component in `commonMain` reads `KvadrantTheme.remastered`** — the accessor has zero call sites inside the library | `grep -rn "LocalKvadrantRemastered\|KvadrantTheme.remastered" kvadrant-core/src kvadrant-material-adapter/src` |
| The one gated behaviour is a 100 ms press-in, and its own KDoc says the number is this project's in both senses | `TiltIndication.kt`, `PRESS_MILLIS` |

**Consequence — the flag is a convention with one instance, which is what D17 said it was.** D17
called that out honestly: "the flag's value is a convention, not a mechanism… what is worth it is
that the next improvement has somewhere to go". Two days after the first release, the next
improvement has not arrived. That is not a defect; it is the measurement this question needs, and it
means the flag is currently a **public accessor the library does not use** — API that a consumer can
read and that changes nothing they can see except one press.

### 1.2 The remastered lane is unphotographed, and this repository knows what that costs

| Fact | Where verified |
|---|---|
| **126 goldens** exist: 72 in the core, 51 in the previews, 3 in the adapter | `find . -name '*.png'` under `src/desktopTest/snapshots` |
| **Zero** of them are recorded with `remastered = true` | `ls kvadrant-core/src/desktopTest/snapshots \| grep -i remaster` → 0 |
| One test file covers the lane, with **two** tests: one that the press sinks, one that the default is `false` | `RemasteredTest.kt` |

**Consequence — the repository's strongest guard is blind to the entire lane.** `check` is a
rendering suite: it photographs components and counts ink. Everything it photographs is canon.
A remastered behaviour added today would be verified by whatever behavioural test its author
remembers to write, and by no image at all — while a green build would read exactly as it does now.

This project has already paid for that shape of error twice, and both are written down: the
on-device guard nobody ran for months ([B-36](../backlog/B-36-the-on-device-guard-does-not-execute.md)),
whose silence read as success while the Android artefact shipped with no fonts
([B-37](../backlog/B-37-the-android-artefact-ships-without-its-fonts.md)). **A remastered lane that
grows without a golden set is the same shape a third time.** The cost is not "write some tests
later": it is that the golden set roughly doubles for every component whose remastered rendering
differs, and the suite is already calibrated to one rasteriser on one machine
([B-35](../backlog/B-35-cyrillic-renders-differently-on-linux.md)).

### 1.3 "More animation" is not the gap

| Fact | Where verified |
|---|---|
| **61** animation call sites (`Animatable`, `animate*AsState`, `tween`, `spring`, `updateTransition`) in `commonMain` | `grep -c` over `kvadrant-core/src/commonMain` |
| They are in **16 of the 21** component files, plus the indication, the overscroll and the theme | same, by file |
| The turnstile, the swivel, the tile flip, the panorama settle, the toast, the progress cycle and the overscroll fling all exist and are tested | `KvadrantTurnstile.kt`, `MessageBoxSwivelTest`, `KvadrantTile.kt`, `PanoramaSnapTest`, `KvadrantToast.kt`, `KvadrantProgressTest`, `OverscrollFlingTimelineTest` |

**Consequence — the library is not short of motion, it is short of motion the phone did not have.**
And that set is small on purpose: [D17](research-architecture.md)'s survey found exactly one, because
the phone animated nearly everything it moved. Asking for "extra animations" against a canon that is
already almost fully animated produces either invention or the *other lineage's* numbers (§1.6).

### 1.4 The request names three things, and they need three different mechanisms

| What is being asked for | The mechanism that fits | Why the flag is not it |
|---|---|---|
| A **behaviour the phone did not have** | `remastered` ✅ | — |
| A **number nobody published** (tile interval, panorama parallax, placeholder opacity) | a parameter of the public API with a KDoc line saying it is ours | D17, verbatim: "turning a number off leaves no number". There are **8 such markers across 7 files** today — the parameter mechanism carries eight times the load the flag does |
| A **component that did not exist** | a new public composable | **Not gateable at all.** `remastered` is a `CompositionLocal` read at composition time; it cannot remove a declaration from the ABI. `checkKotlinAbi` sees a "remastered component" exactly as it sees any other, and so does a consumer's autocomplete |

**Consequence — two thirds of "enhanced version" is not a flag question.** A remastered *component*
is just a component; the only thing the flag could do is change how it looks once it exists, which
is a smaller decision than whether to build it. And the component backlog for that is already
written and is mostly **canon rather than enhancement**:
[research-component-coverage.md](research-component-coverage.md) lists seven Silverlight Toolkit
controls this project transcribed metrics from and never inventoried — `ExpanderView`, `HubTile`,
`MultiselectList`, the `PhoneTextBox` action glyph among them — plus `HyperlinkButton`. Those are
things Windows Phone *had*. Building them is fidelity work, ungated, and it is a better use of the
same effort than an enhanced tier of what exists.

### 1.5 The one genuine deviation the shipping targets want is hover, and it is sourced

| Fact | Where verified |
|---|---|
| **Zero** hover call sites in `commonMain` — no `hoverable`, `PointerOver`, `isHovered` or `HoverInteraction` anywhere | `grep -rn` over `kvadrant-core/src/commonMain` |
| `TiltIndication`'s node collects `PressInteraction` only; focus arrives through a separate modifier and is gated on the input mode | `TiltIndication.kt:203–231`, and [D19](research-architecture.md) |
| The library ships on two targets whose primary input is a pointer: the JVM desktop and wasm — and the documentation site runs every component in a browser | [D14](research-architecture.md), [B-34](../backlog/B-34-component-documentation-site.md), `make site` |
| The desktop lineage published 74 `PointerOver` brushes with dark, light and high-contrast values | [research-desktop-lineage.md](research-desktop-lineage.md) §1.2 |

**Consequence — this is the only candidate that satisfies all three tests at once.** It is a real
deviation, because Windows Phone had no pointer and therefore no state to restore — so it cannot
sneak in ungated the way finger-tracking legitimately did
([B-27](../backlog/B-27-tilt-does-not-follow-the-finger.md)). It is wanted by targets that actually
ship. And its values would not be invented: 74 sourced brushes exist, from Metro's own desktop half.

**It is also where the two questions meet.** A hover state is the first third of a Windows 8.1
profile ([research-desktop-lineage.md](research-desktop-lineage.md) §1.3, Consequence 3), and it is
the most defensible single entry the remastered table could gain. Doing it once serves both, and
doing it under `remastered` keeps the phone claim intact — a Metro surface that lights up under a
mouse is not what a Lumia did.

**Precedent for how, and it argues against the flag.** [D19](research-architecture.md) faced the same
choice for the keyboard focus ring and resolved it *without* the flag: the ring is drawn when the
input mode is `Keyboard`, so it appears for a keyboard user and never in a screenshot. Gating on
**the input actually in use** is truer than gating on taste, and the same reasoning transfers
directly — a pointer-over state that only exists while a pointer exists is arguably canon-preserving
rather than a deviation. *This is a hypothesis, and its address is the first hover implementation:*
if the state can be made invisible to the golden suite the way the ring was, it does not need the
flag; if it cannot, it does.

### 1.6 The remaining candidates, surveyed

| Candidate | Canon | What a remastered version would be | Verdict |
|---|---|---|---|
| Pivot header selection | opacity 0.4 → 1 at `Duration="0"`, no crossfade (`KvadrantPivot.kt:34`) | the desktop lineage's **167 ms** linear crossfade ([§1.11](research-architecture.md)) | A real deviation with a **sourced** number — the strongest table row after hover |
| Progress bar landing | instant, then empty for the rest of the cycle (`KvadrantProgress.kt:40`) | a settle | Deviation, but the number would be ours |
| Live tile interval | Microsoft never specified it and never let apps set it ([§1.10](research-architecture.md)) | — | **Parameter**, not the flag — open question 2 already says so |
| Panorama parallax coefficient | in code, never published ([D15](research-architecture.md)) | — | **Parameter** |
| Keyboard and focus | the phone had none | — | **Already resolved without the flag** — D19, [B-40](../backlog/B-40-keyboard-and-focus-on-desktop-and-wasm.md) |
| The eight Toolkit / platform controls | they existed | — | **Canon**, ungated, and a better use of the effort |

### 1.7 Nobody has asked, and nobody could have

| Fact | Where verified |
|---|---|
| The repository was created **2026-08-28**; **0 stars, 0 forks, 0 watchers, 6 unique viewers** in fourteen days | GitHub API — `created_at`, `traffic/views` |
| `0.1.0` reached `/snapshots` on 2026-08-30 | [CHANGELOG.md](../../CHANGELOG.md) |
| The API was cut before anybody used it, and that is recorded as a deliberate risk | [D20](research-architecture.md) |

**Consequence — an enhanced tier decided now is decided on taste alone.** That is not fatal: the
canon tier was also built from documents rather than from users, and it had to be, because the
documents are the specification. An *enhancement* has no specification — its only possible source of
truth is somebody wanting it. Building the enhanced tier first would be the one part of this project
with neither a source nor a consumer.

---

## 2. Consequences

1. **The flag is right and is not the bottleneck.** One row is a thin table, not a broken mechanism.
   D17's own argument — that its value is having somewhere for the next improvement to go — holds
   exactly as written.
2. **A "remastered version" as a separate tier is not what the request should buy.** It would need
   its own golden set (§1.2), its own numbers (§1.3), and it cannot contain the components the
   request also names (§1.4). Three mechanisms, one word.
3. **There is one improvement worth doing on the evidence, and it is hover** — a real deviation, on
   targets that ship, with 74 published values behind it, and it doubles as the first third of the
   desktop-lineage question. Whether it belongs behind the flag or behind the input mode is an open
   question with an address (§1.5), and D19 is the precedent that says the answer may be neither.
4. **The Pivot header crossfade is the second**, and it is the cleanest possible remastered row:
   the canon is explicitly instant, the alternative is Microsoft's own from the sibling lineage, and
   the difference is one number.
5. **The largest genuine gap is not enhancement at all** — it is the eight controls Windows Phone
   shipped and this library has not built. Ungated, sourced, and already surveyed.

**Recommendation, and it is a recommendation rather than a decision.** No remastered *tier*. Keep
the flag as the convention it is, and let it grow one row at a time under D17's rule — what changes,
what it replaces, where the canon was read. If one thing is done next in this direction, do hover,
and settle first whether the input mode gates it better than the flag does. And before **any**
second row lands, decide how the lane gets photographed, because today it is not.

---

## 3. Risks and open questions

**Risk 1 — the lane grows behind the gate.** Every remastered behaviour added today is invisible to
the screenshot suite (§1.2), and a green `check` will keep saying so. *Mitigation, concrete:* the
first remastered row that changes a pixel comes with a paired fixture — the same screen recorded
both ways — and `ScreenshotSuiteTest`'s registry check is what refuses a fixture without a golden.
Until that pairing exists, "behind `remastered`" means "untested by the suite", and that sentence
belongs in `CLAUDE.md` if the lane grows at all.

**Risk 2 — the flag empties out.** D17 warns of it in one direction (gating canon-restoring
behaviour). The other direction is worse and is not yet written down: gating a *number* would be
meaningless, and gating a *component* is impossible (§1.4). *Mitigation:* the three-mechanism table
in §1.4 is the test — before anything goes behind the flag, say which of the three it is.

**Risk 3 — the enhanced tier becomes the default by attrition.** Two targets out of four are
pointer-and-keyboard machines, and every improvement they want is a deviation on the phone. If
enough of them accumulate, `remastered = false` becomes the setting nobody runs, and the fidelity
claim survives only in the tests. *Mitigation:* the sample and the documentation site render canon,
and the golden suite photographs canon; both would have to be deliberately changed for the drift to
happen quietly.

**Open question A — does the input mode gate hover better than the flag?** §1.5. *Address:* the
first hover implementation, measured the way D19's ring was — assert the surface *is* hovered before
asserting nothing was drawn, or the test passes for a control that was never hoverable.

**Open question B — should `KvadrantTheme.remastered` stay public while nothing reads it?** It is in
the frozen ABI (§1.1) and a consumer can branch on it. That is arguably right — a consumer's own
components should be able to follow the theme's setting. It is also API with no in-library user, and
the honest thing is to say which of the two it is in its KDoc. *Address:* the next ABI change.

---

## Code anchors

| What | Path |
|---|---|
| The flag, and its one consumer | [`KvadrantTheme.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantTheme.kt) |
| The gated behaviour and its admitted number | [`TiltIndication.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt) |
| The two tests that are the whole lane | [`RemasteredTest.kt`](../../kvadrant-core/src/desktopTest/kotlin/io/github/youndie/kvadrant/behaviour/RemasteredTest.kt) |
| The canon that is explicitly instant | [`KvadrantPivot.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/components/KvadrantPivot.kt) |
| The precedent for gating on input rather than on taste | [`KvadrantFocusRing.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/KvadrantFocusRing.kt) |
| The frozen surface | [`kvadrant-core.api`](../../kvadrant-core/api/desktop/kvadrant-core.api) |
