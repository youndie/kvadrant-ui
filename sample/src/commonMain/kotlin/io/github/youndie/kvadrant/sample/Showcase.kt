package io.github.youndie.kvadrant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
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
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.delay

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
    // Where the pressed row sits, so the menu can open below it the way the phone did. The host
    // cannot work this out for itself: it wraps the page, and the row is somewhere inside it.
    var anchorTop by remember { mutableStateOf(0.dp) }
    var anchorHeight by remember { mutableStateOf(0.dp) }
    // A plain map, deliberately not state. Every row reports its position after every layout pass,
    // and three rows writing one piece of *state* is a hang: each write differs from the last, each
    // differing write schedules another layout, and the three chase each other forever. Writing to
    // an ordinary map invalidates nothing, and the value is only lifted into state on a click,
    // which is the one moment it is needed.
    val rowBounds = remember { mutableMapOf<String, Pair<Dp, Dp>>() }
    val density = LocalDensity.current
    val groups =
        listOf(
            "А" to listOf("Анна Петрова", "Артём"),
            "Д" to listOf("Дмитрий"),
            "B" to listOf("build server"),
        )

    // One host around the whole page, which is what its KDoc says it is for: the menu is an overlay
    // and the page goes *behind* it, scaled to 0.94. Wrapping each row in one — which is what this
    // did first — puts a full-screen host inside every list item, so the menu opens by pushing the
    // list apart from inside the row it belongs to instead of covering the page.
    KvadrantContextMenuHost(
        expanded = menuFor != null,
        items = listOf("закрепить на экране", "удалить", "изменить"),
        onDismiss = { menuFor = null },
        onItemClick = { menuFor = null },
        cyrillic = cyrillic,
        anchorTop = anchorTop,
        anchorHeight = anchorHeight,
    ) {
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
                        KvadrantListItem(
                            who,
                            subtitle = "нажмите для контекстного меню",
                            onClick = {
                                rowBounds[who]?.let { (top, height) ->
                                    anchorTop = top
                                    anchorHeight = height
                                }
                                menuFor = who
                            },
                            cyrillic = cyrillic,
                            titleStyle = KvadrantTheme.typography.mediumLarge,
                            modifier =
                                Modifier.onGloballyPositioned { coordinates ->
                                    with(density) {
                                        rowBounds[who] =
                                            coordinates.positionInRoot().y.toDp() to
                                            coordinates.size.height.toDp()
                                    }
                                },
                        )
                    }
                }
            }
            KvadrantButton("назад", onBack, Modifier.padding(top = 18.dp), cyrillic = cyrillic)
        }
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
        KvadrantButton("назад", onBack, Modifier.padding(top = 18.dp), cyrillic = cyrillic)
    }
}

