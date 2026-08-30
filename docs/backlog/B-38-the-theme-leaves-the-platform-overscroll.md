---
id: B-38
title: "The theme replaces the ripple and leaves the platform's overscroll"
status: done
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

## Done

`KvadrantOverscroll` compresses the content towards the edge the finger is pushing against, and
`KvadrantTheme` provides it through `LocalOverscrollFactory` — one line below the one that provides
the tilt, which was the whole argument.

**Measured, because the two candidate behaviours look alike in a still and are opposites in a
gesture.** A translation — iOS's rubber band, and what an easier implementation produces — slides the
content away from the boundary and leaves a gap. A compression keeps the boundary where it is and
squeezes what is behind it. So `OverscrollCompressionTest` asserts *which edge moved*: at the bottom
of a list the band boundary comes down by 18 px of a 300 px viewport — exactly the 6 % the parameter
says — while the bottom edge stays at 299. Removing the factory from the theme fails it; that was
checked.

Three numbers are this project's own and none is a constant: `DEFAULT_MAX_OFFSET`,
`DEFAULT_RESISTANCE` and `RELEASE_MILLIS`, each a parameter with KDoc naming it as ours. Microsoft
published the visual state groups and none of their storyboards. They are on research §1.10's list
now, beside the panorama's peek.

There is a preview, `overscroll`, because this is the kind of thing the documentation site exists
for: it cannot be understood from a still, only by dragging one.

### Three wrong turns, all caught by measurement rather than by review

**The state was an `Animatable`.** A drag is synchronous — `applyToScroll` is called and the next
frame is drawn — while `snapTo` suspends, so updating through `scope.launch` meant the draw pass read
the value from before the gesture. Nothing compressed at all. Animation belongs only to the release,
which genuinely is a coroutine.

**The pivot was on the wrong edge.** The sign belongs to the finger, not to the boundary: a finger
travelling *up* at the end of a list leaves a negative delta and is pushing against the **bottom**.
Before the fix the measurement showed the bottom edge rising and the top staying — a list compressing
away from the boundary it was resting on, which is precisely backwards.

**And the first fixture could not have shown either of those.** It scrolled one solid white block
taller than the viewport, where whatever is squeezed away at the far edge is replaced by more of the
same colour. It reported no change from an effect that was working. Bands fixed it, and the reason is
written in the test.

## Amendment — it was a squeeze, and the phone slid

Asked directly, months later: *are you sure the content compressed rather than moved?* It did not.
The whole implementation rested on Microsoft's name for the visual states —
`HorizontalCompression`, `VerticalCompression` — and the design guidelines describe the behaviour:

> When the end of the list is reached, it will then **scroll up to display the empty section** and
> "rubber band" back to rest in place. Flicking at the end of the list causes it to rubber band
> back; the list won't wrap to the beginning. — `jj735577`

An empty section is exactly what a squeeze cannot produce. "Compression" names the damping of the
**manipulation**, which is also why those visual states carry no storyboard: they exist so an
application can notice it, not draw it.

The effect is a damped translation now. The class keeps its name because this item, B-45 and research
all cite it; the parameters do not — `maxOffset` setting a translation distance is a claim in a
signature — so it is `maxOffset`.

**This item's own test was written to exclude the right answer**, in as many words: it failed a run
where "the content is sliding away from the boundary rather than squeezing towards it — that is a
rubber band". A guard is only as good as the reading behind it, and that reading was a name.

## Acceptance

- ~~AC: a scrolling surface compresses at its end, asserted by measurement rather than by a still.~~
  Done — `OverscrollCompressionTest`. Desktop only, for B-29's reason; the on-device check belongs
  with whatever next runs there.
- ~~AC: the theme installs it.~~ Done, in `KvadrantTheme`.
- ~~AC: the distance and timing are parameters named as ours.~~ Done — three of them.
- ~~AC: research §1.10's list of unpublished numbers gains these.~~ Done — three, not two.
- Anchors: `kvadrant-core/src/commonMain/kotlin/io/github/youndie/kvadrant/theme/KvadrantTheme.kt`
