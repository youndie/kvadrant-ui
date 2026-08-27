---
id: B-02
title: "Recover the Pivot metrics Microsoft never published"
status: done
priority: P0
size: M
stage: stage-0-spikes
---

# B-02 — Recover the Pivot metrics Microsoft never published

**Done.** They were never published, but they were shipped: the WP8 SDK's design-time
`Microsoft.Phone.dll` carries the control templates as plain XAML, and `ThemeResources.xaml` carries
the values they reference. Both came out of the offline SDK ISO preserved in the Internet Archive
(`wpsdk8`), Microsoft's own download having gone 404. The templates were confirmed by diffing them
against an independently obtained copy — identical, 18,572 characters.

| | |
|---|---|
| header | **72 px**, `PhoneFontFamilySemiLight` — the ramp's largest step |
| header padding | **21,0,1,0** — the 21 px is the gap between headers |
| title | **22.667 px**, `PhoneFontFamilySemiBold`, margin **24,17,0,-7** |
| item margin | **12,28,12,0** |
| unselected | opacity **0.4**; selection is instant (`Duration="0"`) |
| peek | not a number — headers sit on a `Canvas`, it falls out of the scroll position |

Full table with addresses, plus the Panorama corrections that came with it, in
[research §1.11](../research/research-architecture.md). The multiplier D5 was derived from three
checks turns out to be annotated in the source seventeen times.

- **Time-boxed, and the fallback is a decision rather than a delay.** Four routes in order of cost:
  decompile `Microsoft.Phone.dll` from the WP8 SDK and read the `Pivot`/`PivotItem` control
  templates; "Edit a Copy" in Blend on a machine with that SDK installed; frame-by-frame analysis of
  a screen recording from a device or emulator; the WinJS `Pivot` sources, which give semantics but
  not metrics.
- The rejected option is inventing the numbers quietly and letting them look like specification.
  If they are ours, they are named as ours — in KDoc, and as parameters of the public API so a
  consumer who knows better can pass their own.
- Not covered: the parallax of Panorama, which is a different component with its own gap
  ([B-17](B-17-panorama.md)).

- AC: either a table of recovered numbers with the artefact each came from, or a written statement
  that the routes were tried and failed, plus the interpreted values this project will ship.
  *Both, in the end — see research §1.11 for what came back and what did not.*
- AC: whichever way it lands, [research §1.10](../research/research-architecture.md) is amended —
  the gap is closed or it is confirmed as permanent. *Done: §1.10 now points at §1.11, which
  separates what is WP8 from what is WinJS.*
- AC: the four symbolic resources get their values. **Done** — they were in
  `WPDT_DESIGN_THEMERESOURCES_XAML`, not in `System.Windows.dll` as expected.
- AC: the templates are **diffed** against the SDK's own copy. **Done and identical**; the citation
  now points at the SDK and the provenance question is closed.
- Not a route: the WP8.1 **WinRT XAML** half of the SDK. It is a different design stack, already
  documented here as drift.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/components/KvadrantPivot.kt`
