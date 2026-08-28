---
id: B-04
title: "Skeleton: modules, targets, version catalog, CI, snapshot publishing"
status: wip
priority: P0
size: M
stage: stage-1-core
---

# B-04 — Skeleton: modules, targets, version catalog, CI, snapshot publishing

**Audited. Two of the five criteria below belong to other items, one is unmet, and it is the one
that matters most for a library.**

**There is no binary-compatibility validation of any kind** — no `abiValidation`, no
`binary-compatibility-validator`, nothing in `.github/workflows/check.yaml`. Every public symbol
here is currently unpinned: a parameter renamed, a `val` turned into a function, a default removed
are all invisible to `./gradlew check` and all break a consumer at link time. This library has
already changed public signatures twice without noticing — `TiltIndication.cameraDistance` went from
`Float` to `Dp` under B-25, and `kvadrantLatin()`/`kvadrantCyrillic()` became `@Composable` under
B-07. Both were right; neither was *declared*, and with a dump in the tree both would have arrived
as a diff a reviewer had to approve.

**There is no CI, and that is the finding.** The workflow ran `make check` and nothing else — the
tests, ktlint, the screenshot suite, the ABI dump and `noMaterialInTheCore` were never in it, so
every "the gate is green" in this repository means *green on one laptop*. A `gradle` job is added
now, and it has still never executed, because **the repository has no remote**: twenty-eight commits,
all local. Until there is one, the workflow file is a plan.

Two things stand between it and working, and both are worth knowing before it is trusted: Java 25
must be available from `setup-java`, and the screenshot suite renders in a real Skiko window, which
needs a runner that can give it one. Neither is checked. There is also a known account constraint —
GitHub-hosted minutes are not paid for — so the runner question may not be answered by pushing.

**The workflow is closer than it was and still unproven.** The `gradle` job now declares a Python —
`check` shells out to it twice for the token and icon generators, and an undeclared interpreter is a
dependency that changes without a diff — and wraps the build in `xvfb-run`, because a runner has no
display server. **That `xvfb-run` line is the one to distrust**, and not only because it is untested:
a headless capture can succeed and produce a frame with nothing in it, so the first green run of this
job proves less than it appears to. Look at a golden by eye the first time it passes.

**An attempt to settle the display question locally failed, and the failure is worth recording.**
`./gradlew … -Djava.awt.headless=true` sets the property on the Gradle *daemon*, not on the forked
test JVM, so the suite that came back byte-identical had not been run headless at all. Settling it
properly needs Linux without a display — the machine in `CLAUDE.md` would do it, and was unreachable
when this was written.

**What is actually blocking, stated plainly:** a git remote, and a decision about where CI runs given
that hosted minutes are not paid for. Both are the repository owner's, not this item's.

**Done.** Kotlin's own `abiValidation` is on and `checkKotlinAbi` was already wired into `check` by
the plugin — verified by adding a public function and watching the gate print the added line, not by
reading that it should. The reference dump is `kvadrant-core/api/desktop/kvadrant-core.api`, 448
lines, and it holds `kvadrantLatin (Landroidx/compose/runtime/Composer;I)` — which is exactly the
signature change B-07 made in silence.

**Named limitation:** the dump covers the desktop JVM ABI only. Nothing is unguarded by that today
because there is no `androidMain` source at all — the Android target compiles `commonMain` and
nothing else — but the first `actual` written there will be outside the dump, and this paragraph is
what should stop that going unnoticed.

**The (a)/(b) question is answered**, by rendering, and the answer inverts the brief: both graphs
draw and both carry the ripple switch, so the choice fell to which risk is cheaper to be wrong
about. A mixed graph fails at runtime in somebody else's application; an alpha API fails at compile
time in ours. The adapter takes **(b)** — `material3:1.12.0-alpha03`. Full working in
[research §1.2](../research/research-architecture.md).

The remaining Material criteria below are [B-14](B-14-material-adapter.md)'s and should be read
there.

Seven Gradle modules with strictly one-way dependencies — `kvadrant-resources ← kvadrant-core ←
kvadrant-material-adapter ← sample-gallery`, and `kvadrant-icons ← kvadrant-core` — convention
plugins in `build-logic`, targets `androidTarget` / `jvm("desktop")` / three iOS / `wasmJs`, a
version catalog as the only place a coordinate is written, ABI validation, and snapshot publishing.

