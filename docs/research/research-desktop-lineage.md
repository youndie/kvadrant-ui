---
id: research-desktop-lineage
title: "Is there a desktop profile on the Windows 8.1 lineage?"
type: research
status: active
date: 2026-08-30
---

# Is there a desktop profile on the Windows 8.1 lineage?

The question asked: a version for the desktop that leans on **Windows RT / Windows 8.1** Metro
rather than on the phone. [Open question 1](research-architecture.md) has carried a placeholder
answer since before there was code — *tokens in the core, components when a desktop consumer
appears* — and it deferred the decision to an event. **The event has not happened** (§1.6), so this
research cannot settle the question on demand. What it can settle, and does, is the two things that
were being guessed at instead: **what the profile would consist of**, and **what it would cost**.

Both came out differently from the standing assumption. The specification is in far better shape
than [B-22](../backlog/B-22-win8-branch.md) recorded — the colour half is public and complete, which
nobody had checked. The code reuse is far worse — the component that defines this library does not
transfer, and Microsoft says so in one sentence.

Nothing here is scheduled and no backlog item is opened: this is research, and the decision is not
mine.

---

## 1. Verified facts

### 1.1 The desktop-Metro audience is real, is served by skins, and has never had a toolkit

| Fact | Where verified |
|---|---|
| `compose-fluent/compose-fluent-ui` — **728★, Apache-2.0, last push 2026-08-18** — is alive and is the occupant of "Windows desktop look for Compose" | GitHub API, `repos/compose-fluent/compose-fluent-ui` |
| A GitHub search for Windows 8 Metro projects returns **54** repositories; the two largest are `aozora/bootmetro` (2172★, last push **2020-10-01**) and `peters/winforms-modernui` (808★, **2019-12-11**) | GitHub search API, `metro ui windows 8 in:name,description`, sorted by stars |
| Every one of the top twelve is web, WinForms or WPF. **None is Compose, and none is a Kotlin project at all** | same listing |
| The only one with commits this year is `RoseTheFlower/MetroSteam` (279★, created 2018-12-01, last push **2026-08-08**) — a Steam **skin** | GitHub API |
| The phone lineage's one precedent is unchanged: `louis993546/Metro-Compose`, **9★, MIT, Android-only**, last push 2026-08-28 | GitHub API — re-verified, [§1.1](research-architecture.md) |

**Consequence — the niche is empty in the same sense it was for the phone, and that cuts both
ways.** There is nothing to compete with, and there is also no evidence anyone has tried to build
this and found an audience. What the audience has demonstrably wanted for a decade is *skins*: a way
to make an existing application look like Windows 8, not a way to build a new one. A component
library serves the second want, and the second want has no visible constituency.

**Consequence — `compose-fluent-ui` is not a competitor and is not a precedent either.** It is the
*successor* design language, which is the road [D16](research-architecture.md) says the WinRT lineage
ends on. A Windows 8.1 profile arrives one stop before it, and the argument for stopping there is an
aesthetic one, not a technical one.

### 1.2 The colour half of the specification is public, complete, and nobody had looked

This is the finding that most changes the picture. The sentence it refutes is in
[research-architecture §1.11](research-architecture.md) — "that branch has no numeric source of its
own yet" — and [B-22](../backlog/B-22-win8-branch.md) carries the same understatement in its own
words, crediting the branch with one source picked up by accident. Both are out of date, and both
are amended.

| Fact | Where verified |
|---|---|
| The archived reference is scoped verbatim "for **Windows 8.x and Windows Phone 8.x** developers writing Windows Runtime apps" | `learn.microsoft.com/.../dn518235(v=win.10)`, line 46 of the fetched page |
| It enumerates **353** distinct `*ThemeBrush` keys, each with a value in **Default (dark)**, **Light** and **HighContrast** | counted over the fetched page: `grep -o '[A-Za-z]*ThemeBrush' \| sort -u \| wc -l` |
| **101** distinct literal hex values appear in it; the HighContrast column forwards to eight `SystemColor*` keys instead | same, `grep -oE '#[0-9A-Fa-f]{6,8}'` |
| Of those brushes, **74 are `PointerOver`**, **64 are `Pressed`**, **12 are focus** | same, counted per family |
| The largest families are Toggle (41), Combo (35), List (33), Slider (22), AppBar (21), Search (20) | same |
| It names its own origin: `themeresources.xaml` in `include/winrt/xaml/design` of a Windows SDK install, "also reproduced in generic.xaml in the same directory" | same page, §"Theme resources and where they fit" |

