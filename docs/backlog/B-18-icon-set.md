---
id: B-18
title: "Draw the base icon set and a generator for it"
status: open
priority: P1
size: L
stage: stage-3-completeness
---

# B-18 — Draw the base icon set and a generator for it

Roughly 40 ApplicationBar glyphs — back, add, delete, edit, save, cancel, check, search, refresh,
share, settings and the rest — drawn from scratch as `ImageVector`, plus a tool that turns SVG into
`ImageVector` so the set can grow without hand-transcribing paths.

- **Nothing can be copied.** Segoe MDL2 Assets and Segoe Fluent Icons are proprietary, and there is
  no open Metro-styled equivalent ([research §1.7](../research/research-architecture.md)). The
  glyphs are drawn on a 26×26 grid inside a 48 dp circle, 1.5 dp stroke, no fill.
- **This is a designer-week, not a programmer-afternoon**, and it is the part of projects like this
  one that is most reliably underestimated — which is why the first release ships a `content` slot
  instead ([B-13](B-13-application-bar-and-page-header.md)) and this item is allowed to take as long
  as it takes.
- Rejected: an existing OFL icon font in a Fluent-ish style. None was found that is both licence-
  clean and stylistically Metro rather than Fluent, and mixing the two languages in one bar is
  visible immediately.

- AC: 40 glyphs in one consistent style, shipped in `kvadrant-icons`, with no Microsoft asset in the
  repository or its history.
- AC: the SVG → `ImageVector` tool is runnable by someone who did not write it.
- Anchors (to be created): `kvadrant-icons/src/commonMain/kotlin/`
