---
id: B-16
title: "Screenshot regression suite, and an honest statement of what it does not cover"
status: done
priority: P1
size: M
stage: stage-2-release
blocked_by: [B-05]
---

# B-16 — Screenshot regression suite, and an honest statement of what it does not cover

**Partly done.** 66 goldens: 20 accents in both themes, every screen, the tilt at nine touch points,
the transitions, and now the whole type ramp in both scripts (`type ramp dark`/`light`) — the row
that turns a missing glyph into a diff instead of a bug report. `ScreenshotSuiteTest` guards the
*set*: it fails if the registry is empty, if a fixture has no golden, or if a golden has no fixture.

Two things that came out of building it, both worth more than the fixtures:

- **The guard did not run.** It passed through two deliberate breakages before anyone noticed
  `desktopTest` was UP-TO-DATE: a test that reads a directory has to declare that directory as a
  task input, or Gradle reports the previous run's verdict about a set that has since changed. Fixed
  in `kvadrant-core/build.gradle.kts`; re-broken twice afterwards to watch it fail.
- **A negative control cannot be a golden here.** A "ramp without the companion family" fixture was
  written and thrown away: without Source Sans 3 the Cyrillic does not vanish, the *host* supplies
  it, so the image would record a macOS font and differ on a Linux runner. These goldens are
  portable only because every glyph comes from a bundled file.

**The Material comparison row exists**, and it is in `kvadrant-material-adapter` rather than here:
`adapter_pairs_dark`/`adapter_pairs_light` put five Kvadrant controls above their Material
counterparts and the three wrapped ones, inside `check`, with its own non-vacuity guard. Keeping it
beside the adapter rather than in the core is not tidiness — the core declares no Material
dependency and `noMaterialInTheCore` fails if it ever does.

**Closed with one row named as blocked rather than pending:** the Win8 baseline-grid overlay needs a
Win8 profile to exist at all ([B-22](B-22-win8-branch.md)), and there is nothing to overlay a grid
on. Android is **not** part of this item — [B-29](B-29-android-screenshot-coverage.md), which exists
because viddik turns out to be JVM-only, and where a second screenshot tool has been ruled out, so
the suite stays one suite.

**viddik** on the desktop target, wired into `check` and already running there
([B-23](B-23-viddik-pins-the-compose-line.md)). What this item adds is the matrix, once there are
components to put in it: 20 accents × dark and light; the whole type ramp in Cyrillic and
Latin; a baseline-grid overlay over the Win8 ramp; tilt at five touch points; and each Kvadrant
control beside its Material counterpart under the adapter.

- **viddik renders in a real Skiko window on the desktop target**, and its goldens are portable
  across operating systems, so a golden recorded on a laptop verifies on CI
  ([research §1.9](../research/research-architecture.md)). What it does not cover is the other
  targets: Android, iOS and wasm have no screenshot coverage at all, and that gap is written down
  rather than glossed — a green suite here says the desktop renderer is unchanged and nothing more.
- **Name every golden in ASCII.** viddik sanitises non-ASCII to underscores, so two fixtures whose
  names differ only in Cyrillic collapse into one file that overwrites itself.
- **Fixtures must never all disappear.** With no fixture, KSP reports SKIPPED, the verify task finds
  no tests and passes green — a screenshot suite that proves nothing while looking healthy. That is
  why a placeholder fixture exists in the module from the first commit.
- The accent × theme matrix is not thoroughness for its own sake: it is the only thing that catches
  a regression in `contrastOn`, which is a one-line function whose failure is invisible in code
  review and glaring on screen.
- The Cyrillic-and-Latin ramp render is how a missing glyph shows up as a diff instead of as a bug
  report ([B-07](B-07-font-stack.md)).

- AC: `./gradlew check` runs `viddikVerify`, and the gate is shown to fail on a changed golden
  rather than assumed to — **already true**, verified twice.
- AC: the contributing notes state which targets the suite does not cover, so a green run is not
  read as full coverage — **done**, and it now names the reason rather than the gap.
- AC: the suite cannot pass while empty, and that is checked rather than commented — **done**,
  `ScreenshotSuiteTest`.
- Anchors: `kvadrant-core/src/desktopTest/kotlin/`, `kvadrant-core/src/desktopTest/snapshots/`
