package io.github.youndie.kvadrant.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import io.github.youndie.kvadrant.theme.KvadrantEasing
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.delay

/**
 * The Start screen's tile sizes: 99, 210, and 432×210 Metro pixels.
 *
 * Three, not four. Windows 8 added a large square; the phone never had one, and adding it here
 * would be inventing a size rather than transcribing one.
 *
 * [units] is the tile's width on the Start screen's four-column grid, which is what
 * [KvadrantTileGrid] packs against.
 */
public enum class TileSize(
    public val units: Int,
) {
    Small(1),
    Medium(2),
    Wide(4),
}

/**
 * A Start-screen tile: a flat rectangle of accent colour that tilts towards the finger.
 *
 * No corner radius, no shadow, no ripple — and nothing here says so, because the theme already
 * does. The only thing a tile adds over a coloured box is its size and the press target.
 */
@Composable
public fun KvadrantTile(
    size: TileSize = TileSize.Medium,
    modifier: Modifier = Modifier,
    color: Color = KvadrantTheme.colors.accent,
    onClick: () -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val metrics = KvadrantTheme.metrics
    val (w, h) =
        when (size) {
            TileSize.Small -> metrics.tileSmall to metrics.tileSmall
            TileSize.Medium -> metrics.tileMedium to metrics.tileMedium
            TileSize.Wide -> metrics.tileWide to metrics.tileMedium
        }
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    Box(
        modifier
            .size(w, h)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ).background(color),
        content = content,
    )
}

/** Tiles in a row with the 12 px gap of the Start screen. */
@Composable
public fun TileRow(
    modifier: Modifier = Modifier,
    gap: Dp = KvadrantTheme.metrics.tileGap,
    content: @Composable () -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) { content() }
}

/**
 * The Start screen's packing: a four-column grid where a small tile takes one column, a medium two
 * and a wide all four, and each tile drops onto the first row with room for it.
 *
 * This is why a Start screen never has a ragged right edge however the tiles are mixed — the grid
 * is in units, not in pixels, and the 12 px gap is the only spacing anywhere in it.
 */
@Composable
public fun KvadrantTileGrid(
    tiles: List<TileSize>,
    modifier: Modifier = Modifier,
    columns: Int = COLUMNS,
    gap: Dp = KvadrantTheme.metrics.tileGap,
    tile: @Composable (index: Int, size: TileSize) -> Unit,
) {
    // Greedy row packing: it is what the Start screen does, and anything cleverer would rearrange
    // tiles the user placed deliberately.
    val rows =
        remember(tiles, columns) {
            val out = mutableListOf<MutableList<Int>>()
            var used = columns
            tiles.forEachIndexed { index, size ->
                if (used + size.units > columns) {
                    out += mutableListOf(index)
                    used = size.units
                } else {
                    out.last() += index
                    used += size.units
                }
            }
            out
        }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { index -> tile(index, tiles[index]) }
            }
        }
    }
}

private const val COLUMNS = 4

/**
 * The counter bubble in a tile's corner.
 *
 * A square, like everything else — the round badge is Material's and iOS's. The phone stopped
 * counting at 99 and showed `99+`, which is a rule rather than a rendering artefact.
 */
@Composable
public fun BoxScope.KvadrantTileBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    val colors = KvadrantTheme.colors
    Box(
        modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .background(colors.contrastBackground)
            .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
    ) {
        KvadrantText(
            if (count > BADGE_MAX) "$BADGE_MAX+" else count.toString(),
            style = KvadrantTheme.typography.subtle.copy(color = colors.contrastForeground),
        )
    }
}

private const val BADGE_MAX = 99

/**
 * A tile that turns over between two faces.
 *
 * **The interval is ours and says so.** Microsoft never specified the flip timing and deliberately
 * never let applications control it — the platform used a random interval with a five-deep FIFO
 * queue. So [intervalMillis] is this project's invention, and the ±25 % jitter matters more than
 * the number: a grid of tiles flipping in lockstep is the tell of every reimplementation.
 */
@Composable
public fun KvadrantFlipTile(
    size: TileSize = TileSize.Medium,
    modifier: Modifier = Modifier,
    color: Color = KvadrantTheme.colors.accent,
    intervalMillis: Long = DEFAULT_FLIP_INTERVAL_MILLIS,
    jitter: Float = DEFAULT_FLIP_JITTER,
    seed: Int = 0,
    front: @Composable BoxScope.() -> Unit = {},
    back: @Composable BoxScope.() -> Unit = {},
) {
    var showingBack by remember { mutableStateOf(false) }
    LaunchedEffect(intervalMillis, jitter, seed) {
        // Deterministic per tile, so a grid does not march in step and a golden stays a golden.
        var offset = ((seed * 2654435761u.toLong()) % 1000L) / 1000f
        while (true) {
            val wait = intervalMillis * (1f - jitter + 2f * jitter * offset)
            delay(wait.toLong())
            showingBack = !showingBack
            offset = (offset * 7f) % 1f
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (showingBack) 180f else 0f,
        animationSpec = tween(FLIP_MILLIS, easing = KvadrantEasing.ExponentialOut6),
        label = "flip",
    )

    KvadrantTile(size, modifier, color) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                rotationX = rotation
                cameraDistance = kvadrantCameraUnits()
            },
        ) {
            if (rotation < 90f) front() else Box(Modifier.graphicsLayer { rotationX = 180f }) { back() }
        }
    }
}

