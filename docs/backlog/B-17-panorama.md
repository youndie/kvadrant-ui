---
id: B-17
title: "Panorama with background parallax"
status: done
priority: P1
size: L
stage: stage-3-completeness
---

# B-17 — Panorama with background parallax

**A first version exists** — `kvadrant-core/src/commonMain/kotlin/.../KvadrantPanorama.kt`,
rendered in `gallery_panorama.png`: the 170 px Light title, 66 px SemiLight section headers, both at
−35/1000 em of tracking, and the parallax.

**The title's rate is derived, not chosen.** A layer wider than the viewport has to finish crossing
its own overflow exactly when the content finishes crossing its own, so its rate is one overflow
divided by the other. It holds at any number of sections and there is nothing to tune — the same
move as the Pivot's headers.

**The background's rate is not derivable and is therefore a parameter.** The original's background
is an image whose width the application picks and whose coefficient Microsoft never published, so
`DEFAULT_BACKGROUND_RATE = 0.35` is this project's invention. It is a parameter of the composable
rather than a constant inside it, which is the rule D15 left behind: an invented number ships where
someone who measures the real thing can replace it without a fork.

**Wrap-around is in, and the reason for skipping it expired.** It was left out on the grounds that a
still cannot show whether a wrap is seamless — true of screenshots, and untrue the moment
`runComposeUiTest` arrived for something else. The sections are laid out twice and the scroll folds
back by one copy's width on crossing into the second, which is what the original's `LeftWraparound`
and `RightWraparound` borders do. `PanoramaWrapTest` drives it and **was verified by removing the
fold**, which turns it red.

Two things came out of writing that test:

- the scrolling row carried `fillMaxWidth()`, which caps a horizontally scrolling row at the
  viewport. The panorama had been scrolling a few pixels where it should have scrolled pages;
- the first version of the test used 200 dp sections, which do not overflow a desktop test window
  at all — so there was nothing to wrap and the test would have passed for that reason. A panorama
  narrower than the screen is the one case where the wrap is correctly absent.

**Left:** the background layer is a slot with nothing in it yet.

Up to five sections on a continuous horizontal surface, a background image that moves at a different
rate from the content, wrap-around, and a 72 px SemiLight panorama title.

- **The template has been recovered** ([research §1.11](../research/research-architecture.md)), and
  it corrects the brief: the panorama title is **170 px `PhoneFontFamilyLight`, tracking −35, margin
  10,-34,0,0**; the **section header** is 66 px SemiLight with the same tracking. Building this from
  the brief's "72 px SemiLight" would have produced a title less than half the size it should be, in
  the one component whose point is a title too big for the screen.
- The mechanism is recovered too: the section header carries its own `TranslateTransform`, so the
  parallax is a translate on the header rather than on its container; wrap-around is a horizontal
  `StackPanel` with `LeftWraparound`/`RightWraparound` borders under a pair of translates.
- **Still not a number:** the parallax *coefficient*. The template gives the transform, not the rate
  it is driven at, and that lives in code rather than in the theme.
- The parallax coefficient is the one number worth measuring off a screen recording rather than
  guessing, because it is the whole effect: too little and the background looks pinned, too much and
  it looks like a bug.
- Not covered: the Win8 `Hub`, which is the same idea with different metrics and belongs to
  [B-22](B-22-win8-branch.md).

- AC: five sections, wrap-around, and a background that visibly lags the content.
- AC: the parallax coefficient and its provenance — measured or invented — are recorded in KDoc.
  The type sizes and tracking are transcription and do not need that note.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/components/KvadrantPanorama.kt`
