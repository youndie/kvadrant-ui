---
id: B-13
title: "ApplicationBar and PageHeader"
status: done
priority: P0
size: M
stage: stage-2-release
blocked_by: [B-05]
---

# B-13 — ApplicationBar and PageHeader

**`KvadrantAppBar` is in** — 54 dp tall (22.5 dp in its mini form), circular 36 dp buttons with a
1.125 dp ring and a 19.5 dp glyph box, the `···` overflow, and menu labels lowercased by the bar
because the phone lowercased them. `appbar_start_with_bar.png` and `appbar_menu_open.png`.

**The bar draws the ring and the caller supplies the glyph** (D10), so nothing here is blocked on
the icon set that has not been drawn.

**`KvadrantPage` closes this item.** It puts the title block, the page margin, the scroll and the
bar together — and it exists because five demo screens had assembled exactly that by hand,
identically each time, which is the signal that a pattern people copy should be a component. The
title scrolls away and the bar does not, which is the arrangement Metro chose: the top of the screen
belongs to the content and the bottom to the application.

`KvadrantPageHeader` is now its own file with the published margins — including the page title's
**negative** top margin of −7 px, which is an offset rather than a padding, because a negative
padding is not a thing and pretending otherwise would have silently dropped the number.

The two chrome components with fully published metrics: a 72 px bar with at most four 48×48 circular
icon buttons carrying a 26×26 glyph, a lowercase menu, and a 30 px mini form; and the WP page header,
which is an ApplicationTitle at 20 px above a PageTitle at 72 px.

- **The page header is not a top app bar.** In Metro the page title is ordinary text in the content
  flow and it scrolls away with the content. Anyone reaching for `TopAppBar` here gets a Material
  screen with Metro colours, which is exactly the failure mode this library exists to avoid
  ([research §1.3](../research/research-architecture.md)).
- **The bar draws the circle, the consumer supplies the glyph.** Icons are a designer-week that is
  routinely planned as a programmer-afternoon, so the first release takes a `content` slot and the
  drawn set arrives in [B-18](B-18-icon-set.md) — decision
  [D10](../research/research-architecture.md).
- Not covered: the Win8 silhouette header, which belongs to the optional branch
  ([B-22](B-22-win8-branch.md)).

- AC: four icons, an overflow menu in lowercase, and the mini form, all matching the published
  metrics in a screenshot test.
- AC: the page header scrolls with the content in the sample, not above it.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/components/KvadrantAppBar.kt`
