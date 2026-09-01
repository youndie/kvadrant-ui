package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantTileBadge
import io.github.youndie.kvadrant.components.KvadrantTileGrid
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantMetrics
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.scaled
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * The sample application's own window, at the size it opens — 360×600 dp, which is the phone's
 * 480×800 canvas at the canonical 0.75. A golden of what a person actually sees when they run it,
 * rather than of a component in isolation.
 *
 * The size is not incidental: the Start grid is four fixed columns, so a window wider than the
 * canvas leaves dead space at the right rather than stretching. That is the grid being faithful,
 * and it is why the demo opens at the size the design was drawn for.
 */
@ViddikScreenshot(name = "sample window", group = "app", width = 560, height = 860)
@Composable
internal fun SampleWindow() {
    val cyrillic = kvadrantCyrillic()
    val layout =
        listOf(
            TileSize.Medium,
            TileSize.Small,
            TileSize.Small,
            TileSize.Wide,
            TileSize.Small,
            TileSize.Small,
            TileSize.Medium,
        )
    val colours =
        listOf(
            KvadrantAccents.Cyan,
            KvadrantAccents.Emerald,
            KvadrantAccents.Magenta,
            KvadrantAccents.Cobalt,
            KvadrantAccents.Amber,
            KvadrantAccents.Steel,
            KvadrantAccents.Crimson,
        )
    val labels = mapOf(0 to "почта", 3 to "календарь", 6 to "фото")

    KvadrantTheme(
        colors = KvadrantColors.dark(),
        typography = portableTypography(kvadrantLatin()),
        metrics = KvadrantMetrics().scaled(1.6f),
    ) {
        Column(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) {
            KvadrantPivot(
                titles = listOf("start", "почта", "settings"),
                title = "KVADRANT UI",
                cyrillic = cyrillic,
                modifier = Modifier.weight(1f),
            ) {
                KvadrantTileGrid(layout) { index, size ->
                    KvadrantTile(size, color = colours[index]) {
                        if (index == 0) KvadrantTileBadge(7)
                        labels[index]?.let {
                            KvadrantText(
                                it,
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
            KvadrantAppBar(menuItems = listOf("Настройки", "О программе"), cyrillic = cyrillic) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    listOf(KvadrantAccents.Cyan, KvadrantAccents.Emerald, KvadrantAccents.Amber)
                        .forEach { colour ->
                            KvadrantAppBarButton(onClick = {}) {
                                // Sized to the glyph box and clipped round, which is what
                                // `KvadrantSampleApp` draws. A `Small` tile filling the button
                                // covers the ring the button is made of — the sample says so in a
                                // comment and this fixture had the version the comment warns
                                // about, so the golden of "what a person sees when they run it"
                                // showed something the demo does not.
                                Box(
                                    Modifier
                                        .size(KvadrantTheme.metrics.appBarGlyph)
                                        .clip(CircleShape)
                                        .background(colour),
                                )
                            }
                        }
                }
            }
        }
    }
}
