---
id: B-22
title: "Is the Windows 8 profile part of this library or a separate one?"
status: done
priority: P3
size: XL
stage: stage-4-win8
---

# B-22 — Is the Windows 8 profile part of this library or a separate one?

**Answered: separate.** Which is what [D16](../research/research-architecture.md) had already argued
for on its own evidence — Windows 8 → 8.1 WinRT XAML → UWP is the *desktop* iteration of Metro and
this library is the *phone* one, so a Win8 profile is not a variant of what is being built here but
the other lineage. [B-26](B-26-per-layer-camera-versus-a-global-one.md)'s reading of the 8.1 button
brushes pointed the same way: the light-theme WinRT button is filled at rest where the phone's is
transparent, which is that lineage softening a rule rather than restating it.

Nothing is scheduled for it. **A desktop profile is a conversation to have later**, on its own terms,
rather than a milestone in this backlog — and this item exists now only so that the next person to
ask gets an answer instead of the question.

The Win8 type ramp, the 20 px baseline grid, the 120/100 px silhouette, `Hub`, `AppBar`/`CommandBar`,
`SemanticZoom` and the settings flyout — a second design profile that shares a name with the first
and disagrees with it.

- **Windows Phone and Windows 8 conflict on things as basic as the order of buttons in a dialog**
  ([research §1.3](../research/research-architecture.md)). They cannot share one theme without one of
  them being wrong, which is the argument for a separate `kvadrant-win8` module rather than flags
  inside the existing components.
- The working hypothesis, and the reason this is a question rather than a plan: the Win8 **tokens**
  (type ramp, grid, silhouette metrics) go into the core from the start, because they are already
  transcribed and cost nothing to carry; the Win8 **components** do not exist until a desktop
  consumer asks for them.
- `SemanticZoom` in particular is an XL on its own, and its transition duration is one of the eight
  unclosed gaps in the specification.
- **This branch already has a numeric source, acquired by accident.** The WinJS and UWP Pivot
  metrics in [research §1.11](../research/research-architecture.md) were gathered while hunting for
  the phone's numbers and turned out to belong to the desktop lineage — Windows 8.1 HTML and the
  XAML line that descends from Windows 8. They are wrong for the phone and right for here: a 48 px
  header strip, 24 px SemiLight headers, a 2 px accent underline, and the full switch animation
  (67 ms out, 333 ms opacity and 767 ms slide in, on `cubic-bezier(0.1, 0.9, 0.2, 1)`).

- **Continuum belongs here too, and only here.** Looking for the phone's version turned up nothing
  in three artefacts — it was a shell transition, never exposed — while WinJS's is complete: four
  directions, each three independent curves, two of them deliberately overshooting so the item
  passes its resting place and returns. The full table is in
  [research §1.11](../research/research-architecture.md).

**Amendment, 2026-08-30 — "a numeric source" understates it by an order of magnitude, and the
understatement was load-bearing.** The bullet above, and
[research §1.11](../research/research-architecture.md) with it, left this branch looking like one
that would have to transcribe its own specification before it could start. It would not.
[research-desktop-lineage §1.2](../research/research-desktop-lineage.md) went and looked:

- the archived `dn518235` enumerates **353 `*ThemeBrush` keys**, each with a value in Default
  (dark), Light and HighContrast — 101 distinct literal hex values — and is scoped verbatim to
  "Windows 8.x and Windows Phone 8.x developers writing Windows Runtime apps";
- it names its own origin, `themeresources.xaml` in `include/winrt/xaml/design` of a Windows SDK
  install, "also reproduced in generic.xaml in the same directory" — so the templates and
  thicknesses are one extraction away, on a path this project has walked once already for the
  phone (research §1.12);
- the **Win8 type ramp this item lists first is not untranscribed either**: the six styles in
  `metro-tokens.json` match the Microsoft reference on every number it states, so they are
  corroborated rather than inherited;
- `metrics.windows8` in the same file already carries **28 numbers** — the 20 px grid and its 5 px
  sub-unit, the 120 px left margin, the 100 px header baseline, the four scaling plateaus, the
  snapped view, the app-bar and flyout sizes.

**So the shape of the decision changes.** This item's working hypothesis — tokens into the core
now, components when a consumer asks — was written on the assumption that the tokens were the
cheap half because they were "already transcribed". They are cheaper than that: most of the colour
half was never transcribed at all and is simply public. What the amendment does **not** change is
the conclusion, and the reason has moved from the specification to the code: the tilt does not
cross the lineage (Microsoft's own wording has the same named animation shrink on Windows and tilt
on the phone), so roughly 1,100 of 7,510 lines of `commonMain` transfer and the components
transfer at approximately zero. Separate stays separate; it is now separate for a better reason.

The decision itself is open in [#41](https://github.com/youndie/kvadrant-ui/issues/41), and two
questions ride on the same extraction: whether WinRT's `ScrollViewer` compressed at the ends, and
whether `ControlContentThemeFontSize` is really 14.667 px.

- AC: a decision — tokens only, full profile, or separate library — recorded in
  [research §3](../research/research-architecture.md) under open question 1, with what made the call.
