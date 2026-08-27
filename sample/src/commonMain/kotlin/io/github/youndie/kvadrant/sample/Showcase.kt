package io.github.youndie.kvadrant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantContextMenuHost
import io.github.youndie.kvadrant.components.KvadrantGroupHeader
import io.github.youndie.kvadrant.components.KvadrantJumpList
import io.github.youndie.kvadrant.components.KvadrantListItem
import io.github.youndie.kvadrant.components.KvadrantLoopingSelector
import io.github.youndie.kvadrant.components.KvadrantPage
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.components.KvadrantRoll
import io.github.youndie.kvadrant.components.KvadrantRotate
import io.github.youndie.kvadrant.components.KvadrantSlide
import io.github.youndie.kvadrant.components.KvadrantSurface
import io.github.youndie.kvadrant.components.KvadrantSwivel
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.SlideDirection
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme

/**
 * One page per tile, so the components that only existed in screenshot fixtures can be pressed.
 *
 * Twenty-four of the library's forty-two public composables were never called by the interactive
 * demo — `KvadrantPanorama`, `KvadrantJumpList`, `KvadrantLoopingSelector`, the live tiles, the
 * message box, the toast — and a component nobody has touched by hand is a component whose
 * behaviour is known only from a still frame. The tiles became doors once the demo had navigation.
 */
@Composable
internal fun Showcase(
    name: String,
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    when (name) {
        "почта" -> ContactsShowcase(cyrillic, onBack)
        "календарь" -> PickerShowcase(cyrillic, onBack)
        "фото" -> PanoramaShowcase(cyrillic, onBack)
        else -> MotionShowcase(name, cyrillic, onBack)
    }
}

/** A long list the way the phone built one: group headers, and a jump list over them. */
@Composable
private fun ContactsShowcase(
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    var jumping by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val groups =
        listOf(
            "А" to listOf("Анна Петрова", "Артём"),
            "Д" to listOf("Дмитрий"),
            "B" to listOf("build server"),
        )

    KvadrantPage(applicationTitle = "KVADRANT UI", pageTitle = "почта", cyrillic = cyrillic) {
        KvadrantButton(
            if (jumping) "назад к списку" else "перейти к букве",
            { jumping = !jumping },
            cyrillic = cyrillic,
        )

        if (jumping) {
            KvadrantJumpList(
                letters = ('А'..'Я').map(Char::toString),
                enabled = groups.map { it.first }.toSet(),
                onLetterClick = { jumping = false },
                cyrillic = cyrillic,
                modifier = Modifier.padding(top = 9.dp),
            )
        } else {
            groups.forEach { (letter, names) ->
                KvadrantGroupHeader(letter, Modifier.padding(top = 9.dp), onClick = { jumping = true })
                names.forEach { who ->
                    KvadrantContextMenuHost(
                        expanded = menuFor == who,
                        items = listOf("закрепить на экране", "удалить", "изменить"),
                        onDismiss = { menuFor = null },
                        onItemClick = { menuFor = null },
                        cyrillic = cyrillic,
                    ) {
                        KvadrantListItem(
                            who,
                            subtitle = "нажмите для контекстного меню",
                            onClick = { menuFor = who },
                            cyrillic = cyrillic,
                            titleStyle = KvadrantTheme.typography.mediumLarge,
                        )
                    }
                }
            }
        }
        KvadrantButton("назад", onBack, Modifier.padding(top = 18.dp), cyrillic)
    }
}

/** Three looping selectors, which is what the phone's date picker is made of. */
@Composable
private fun PickerShowcase(
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    var day by remember { mutableStateOf(12) }
    var month by remember { mutableStateOf(7) }
    var year by remember { mutableStateOf(6) }

    KvadrantPage(applicationTitle = "KVADRANT UI", pageTitle = "календарь", cyrillic = cyrillic) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            KvadrantLoopingSelector(
                (1..31).map(Int::toString),
                day,
                { day = it },
                Modifier.weight(1f),
                label = "день",
                cyrillic = cyrillic,
            )
            KvadrantLoopingSelector(
                listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"),
                month,
                { month = it },
                Modifier.weight(1f),
                label = "месяц",
                cyrillic = cyrillic,
            )
            KvadrantLoopingSelector(
                (2020..2030).map(Int::toString),
                year,
                { year = it },
                Modifier.weight(1f),
                label = "год",
                cyrillic = cyrillic,
            )
        }
        KvadrantButton("назад", onBack, Modifier.padding(top = 18.dp), cyrillic)
    }
}

/** The panorama, which cannot be shown inside a Pivot page — it is a whole screen. */
@Composable
private fun PanoramaShowcase(
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) {
        KvadrantPanorama(
            title = "фото",
            cyrillic = cyrillic,
            background = { modifier ->
                // A stand-in for the wide image the phone parallaxed behind the sections.
                Row(modifier) {
                    listOf(
                        KvadrantAccents.Crimson,
                        KvadrantAccents.Violet,
                        KvadrantAccents.Cobalt,
                        KvadrantAccents.Teal,
                    ).forEach { Box(Modifier.width(320.dp).fillMaxSize().background(it)) }
                }
            },
            sections =
                listOf(
                    "недавние" to { PhotoStrip(KvadrantAccents.Amber) },
                    "альбомы" to { PhotoStrip(KvadrantAccents.Emerald) },
                    "избранное" to { PhotoStrip(KvadrantAccents.Magenta) },
                ),
        )
        KvadrantButton("назад", onBack, Modifier.align(Alignment.BottomStart).padding(9.dp), cyrillic)
    }
}

@Composable
private fun PhotoStrip(colour: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(3) { KvadrantTile(io.github.youndie.kvadrant.components.TileSize.Small, color = colour) {} }
    }
}

/** The four transitions that were written and never watched outside a still. */
@Composable
private fun MotionShowcase(
    name: String,
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    var shown by remember { mutableStateOf(true) }

    KvadrantPage(applicationTitle = "KVADRANT UI", pageTitle = name, cyrillic = cyrillic) {
        KvadrantButton(if (shown) "спрятать" else "показать", { shown = !shown }, cyrillic = cyrillic)

        listOf<Pair<String, @Composable (@Composable () -> Unit) -> Unit>>(
            "swivel" to { c -> KvadrantSwivel(shown) { c() } },
            "roll" to { c -> KvadrantRoll(shown) { c() } },
            "slide" to { c -> KvadrantSlide(shown, SlideDirection.Up) { c() } },
            "rotate" to { c -> KvadrantRotate(shown, 25f) { c() } },
        ).forEach { (label, wrap) ->
            Box(Modifier.fillMaxWidth().height(60.dp).padding(top = 9.dp)) {
                wrap {
                    KvadrantSurface(Modifier.fillMaxSize()) {
                        KvadrantText(label, Modifier.align(Alignment.Center), cyrillic = cyrillic)
                    }
                }
            }
        }
        KvadrantButton("назад", onBack, Modifier.padding(top = 18.dp), cyrillic)
    }
}
