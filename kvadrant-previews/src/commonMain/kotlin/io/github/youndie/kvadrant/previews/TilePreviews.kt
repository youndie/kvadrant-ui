package io.github.youndie.kvadrant.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantCycleTile
import io.github.youndie.kvadrant.components.KvadrantFlipTile
import io.github.youndie.kvadrant.components.KvadrantIconicTile
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantTileBadge
import io.github.youndie.kvadrant.components.KvadrantTileGrid
import io.github.youndie.kvadrant.components.TileRow
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.icons.KvadrantIcons
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.contrastOn

internal fun tilePreviews(): List<KvadrantPreview> =
    listOf(
        KvadrantPreview(
            id = "tile",
            group = "tiles",
            component = "KvadrantTile",
            summary = "the three sizes, on the gap the Start screen actually used",
            heightDp = 340,
        ) { TilePreview() },
        KvadrantPreview(
            id = "tile-row",
            group = "tiles",
            component = "TileRow",
            summary = "tiles laid end to end on the tile gap",
            heightDp = 200,
        ) { TileRowPreview() },
        KvadrantPreview(
            id = "tile-grid",
            group = "tiles",
            component = "KvadrantTileGrid",
            summary = "mixed sizes packed into the Start screen's column grid",
            heightDp = 420,
        ) { TileGridPreview() },
        KvadrantPreview(
            id = "tile-badge",
            group = "tiles",
            component = "KvadrantTileBadge",
            summary = "the unread count in the corner of a tile",
            heightDp = 240,
        ) { TileBadgePreview() },
        KvadrantPreview(
            id = "flip-tile",
            group = "tiles",
            component = "KvadrantFlipTile",
            summary = "a live tile turning over on its own — the interval is jittered, as the phone's was",
            heightDp = 260,
        ) { FlipTilePreview() },
        KvadrantPreview(
            id = "iconic-tile",
            group = "tiles",
            component = "KvadrantIconicTile",
            summary = "the icon-and-count template, with the label at the foot",
            heightDp = 260,
        ) { IconicTilePreview() },
        KvadrantPreview(
            id = "cycle-tile",
            group = "tiles",
            component = "KvadrantCycleTile",
            summary = "several faces in rotation rather than two",
            heightDp = 260,
        ) { CycleTilePreview() },
    )

@Composable
private fun TileCentre(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center, content = content)
}

@Composable
private fun Caption(
    text: String,
    color: Color,
) {
    KvadrantText(
        text,
        Modifier.padding(8.dp),
        style = KvadrantTheme.typography.normal.copy(color = contrastOn(color)),
    )
}

@Composable
private fun TilePreview() {
    TileCentre {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KvadrantTile(TileSize.Wide, color = KvadrantAccents.Cobalt) {
                Caption("wide", KvadrantAccents.Cobalt)
            }
            TileRow {
                KvadrantTile(TileSize.Medium, color = KvadrantAccents.Emerald) {
                    Caption("medium", KvadrantAccents.Emerald)
                }
                KvadrantTile(TileSize.Small, color = KvadrantAccents.Crimson)
            }
        }
    }
}

@Composable
private fun TileRowPreview() {
    TileCentre {
        TileRow {
            KvadrantTile(TileSize.Small, color = KvadrantAccents.Emerald)
            KvadrantTile(TileSize.Small, color = KvadrantAccents.Amber)
            KvadrantTile(TileSize.Small, color = KvadrantAccents.Violet)
        }
    }
}

@Composable
private fun TileGridPreview() {
    val sizes =
        listOf(
            TileSize.Medium,
            TileSize.Medium,
            TileSize.Wide,
            TileSize.Small,
            TileSize.Small,
            TileSize.Small,
            TileSize.Small,
            TileSize.Medium,
        )
    val palette =
        listOf(
            KvadrantAccents.Cobalt,
            KvadrantAccents.Emerald,
            KvadrantAccents.Crimson,
            KvadrantAccents.Amber,
            KvadrantAccents.Violet,
            KvadrantAccents.Teal,
            KvadrantAccents.Magenta,
            KvadrantAccents.Olive,
        )
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        KvadrantTileGrid(sizes) { index, size ->
            KvadrantTile(size, color = palette[index % palette.size])
        }
    }
}

@Composable
private fun TileBadgePreview() {
    TileCentre {
        KvadrantTile(TileSize.Medium, color = KvadrantAccents.Cobalt) {
            Caption("mail", KvadrantAccents.Cobalt)
            KvadrantTileBadge(12)
        }
    }
}

@Composable
private fun FlipTilePreview() {
    TileCentre {
        KvadrantFlipTile(
            TileSize.Medium,
            color = KvadrantAccents.Magenta,
            front = { Caption("front", KvadrantAccents.Magenta) },
            back = { Caption("back", KvadrantAccents.Magenta) },
        )
    }
}

@Composable
private fun IconicTilePreview() {
    TileCentre {
        KvadrantIconicTile(
            size = TileSize.Medium,
            color = KvadrantAccents.Teal,
            count = 3,
            label = "messages",
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .paint(
                        rememberVectorPainter(KvadrantIcons.Mail),
                        colorFilter = ColorFilter.tint(contrastOn(KvadrantAccents.Teal)),
                    ),
            )
        }
    }
}

@Composable
private fun CycleTilePreview() {
    TileCentre {
        KvadrantCycleTile(
            faces =
                listOf<@Composable BoxScope.() -> Unit>(
                    { Caption("one", KvadrantAccents.Orange) },
                    { Caption("two", KvadrantAccents.Orange) },
                    { Caption("three", KvadrantAccents.Orange) },
                ),
            size = TileSize.Medium,
            color = KvadrantAccents.Orange,
        )
    }
}
