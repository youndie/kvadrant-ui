---
id: B-34
title: "A documentation site with the components running in it"
status: open
priority: P1
size: L
stage: stage-2-release
blocked_by: [B-04]
---

# B-34 — A documentation site with the components running in it

A page per component: what it is, the numbers it was transcribed from, and **the component itself,
running**, built as wasm and served from GitHub Pages beside the API documentation.

**The reason this is not a nice-to-have.** Every claim this library makes is about how something
looks and moves, and today the only ways to check one are to clone the repository and run the demo,
or to look at a still. A still cannot show a tilt following a finger, a panorama wrapping, a dot
switching off when it lands, or a picker page tipping in from -50°; those are four of the things this
library exists to get right, and none of them survives a screenshot. A consumer deciding whether to
depend on this has nothing to press.

- **wasm, because the demo already exists.** `sample` is a Compose Multiplatform application and the
  same source can build a wasm target — the work is a target and a bundle, not a second demo. What
  it costs is honest to state: wasm is one of the two targets [D14](../research/research-architecture.md)
  is still waiting on, so this item adds a renderer that nothing currently guards
  ([B-29](B-29-android-screenshot-coverage.md) records what that means).
- **Rejected: a gallery of recorded GIFs.** Cheaper, and it rots silently — a GIF of a component that
  has since changed looks exactly like a GIF of one that has not. A running build is wrong only when
  the library is.
- **Rejected: embedding the demo as one page.** A component library's documentation is read one
  component at a time, by somebody who already knows which one they want. One page with everything on
  it is the demo, and the demo already exists.
- **The pages should carry the transcription, not a description.** Every component's KDoc already
  names the template it came from and the numbers it did not get from anywhere; that is the
  interesting half and it is what nobody else's Metro library can show. Dokka's HTML output is the
  obvious source for it rather than prose written twice.
- **The examples are the fixtures.** `kvadrant-core/src/desktopTest/.../demo/` already builds a
  screen per component for the golden suite. A page whose example is a *separate* snippet is a page
  that drifts from the tested one; the same composable should feed both, which means those fixtures
  have to move out of the test source set into something publishable.

## What it depends on

- **[B-04](B-04-repository-skeleton.md), and this is a hard block.** GitHub Pages publishes from a
  repository, and there is no remote — twenty-nine commits, all local, when B-04 was written and
  eighty now. Nothing about this item can be finished before that is answered, and the same account
  constraint applies: hosted minutes are not paid for, so whoever answers B-04 answers this too.
- ~~**A wasm target**, which is D14's second waiting target.~~ **Done.** `wasmJs { browser() }` on
  the core, the adapter and the sample; the demo builds to a browser bundle and renders — tiles,
  pivot, app bar, Cyrillic from `composeResources`, and a tile that leans towards a click. The entry
  point is eighteen lines and calls the same `KvadrantSampleApp` the other two demos do.

  Two things it did **not** buy, both worth knowing before this item is planned around them.
  `wasmJsBrowserTest` is *skipped* rather than run, because no browser runner is configured — so
  nothing executes on this target and a green `check` says only that it compiles. And viddik cannot
  photograph it for the same reason it cannot photograph Android, so the site's pages will be the
  first place anybody ever looks at wasm output.

## Acceptance

- AC: a page per public component, each with the component running in it, reachable from one index.
- AC: the example on a page and the fixture in the golden suite are the same composable, so a
  component that changes cannot leave a page showing the old one.
- AC: the site is built by `check` or by a job beside it, so a page that no longer compiles is a red
  build rather than a discovery.
- AC: the API documentation is generated rather than written, and the transcription notes reach it —
  a page that omits which Microsoft template a number came from is a page this project has no reason
  to publish.
- Anchors (to be created): `sample/src/wasmJsMain/`, `.github/workflows/`
