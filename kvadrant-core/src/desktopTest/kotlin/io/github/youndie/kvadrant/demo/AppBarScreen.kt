package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantAppBar
import io.github.youndie.kvadrant.components.KvadrantAppBarButton
import io.github.youndie.kvadrant.components.KvadrantPage
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantTileGrid
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

private val Layout =
    listOf(
        TileSize.Medium,
        TileSize.Small,
        TileSize.Small,
        TileSize.Wide,
        TileSize.Small,
        TileSize.Small,
        TileSize.Medium,
    )

private val Labels = mapOf(0 to "почта", 3 to "календарь", 6 to "фото")

private val Colours =
    listOf(
        KvadrantAccents.Cyan,
        KvadrantAccents.Emerald,
        KvadrantAccents.Magenta,
        KvadrantAccents.Cobalt,
        KvadrantAccents.Amber,
        KvadrantAccents.Steel,
        KvadrantAccents.Crimson,
    )

@Composable
private fun StartWithBar(
    colors: KvadrantColors,
    menuExpanded: Boolean,
) {
    val cyrillic = kvadrantCyrillic()
    KvadrantTheme(colors = colors, typography = portableTypography(kvadrantLatin())) {
        KvadrantPage(
            applicationTitle = "KVADRANT UI",
            pageTitle = "start",
            cyrillic = cyrillic,
            appBar = {
                KvadrantAppBar(
                    menuItems = listOf("Настройки", "О программе"),
                    menuExpanded = menuExpanded,
                    cyrillic = cyrillic,
                ) {
                    // The bar draws the ring; these stand in for the glyph set not drawn yet.
                    listOf(KvadrantAccents.Cyan, KvadrantAccents.Emerald, KvadrantAccents.Amber)
                        .forEach { colour ->
                            KvadrantAppBarButton(onClick = {}) {
                                Box(
                                    Modifier
                                        .size(KvadrantTheme.metrics.appBarGlyph)
                                        .clip(CircleShape)
                                        .background(colour),
                                )
                            }
                        }
                }
            },
        ) {
            KvadrantTileGrid(Layout) { index, size ->
                KvadrantTile(size, color = Colours[index]) {
                    if (size != TileSize.Small) {
                        KvadrantText(
                            Labels.getValue(index),
                            Modifier.align(Alignment.BottomStart).padding(9.dp),
                            KvadrantTheme.typography.normal.copy(
                                color = KvadrantTheme.colors.onAccent,
                            ),
                            cyrillic,
                        )
                    }
                }
            }
        }
    }
}

@ViddikScreenshot(name = "start with bar", group = "appbar", width = 400, height = 640)
@Composable
internal fun StartWithBarDark(): Unit = StartWithBar(KvadrantColors.dark(), menuExpanded = false)

/** The overflow open. Menu labels are lowercased by the bar, because the phone lowercased them. */
@ViddikScreenshot(name = "menu open", group = "appbar", width = 400, height = 640)
@Composable
internal fun StartWithBarMenu(): Unit = StartWithBar(KvadrantColors.dark(), menuExpanded = true)

@ViddikScreenshot(name = "tile grid packing", group = "appbar", width = 400, height = 480)
@Composable
internal fun TileGridPacking() {
    KvadrantTheme(typography = portableTypography(kvadrantLatin())) {
        Column(
            Modifier.fillMaxSize().background(KvadrantTheme.colors.background).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            KvadrantText("four columns, greedy rows", style = KvadrantTheme.typography.normal)
            KvadrantTileGrid(Layout, Modifier.fillMaxWidth()) { index, size ->
                KvadrantTile(size, color = Colours[index])
            }
        }
    }
}
