---
id: B-10
title: "Tile and TileGrid"
status: done
priority: P0
size: M
stage: stage-1-core
blocked_by: [B-05]
---

# B-10 — Tile and TileGrid

**Done:** `KvadrantTile` in three sizes and `TileRow` with the 9 dp gap; the tile takes the theme's
indication, so it tilts (`screen_start_pressed_tile.png`). `KvadrantTileGrid` packs mixed sizes on the Start screen's four-column grid — small takes one
column, medium two, wide all four, each tile dropping onto the first row with room. That is why a
Start screen never has a ragged right edge (`appbar_tile_grid_packing.png`).

`KvadrantTileBadge` is a **square** — the round badge belongs to Material and iOS — and stops at
`99+`, which is a rule rather than a rendering limit. `KvadrantFlipTile` turns between two faces on
an interval that **is this project's invention and says so**: Microsoft never specified the timing
and deliberately never let applications control it (a random interval behind a five-deep FIFO
queue), so the parameter carries a ±25 % jitter — a grid flipping in lockstep is the tell of every
reimplementation, and the jitter matters more than the number.

`KvadrantCycleTile` completes the set: up to **nine** faces, which is the platform's limit rather
than a suggestion, one sliding up as the next arrives from below — never a crossfade, which is not
how the phone's picture tiles moved.

`KvadrantIconicTile` before it. Its icon is sized to a **best-fit box** — 70×110 px in a
small tile, 130×202 in a medium one — rather than filling the tile, which is why the phone's own
tiles read as one family however different their glyphs were.

**There is no large size and there will not be one:** Windows 8 added a large square, the phone
never had one, and adding it would be inventing a size rather than transcribing one.

Four tile sizes with a `TileSize` enum, and a grid that lays small / medium / wide / large out with
the 9 dp gap. Tiles are what people picture when they hear "Metro", and they are the demo that makes
the library legible in one screenshot.

- The tile sizes are published; **the 12 px gap is derived arithmetically rather than published**
  (🟡 in the specification), so it is a number to verify against a screenshot of a real Start screen
  rather than to trust.
- Tiles are also the surface where tilt is most visible, which makes this item the practical
  acceptance test for [B-08](B-08-tilt-indication.md).
- Not covered: flip, iconic and cycle tiles, badges and drag-reorder. The flip interval in
  particular is a number Microsoft never specified and deliberately never let applications control
  — see open question 2 in [research §3](../research/research-architecture.md).

- AC: a grid of mixed sizes lays out with no gaps or overlaps at every window size in the sample.
- AC: a screenshot test compares the grid against the reference diagram in the brief.
- Anchors (to be created): `kvadrant-core/src/commonMain/kotlin/components/KvadrantTile.kt`