**The six Windows 8 text styles corroborate the vendored brief exactly.** The brief's
`typography.windows8.styles` block was transcribed from an unnamed source; every number in it that
the Microsoft reference states, the reference states identically:

| Style | Brief | `dn518235` |
|---|---|---|
| header | 56 px, Light, line height 40 | `FontSize 56`, `FontWeight Light`, `LineHeight 40` |
| subheader | 26.667 px, Light, line height 30 | `FontSize 26.667`, `FontWeight Light`, `LineHeight 30` |
| title | SemiBold | `FontWeight SemiBold` |
| subtitle | Normal | `FontWeight Normal` |
| body | SemiLight | `FontWeight SemiLight` |
| caption | 12 px, Normal | `FontSize 12`, `FontWeight Normal` |
| `StylisticSet20`, `DiscretionaryLigatures`, `CaseSensitiveForms` | all three | all three, on `BaseTextBlockStyle` |

**An inherited claim, re-checked rather than carried.** [D16](research-architecture.md) asserts that
"the word *accent* does not appear anywhere in that reference". A case-insensitive search of the
whole 56 KB page returns **0 matches**. The claim holds, and now says so from a count rather than
from a reading.

**What is still not published, and where to get it.** The reference lists the brushes and the text
styles; it does **not** list `ControlContentThemeFontSize` (the base the ramp's middle four inherit —
the brief says 14.667 px / 11 pt), the thicknesses, or any control template. All of those are in
`themeresources.xaml` and `generic.xaml` in a Windows 8.1 SDK installation, at the path the page
itself names. *Hypothesis, with an address:* the 8.1 SDK's copy of those two files closes the
remaining gaps in one extraction, the same way the phone SDK closed the phone's
([§1.11](research-architecture.md), §1.12). Check before any Win8 token is written down.

**Consequence — the desktop profile would begin with more published specification than the phone
one ever had.** Colours: 353 sourced brushes against the phone's thirteen plus twenty accents.
Metrics: the `metrics.windows8` block in `reference/metro-compose-brief/references/metro-tokens.json`
already carries **28 numbers** — 20 px grid and 5 px sub-unit, 120 px left margin, 100 px header
baseline, 140 px content top, 80 px group gap, the four scaling plateaus 80/100/140/180, snapped
320 px, the 8.1 minimum window of 500 px, app-bar heights, settings-pane and flyout widths. Motion:
WinJS, MIT, every curve in the open ([§1.11](research-architecture.md)). The specification is not
the risk here. The code is.

### 1.3 The press is not the tilt, and Microsoft says so in one sentence

| Fact | Where verified |
|---|---|
| "On Windows, the animation slightly shrinks the item to indicate that it is pressed; on Windows Phone, the animation tilts the item slightly around the positive y-axis in a 2.5D effect." | `learn.microsoft.com/.../jj649432(v=win.10)`, *Animating pointer actions (XAML)* — the 8.x-era page, verbatim |
| The desktop shrink, with its numbers: `scale(0.975, 0.975)`, **167 ms**, `cubic-bezier(0.1, 0.9, 0.2, 1)`; `pointerUp` reverses it over the same duration and curve | `winjs/winjs`, `src/js/WinJS/Animations.js`, `pointerDown` / `pointerUp` — MIT, verified from `License.txt` |
| `ListViewItem` and `GridViewItem` use it for `Pressed` and `PointerOverPressed`; on the phone `Button` used it by default | `jj649432(v=win.10)`, §"Pointer animations in default Windows Runtime control behavior" |

**Consequence — the thing this library is known for does not cross the lineage.** `TiltIndication`,
the shared camera, the homography and the whole geometry cluster are **1,109 lines of
`indication/` plus most of `foundation/`**, and they are the answer to a question the desktop
lineage answers with a uniform scale. What transfers is the *harness* — an `IndicationNodeFactory`
provided through `LocalIndication` so no control can forget it — not the effect inside it. That is
a real reuse, and it is a shape rather than a body.

**Consequence — the replacement is transcription, not invention.** 0.975 over 167 ms on a published
curve is a complete specification for the desktop press. A Win8 profile would not need this
project's own number for the one thing the phone profile needed four spikes to settle.

### 1.4 The two layout languages differ in kind, not in size