- **This item also settles the one open question no spike can answer.** [Research
  §1.2](../research/research-architecture.md) established that there is no stable Material 3 in the
  Compose Multiplatform 1.12 line: the newest stable `org.jetbrains.compose.material3:material3` is
  1.9.0 on Jetpack M3 1.4.0, while CMP 1.12.0 ships `1.12.0-alpha03` on Jetpack M3 1.5.0-alpha22.
  Resolve both graphs and see which compiles — option (a) CMP 1.9.x + material3 1.9.0, or option (b)
  CMP 1.12.0 + the alpha.
- **Do not copy the brief's pinning snippet.** `strictly("[1.12.0, 1.13.0)")` on `compose.material3`
  resolves into the alpha line it was written to keep out.
- The rejected alternative is picking on paper from the POMs. `material3:1.9.0` pulls Compose
  runtime artefacts from the 1.9 line, so whether it co-resolves with a CMP 1.12 core is a fact
  about Gradle, not about POM text.
- **Resolving and compiling is not the check.** A mixed Compose graph of exactly this shape has
  been observed to resolve cleanly, compile cleanly and then throw `AbstractMethodError` at render
  time on the first text field ([research §1.2](../research/research-architecture.md)). The
  candidate graph is accepted when something is drawn, not when the build is green.
- Not covered: the adapter's own code, which is [B-14](B-14-material-adapter.md).

**Done so far:** Gradle 9.7.1, a Java 25 toolchain, Kotlin 2.4.10, ktlint (plugin 14.2.0 running
CLI 1.8.0) wired into `check` for every subproject, the shared `.editorconfig`, viddik screenshots
also wired into `check`, and one module — `kvadrant-core` — with the **desktop target only**.
`./gradlew check` is green and the screenshot gate has been shown to fail on a changed golden.
A `sample` module was added with it: a real Compose Desktop application, `./gradlew :sample:run`,
opening at **360×600 dp** — the phone's 480×800 canvas at the canonical 0.75. The size is not
incidental: the Start grid is four fixed columns, so a wider window leaves dead space at the right
rather than stretching, which is the grid being faithful.

**Running it found three defects that every screenshot had missed**, and they are worth listing
because of what they have in common — each is about the state a thing opens in, which a fixture sets
by hand and an application does not:

- **nothing painted the background.** `KvadrantTheme` provides colours and paints nothing;
  `KvadrantPage` paints, but an application rooted on a `KvadrantPivot` — a control, not a page —
  showed the window's own white through everything. Every fixture had painted it explicitly;
- **the pivot opened on the wrong page.** The cyclic pager started at `VIRTUAL_PAGES / 2`, and
  `524288 mod 3` is 2 — so it opened on the third title, looking like an application remembering a
  position it was never given. The start is now rounded down to a whole number of cycles;
- **the previous header showed through the page margin.** The strip extends left of the selected
  header, and nothing clipped it. Fixtures had all been rendered at a position where that did not
  show.

**Left:** the Material adapter module and with it the version question below, publishing, ABI
validation, and the remaining targets.

- AC met: `./gradlew build` compiles every declared target — four modules, desktop and Android —
  in 2m46s.
- AC met on desktop, and **the Android half was not a missing test**: `GraphRendersTest` renders an
  `OutlinedTextField` under the adapter and counts pixels, which is the check research §1.2 asks for
  — the failure it guards against compiled and resolved cleanly and died on the first text field.
  The adapter had no Android target *at all*, so the platform this interop exists to serve could not
  consume it. The target is there now (`androidLibrary`, an AAR and an `androidPublication`). That it
  *renders* on Android is Android's usual answer and not this gate's: viddik is JVM-only, so a green
  `check` says nothing about it (B-29).
- AC met: `composeMaterial3` in the catalog names the Jetpack version it resolves to, the graph it
  was chosen over, and why — and says to expect it to need re-checking, because it is an alpha whose
  ripple corner is the part being reshaped.
- AC superseded: the version catalog names each Material coordinate with the Jetpack version it maps to in a
  comment, and no coordinate appears as a string anywhere else.
- AC: the answer to the option (a)/(b) question is written into
  [research §D3](../research/research-architecture.md), with what actually failed if one failed.
- AC met: `abiValidation` runs in `check`, which CI runs on every pull request — verified by adding
  a public function and watching `checkKotlinAbi` fail, naming the new symbol in both the desktop
  dump and the klib one. A task that is *in* a graph and a task that would *catch* something are
  different claims and only the second one is worth an AC.
- Anchors (to be created): `build-logic/`, `gradle/libs.versions.toml`, `.github/workflows/`
