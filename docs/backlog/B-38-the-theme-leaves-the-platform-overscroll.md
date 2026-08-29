---
id: B-38
title: "The theme replaces the ripple and leaves the platform's overscroll"
status: open
priority: P0
size: M
stage: stage-2-release
blocked_by: []
---

# B-38 — The theme replaces the ripple and leaves the platform's overscroll

`KvadrantTheme` provides `LocalIndication` with the tilt, because a Metro surface that ripples is not
a Metro surface. It provides nothing for overscroll, so **on Android a Metro list still ends with
Android's stretch** — the same category of error, in the same theme, one line apart.

**Windows Phone compressed instead, and that is documented rather than remembered.** Microsoft added
`HorizontalCompression` and `VerticalCompression` visual state groups to `ScrollViewer` in Windows
Phone 7.1 so that applications could react to the end of a scroll. It is a behaviour a finger meets
every time a list runs out, which makes it about as frequent as the tilt and about as recognisable.

`grep` finds no `overscroll` anywhere in `kvadrant-core`.

## Why this is P0 and a component is not

- It is **not a component**. It is one factory in the theme, and it reaches every scrolling surface
  at once — the pivot's pages, the panorama, the long list, the picker page, the previews on the
  documentation site.
- Until it lands, every Android build of anything built with this library shows a Material behaviour
  at the end of every list, and the library's central claim is that it does not do that.
- The mechanism already exists at the pinned version: Compose Foundation 1.12.0 carries
  `OverscrollEffect`, `OverscrollFactory` and `LocalOverscrollFactory` — verified by unpacking
  `foundation-desktop-1.12.0.jar` rather than from documentation.

## What is known and what is not

**Known:** that it compressed, that Microsoft named the states, and that the effect is a compression
of the content rather than a glow or a stretch.

**Not known:** the distance, the curve and the release timing. Microsoft published the state groups,
not their storyboards. So the numbers are this project's own, they ship as parameters of the public
API rather than as constants, and they are named as ours in KDoc — the same rule §1.10 already
applies to the panorama's peek and the settle.

**Do not tune it until the demo looks right.** That is B-26's mistake in a different component: a
constant fitted to one screen at one window size is unre-derivable a week later. Establish the shape
against a source first — the visual state groups say *what* changes, and the WinJS animation library
is the nearest published relative for *how fast*.

## Acceptance

- AC: a scrolling surface under `KvadrantTheme` compresses at its end on every target, and the
  platform's own overscroll is gone — asserted by a test that scrolls past the end and measures the
  content's displacement, not by looking at a still.
- AC: the theme installs it, so a consumer gets it without asking, exactly as they get the tilt.
- AC: the distance and timing are parameters with KDoc naming them as this project's, because
  Microsoft published neither.
- AC: research §1.10's list of unpublished numbers gains these two.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantTheme.kt`
