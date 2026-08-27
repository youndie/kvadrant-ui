---
id: B-23
title: "Bring the screenshot suite back once viddik runs on the current Compose line"
status: done
priority: P1
size: S
stage: stage-1-core
---

# B-23 — Bring the screenshot suite back once viddik runs on the current Compose line

**Done.** viddik **0.2.0.14** runs on Compose Multiplatform 1.12.0, and the suite is back: skiko
resolves to 0.150.1 on every configuration with no force anywhere, `viddikVerify` runs inside
`./gradlew check`, and the gate was re-tested by swapping one golden for the other and watching the
build go red. What follows is the record of why it was out, kept because the reasoning is what
stops the same trade being made again.

There was no screenshot coverage in this repository for a while. viddik 0.1.2.13 cannot run against
Compose Multiplatform 1.12.0, and the library was briefly pinned to CMP 1.11.1 to accommodate it —
which was the wrong trade and has been reverted: a test-only tool must not choose the Compose line a
published library is compiled against.

Two independent breakages, both inside viddik's capture engine, and neither fixable from here:

```
skiko 0.150.1   Matrix44(float[]) is now private
                → IllegalAccessError at the first rendered frame

CMP 1.12.0      ComposeScene.render(Canvas, long) is gone; the interface now exposes
                measureAndLayout() and draw(Canvas) as separate calls
                → NoSuchMethodError
```

- **Holding skiko back on the test classpath does not work**, and that was measured rather than
  assumed: forcing skiko 0.144.6 under CMP 1.12.0 gets past the `IllegalAccessError` and lands on
  the `NoSuchMethodError` instead. viddik is bound to the 1.11 line by the Compose API, not only by
  skiko.
- **skiko is not ours to pin in any case.** It arrives transitively with `compose.ui` and is chosen
  by the CMP version; forcing it means calling a renderer the Compose runtime above it was not built
  against — the same class of failure one layer down.
- The rejected alternative is keeping CMP 1.11.1 until viddik catches up. It buys a screenshot gate
  over a module with no components in it, and charges every future consumer an outdated Compose
  line for it.

- AC: viddik publishes a version that runs on the current CMP, and `ksp` + `viddik` come back into
  `gradle/libs.versions.toml`, the root build file and `kvadrant-core`.
- AC: `viddik { verifyOnCheck.set(true) }` — the suite is inside the one gate, not beside it.
- AC: `desktopTest` gets `compose.desktop.currentOs`; without the host's native skia the test dies
  with `NoClassDefFoundError: Could not initialize class org.jetbrains.skia.Surface`. It must not
  reach a published source set — a POM that pins a host-specific skiko artefact is broken for every
  other host.
- AC: at least one fixture exists from the moment the plugin returns. With none, KSP reports SKIPPED
  and the verify task passes green with no tests in it.
- Anchors: `gradle/libs.versions.toml`, `kvadrant-core/build.gradle.kts`