/** The panorama, which cannot be shown inside a Pivot page — it is a whole screen. */
@Composable
private fun PanoramaShowcase(
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    // No insets here, and that is the correction rather than the omission it looks like. A panorama
    // *is* the page, and its background image reaches the top of the glass; inset the whole thing
    // and you get a band of page colour above an image that should have run under the status bar.
    // `KvadrantPanorama` holds its own content clear and lets the background through.
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
            // Four sections of four different widths, and that is the demonstration.
            //
            // A panorama is not a pager: a section is as wide as its content, the viewport shows
            // whatever falls inside it, and two sections are visible at once whenever the first one
            // is narrow. The old example had three sections of identical three-tile strips, so it
            // showed a horizontal scroller and none of that — no section wider than the screen to
            // scroll *into*, nothing narrow enough to sit beside its neighbour, and the title's
            // parallax against a uniform rhythm to compare it with.
            sections =
                listOf(
                    // Wider than any phone: two rows of five. The one section you scroll through
                    // rather than past, and where the title and background visibly lag behind.
                    "недавние" to {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            repeat(2) { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    repeat(5) { column ->
                                        KvadrantTile(
                                            TileSize.Medium,
                                            color = PHOTO_COLOURS[(row * 5 + column) % PHOTO_COLOURS.size],
                                        ) {}
                                    }
                                }
                            }
                        }
                    },
                    // Narrower than the viewport, so its neighbour's header is already on screen —
                    // the panorama's whole promise that the page continues past the edge.
                    "альбомы" to {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            ALBUMS.forEach { (name, count) ->
                                KvadrantListItem(name, subtitle = count, onClick = {}, cyrillic = cyrillic)
                            }
                        }
                    },
                    // One wide tile and a caption: a section can be a single object.
                    "избранное" to {
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            KvadrantTile(TileSize.Wide, color = KvadrantAccents.Magenta) {
                                KvadrantText(
                                    "лето",
                                    Modifier.align(Alignment.BottomStart).padding(9.dp),
                                    style = KvadrantTheme.typography.large,
                                    cyrillic = cyrillic,
                                )
                            }
                            KvadrantText(
                                "снято в июле",
                                style = KvadrantTheme.typography.subtle,
                                cyrillic = cyrillic,
                            )
                        }
                    },
                    // Prose, wrapped to a width the section chooses for itself. The phone's own
                    // panoramas ended on something quiet like this rather than on more tiles.
                    "о панораме" to {
                        KvadrantText(
                            "секция шириной со своё содержимое, а не со экран.\n" +
                                "заголовок и фон едут медленнее секций,\n" +
                                "и список не кончается — он замкнут.",
                            Modifier.width(280.dp),
                            cyrillic = cyrillic,
                        )
                    },
                ),
        )
        KvadrantButton("назад", onBack, Modifier.align(Alignment.BottomStart).padding(9.dp), cyrillic = cyrillic)
    }
}

/** Enough hues that a ten-tile grid does not read as a repeating pattern. */
private val PHOTO_COLOURS =
    listOf(
        KvadrantAccents.Amber,
        KvadrantAccents.Emerald,
        KvadrantAccents.Cobalt,
        KvadrantAccents.Crimson,
        KvadrantAccents.Violet,
        KvadrantAccents.Teal,
        KvadrantAccents.Magenta,
    )

private val ALBUMS =
    listOf(
        "камера" to "241 снимок",
        "скриншоты" to "58 снимков",
        "сохранённые" to "12 снимков",
        "панорамы" to "3 снимка",
    )

/** The four transitions that were written and never watched outside a still. */
@Composable
private fun MotionShowcase(
    name: String,
    cyrillic: FontFamily,
    onBack: () -> Unit,
) {
    var shown by remember { mutableStateOf(true) }
    // Composed until the longest exit has finished, and then not. These transitions turn their
    // content; they do not hide it, and on the phone the navigation removes the page when the
    // transition completes. Leave it composed and the roll ends as a full-width strip lying across
    // the page at ninety degrees, which is what "roll does not disappear" looks like.
    var present by remember { mutableStateOf(true) }
    LaunchedEffect(shown) {
        if (shown) {
            present = true
        } else {
            delay(ROLL_MILLIS)
            present = false
        }
    }

    KvadrantPage(applicationTitle = "KVADRANT UI", pageTitle = name, cyrillic = cyrillic) {
        KvadrantButton(if (shown) "спрятать" else "показать", { shown = !shown }, cyrillic = cyrillic)

        listOf<Pair<String, @Composable (@Composable () -> Unit) -> Unit>>(
            "swivel" to { c -> KvadrantSwivel(shown) { c() } },
            "roll" to { c -> KvadrantRoll(shown) { c() } },
            "slide" to { c -> KvadrantSlide(shown, SlideDirection.Up) { c() } },
            "rotate" to { c -> KvadrantRotate(shown, 25f) { c() } },
        ).forEach { (label, wrap) ->
            Box(Modifier.fillMaxWidth().height(60.dp).padding(top = 9.dp)) {
                if (present) {
                    wrap {
                        KvadrantSurface(Modifier.fillMaxSize()) {
                            KvadrantText(label, Modifier.align(Alignment.Center), cyrillic = cyrillic)
                        }
                    }
                }
            }
        }
        KvadrantButton("назад", onBack, Modifier.padding(top = 18.dp), cyrillic = cyrillic)
    }
}

/** The roll's six hundred, which is the longest of the four and therefore the one to wait for. */
private const val ROLL_MILLIS = 600L