| Fact | Where verified |
|---|---|
| "a Metro layout has no breakpoints: Windows Phone stretched one 480-pixel canvas to WVGA, WXGA and 720p rather than reflowing anything" | KDoc of `KvadrantMetrics.scaledToWidth`, `kvadrant-core/.../theme/KvadrantMetrics.kt` |
| The desktop demo opens at `rememberWindowState(width = 560.dp, height = 860.dp)` and fits the metric set to that width | `sample/src/desktopMain/.../Main.kt:14`, `sample/src/commonMain/.../SampleApp.kt:165` |
| Windows 8 scales in **four plateaus** (80/100/140/180 %) and has a **snapped** view at 320 px and an 8.1 minimum window of 500 px | `metro-tokens.json`, `metrics.windows8` |
| Windows 8 has a **20 px baseline grid** with a 5 px sub-unit, a 120 px left margin and a 100 px header baseline | same |
| No Win8 tile silhouette is vendored — `tiles` carries `windowsPhone8` only | same file, `tiles` |

**Consequence — the desktop profile is not "the phone, bigger".** Today the desktop target is
literally a phone in a window, scaled by a fitted factor, and it works because the phone's own
answer to a bigger screen was to scale. The desktop lineage's answer was the opposite: plateaus,
a snapped view, a grid, and horizontal panning across a canvas wider than the screen. A profile
that scales the existing components to 1920 px produces very large phone controls, which is the
failure mode worth naming out loud, because it is the cheap thing to build and it looks like
progress for about a day.

**Consequence — and the one place the lineages agree is worth noting.** Both are flat, both are
rectangular, both invert on press rather than shading, and the *light* WinRT button is the single
recorded softening ([D16](research-architecture.md)). The disagreement is structural, not aesthetic:
input model, scaling model and navigation model, three for three.

### 1.5 What would actually be reused, measured rather than felt

`kvadrant-core/src/commonMain` is **7,510 lines** across five packages.

| Package | Lines | Share | Transfers to a Win8 profile? |
|---|---|---|---|
| `components/` (21 files) | 4,132 | 55 % | **Essentially none.** Pivot differs (48 px strip, 24 px SemiLight, accent underline — [§1.11](research-architecture.md)); Panorama is **deprecated** in that lineage ([D16](research-architecture.md)); ListPicker, LoopingSelector, LongList, AppBar and the tiles are phone controls with phone metrics |
| `indication/` (5 files) | 1,109 | 15 % | **Shape only** — §1.3. The factory and the focus ring survive; the tilt, the camera and the perspective do not |
| `theme/` (7 files) | 1,055 | 14 % | **Shape only.** `@Immutable` tokens behind a static local is the right structure; every value is re-transcribed from a different dictionary |
| `icons/` (2 files) | 833 | 11 % | **Yes, whole.** Segoe UI Symbol is one glyph set across both lineages, and `KvadrantIconBuilder` draws rather than ships a face |
| `foundation/` (4 files) | 381 | 5 % | **Split.** `KvadrantText` and `KvadrantFonts` transfer; `KvadrantCamera` and `KvadrantHomography` are the tilt's geometry |

**Consequence — roughly 1,000–1,200 lines transfer unchanged, and about 4,000 do not.** The
honest description of a Windows 8.1 profile is *a second library that shares a font stack, an icon
set, a build and a test harness* — which is what [B-22](../backlog/B-22-win8-branch.md) concluded
from the design side, now with the code side agreeing. The shared half is real and it is
infrastructure: the Gradle setup, the golden suite, `portableTypography`, the ABI dump, the
documentation gate. That is not nothing; it is also not a head start on the components.

### 1.6 The event that was supposed to settle this has not occurred

| Fact | Where verified |
|---|---|
| `youndie/kvadrant-ui` was created **2026-08-28** — two days ago | GitHub API, `created_at` |
| **0 stars, 0 forks, 0 watchers**; **6 unique** page viewers over fourteen days | GitHub API, `repos/.../traffic/views` |
| 565 clones from 100 unique cloners in the same window — a figure dominated by CI | `repos/.../traffic/clones`, read against a two-day-old repository |
| One open issue, and it is our own build task | `gh issue list` |
| `0.1.0` was published to `/snapshots` on 2026-08-30 | [CHANGELOG.md](../../CHANGELOG.md), [B-46](../backlog/B-46-the-first-release.md) |

**Consequence — "settled when the first desktop consumer appears" is still the right rule and it is
still unsatisfied.** [D20](research-architecture.md) already records that `0.1.0` was cut before
anybody used the API. Deciding a second lineage on the same absence of evidence would repeat that,
one order of magnitude more expensively.

---

## 2. Consequences