/** Six seconds. Not Microsoft's number — see the note on [KvadrantFlipTile]. */
public const val DEFAULT_FLIP_INTERVAL_MILLIS: Long = 6_000L

/** ±25 %, so that no two tiles in a grid turn together. */
public const val DEFAULT_FLIP_JITTER: Float = 0.25f

private const val FLIP_MILLIS = 500

/**
 * The phone's own tile shape: a small icon, a count, and a label along the bottom.
 *
 * The icon is not artwork filling the tile — it is a glyph sized to a **best-fit box** that leaves
 * room for the rest: 70×110 px inside a small tile, 130×202 inside a medium one. That is why the
 * phone's own tiles all looked like one family however different their icons were.
 */
@Composable
public fun KvadrantIconicTile(
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.Medium,
    color: Color = KvadrantTheme.colors.accent,
    count: Int = 0,
    label: String? = null,
    cyrillic: FontFamily? = null,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit = {},
) {
    val iconBox =
        when (size) {
            TileSize.Small -> ICON_SMALL
            else -> ICON_MEDIUM
        }
    KvadrantTile(size, modifier, color, onClick) {
        Box(Modifier.align(Alignment.Center).size(iconBox.first, iconBox.second)) { icon() }
        if (count > 0) KvadrantTileBadge(count)
        if (label != null && size != TileSize.Small) {
            KvadrantText(
                label,
                Modifier.align(Alignment.BottomStart).padding(9.dp),
                KvadrantTheme.typography.normal.copy(color = KvadrantTheme.colors.onAccent),
                cyrillic,
            )
        }
    }
}

/** Best-fit boxes: 70×110 px in a small tile, 130×202 in a medium one. */
private val ICON_SMALL = 52.5.dp to 82.5.dp
private val ICON_MEDIUM = 97.5.dp to 151.5.dp

/**
 * A tile that cycles through up to nine faces, one sliding up as the next arrives.
 *
 * Nine is the platform's limit, not a suggestion. The interval carries the same caveat as
 * [KvadrantFlipTile]'s: Microsoft never specified it and never let applications set it, so this is
 * ours, jittered so a grid does not march in step.
 */
@Composable
public fun KvadrantCycleTile(
    faces: List<@Composable BoxScope.() -> Unit>,
    modifier: Modifier = Modifier,
    size: TileSize = TileSize.Medium,
    color: Color = KvadrantTheme.colors.accent,
    intervalMillis: Long = DEFAULT_FLIP_INTERVAL_MILLIS,
    jitter: Float = DEFAULT_FLIP_JITTER,
    seed: Int = 0,
) {
    require(faces.size <= MAX_CYCLE_FACES) {
        "a cycle tile shows at most $MAX_CYCLE_FACES faces; got ${faces.size}"
    }
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(faces.size, intervalMillis, seed) {
        if (faces.size < 2) return@LaunchedEffect
        var offset = ((seed * 2654435761u.toLong()) % 1000L) / 1000f
        while (true) {
            delay((intervalMillis * (1f - jitter + 2f * jitter * offset)).toLong())
            index = (index + 1) % faces.size
            offset = (offset * 7f) % 1f
        }
    }

    val slide by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = tween(CYCLE_MILLIS, easing = KvadrantEasing.ExponentialOut6),
        label = "cycle",
    )

    KvadrantTile(size, modifier, color) {
        // One face leaves upwards as the next arrives from below, which is how the phone's picture
        // tiles moved — never a crossfade.
        val current = slide.toInt().coerceIn(0, faces.lastIndex)
        val fraction = slide - current
        Box(Modifier.fillMaxSize().graphicsLayer { translationY = -fraction * this.size.height }) {
            faces.getOrNull(current)?.invoke(this)
        }
        if (fraction > 0f && current + 1 <= faces.lastIndex) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    translationY = (1f - fraction) * this.size.height
                },
            ) { faces[current + 1].invoke(this) }
        }
    }
}

/** The platform's own limit. */
public const val MAX_CYCLE_FACES: Int = 9

private const val CYCLE_MILLIS = 500
