package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.components.TileRow
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

/**
 * A page as Windows Phone laid one out: a small application title, an oversized page title in the
 * flow of the content rather than in a bar above it, and everything hanging off the 12 px margin.
 */
@Composable
private fun StartPage(
    colors: KvadrantColors,
    title: String,
    subtitle: String,
    pressTileAt: Offset? = null,
) {
    val latin = kvadrantLatin()
    val cyrillic = kvadrantCyrillic()
    KvadrantTheme(colors = colors, typography = portableTypography(latin)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(KvadrantTheme.colors.background)
                .padding(horizontal = KvadrantTheme.metrics.margin),
            verticalArrangement = Arrangement.spacedBy(KvadrantTheme.metrics.tileGap),
        ) {
            Box(Modifier.padding(top = 21.dp, bottom = 4.dp)) {
                KvadrantText(
                    "KVADRANT UI",
                    style = KvadrantTheme.typography.pageTitle.copy(color = KvadrantTheme.colors.foreground),
                    cyrillic = cyrillic,
                )
            }

            // The Pivot header row: the selected one at full opacity, the next one peeking.
            Row(horizontalArrangement = Arrangement.spacedBy(KvadrantTheme.metrics.pivotHeaderGap)) {
                KvadrantText(title, style = KvadrantTheme.typography.pivotHeader, cyrillic = cyrillic)
                KvadrantText(
                    subtitle,
                    style = KvadrantTheme.typography.pivotHeader.copy(color = KvadrantTheme.colors.subtle),
                    cyrillic = cyrillic,
                )
            }

            TileRow(Modifier.padding(top = 6.dp)) {
                val pressed = remember { MutableInteractionSource() }
                LaunchedEffect(pressTileAt) {
                    pressTileAt?.let { pressed.emit(PressInteraction.Press(it)) }
                }
                KvadrantTile(TileSize.Medium, interactionSource = pressed) {
                    KvadrantText(
                        "почта",
                        Modifier.align(Alignment.BottomStart).padding(9.dp),
                        KvadrantTheme.typography.normal.copy(color = KvadrantTheme.colors.onAccent),
                        cyrillic = cyrillic,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(KvadrantTheme.metrics.tileGap)) {
                    KvadrantTile(TileSize.Small, color = KvadrantAccents.Emerald)
                    KvadrantTile(TileSize.Small, color = KvadrantAccents.Magenta)
                }
                Column(verticalArrangement = Arrangement.spacedBy(KvadrantTheme.metrics.tileGap)) {
                    KvadrantTile(TileSize.Small, color = KvadrantAccents.Amber)
                    KvadrantTile(TileSize.Small, color = KvadrantAccents.Steel)
                }
            }

            KvadrantTile(TileSize.Wide, color = KvadrantAccents.Cobalt) {
                KvadrantText(
                    "календарь",
                    Modifier.align(Alignment.BottomStart).padding(9.dp),
                    KvadrantTheme.typography.normal.copy(color = KvadrantTheme.colors.onAccent),
                    cyrillic = cyrillic,
                )
            }

            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KvadrantButton("сохранить", onClick = {}, cyrillic = cyrillic)
                KvadrantToggleSwitch(checked = true, onCheckedChange = {})
                KvadrantToggleSwitch(checked = false, onCheckedChange = {})
            }

            KvadrantText(
                "flat, rectangular, and it tilts · плоско, прямоугольно, и наклоняется",
                style = KvadrantTheme.typography.subtle.copy(color = KvadrantTheme.colors.subtle),
                cyrillic = cyrillic,
            )
        }
    }
}

@ViddikScreenshot(name = "start dark", group = "screen", width = 400, height = 700)
@Composable
internal fun StartDark(): Unit = StartPage(KvadrantColors.dark(), "start", "почта")

@ViddikScreenshot(name = "start light", group = "screen", width = 400, height = 700)
@Composable
internal fun StartLight(): Unit = StartPage(KvadrantColors.light(), "start", "почта")

@ViddikScreenshot(name = "start amber", group = "screen", width = 400, height = 700)
@Composable
internal fun StartAmber(): Unit = StartPage(KvadrantColors.dark(KvadrantAccents.Amber), "start", "почта")

/**
 * The same page with a finger on the mail tile, near its top-left corner. This is the whole reason
 * the library exists: Material grows a circle out of the touch point, Metro leans the plane towards
 * it.
 */
@ViddikScreenshot(name = "start pressed tile", group = "screen", width = 400, height = 700)
@Composable
internal fun StartPressed(): Unit = StartPage(KvadrantColors.dark(), "start", "почта", pressTileAt = Offset(12f, 12f))