1. **The specification question is closed and the answer is favourable.** A Windows 8.1 profile has
   a public colour dictionary (353 brushes, three themes), a vendored metric block (28 numbers), a
   corroborated type ramp (six styles), and MIT-licensed motion. What remains unread is one SDK
   extraction with a named path. **[§1.11](research-architecture.md) and
   [B-22](../backlog/B-22-win8-branch.md) are amended accordingly** — this is now the better-sourced
   of the two lineages.
2. **The code question is closed and the answer is unfavourable.** About 15 % of the core transfers
   unchanged, and the 55 % that is components transfers at approximately zero. This is a second
   library, not a profile — which is the conclusion B-22 reached on design grounds, now measured.
3. **The lineage disagrees on input, not only on looks**, and that is the single most expensive
   line in this document. 74 `PointerOver` brushes against a library with **no hover concept at
   all** (see [research-remastered-lane.md](research-remastered-lane.md) §1.5). A desktop profile is
   a second interaction model before it is a second palette.
4. **The demand question is open and unmeasurable today**, at 0 stars and two days of existence.

**Recommendation, and it is a recommendation rather than a decision.** Not now, and not as a
module. The cheap, honest, reversible half is worth taking on its own: **carry the Windows 8.1
tokens** — colours and the corroborated type ramp — into `references/` beside the phone's, marked as
the other lineage's, unused by any component. That is the hypothesis B-22 already stated, it costs
one extraction, it makes the *next* answer cheap, and it commits nothing. The expensive half —
components, a second interaction model, a second golden set — should wait for the consumer the
architecture research already named as the trigger.

**What would change this answer, concretely:** a consumer asking for it; or the phone library
acquiring hover and keyboard behaviour for its own desktop targets, at which point the interaction
gap in Consequence 3 closes for free and the remaining cost is palette and layout.

---

## 3. Risks and open questions

**Risk 1 — the profile gets built as a scale factor.** The cheap version is "run the existing
components at 1920 px", it can be demonstrated in an afternoon, and §1.4 says it is the wrong thing:
Windows 8 has plateaus and a snapped view, not one stretched canvas. *Mitigation:* the first thing
any Win8 work builds is the **20 px grid and the plateau set**, before a single component, so the
layout model is in place before anything can be scaled into it.

**Risk 2 — two design systems in one repository, one gate.** The golden suite is calibrated to one
rasteriser and one type ramp ([B-35](../backlog/B-35-cyrillic-renders-differently-on-linux.md), `make check`). A second
profile doubles the golden set and adds a second calibration. *Mitigation:* if it happens at all, it
is a separate module with its own snapshot directory, and `ScreenshotSuiteTest`'s registry check is
per-module already.

**Risk 3 — the archived reference disappears.** Everything in §1.2 rests on one `learn.microsoft.com`
archive page marked `is_archived: true`, `ROBOTS: NOINDEX`. *Mitigation:* it is a table, it is 56 KB,
and the fix is the same one the phone tokens got — extract it into a vendored JSON under
`reference/` at the moment it is first used, with the SDK path recorded beside it.

**Open question A — does the desktop lineage compress at the ends of a scroll, as the phone did?**
The phone's `HorizontalCompression`/`VerticalCompression` visual states are documented
([research-component-coverage.md](research-component-coverage.md) §3.1) and drive
`KvadrantOverscroll`. Whether WinRT XAML's `ScrollViewer` did the same, and with what numbers, is
unread. *Address:* `generic.xaml` in the 8.1 SDK, in the same extraction as §1.2's remaining gaps.

**Open question B — is `ControlContentThemeFontSize` 14.667 px?** Three of the six text styles
inherit their size from it and the reference does not state it; the brief says 14.667 (11 pt).
*Address:* `themeresources.xaml`, same extraction. Until then, half the Win8 ramp is one unverified
number.

---

## Code anchors

| What | Path |
|---|---|
| The metric set, its scale, and the "no breakpoints" argument | [`KvadrantMetrics.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantMetrics.kt) |
| The press this lineage would replace | [`TiltIndication.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/indication/TiltIndication.kt) |
| The geometry that does not transfer | [`KvadrantCamera.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/foundation/KvadrantCamera.kt), [`KvadrantHomography.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/foundation/KvadrantHomography.kt) |
| The layer that transfers whole | [`KvadrantIconBuilder.kt`](../../kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/icons/KvadrantIconBuilder.kt) |
| The desktop demo that is a phone in a window | [`Main.kt`](../../sample/src/desktopMain/kotlin/io/github/youndie/kvadrant/sample/Main.kt) |
| The vendored Win8 metric and type blocks | [`metro-tokens.json`](../../reference/metro-compose-brief/references/metro-tokens.json) |
