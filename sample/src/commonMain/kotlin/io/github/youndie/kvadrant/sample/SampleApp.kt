package io.github.youndie.kvadrant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantAppBar
import io.github.youndie.kvadrant.components.KvadrantAppBarButton
import io.github.youndie.kvadrant.components.KvadrantAppBarGlyphSize
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantListItem
import io.github.youndie.kvadrant.components.KvadrantListPicker
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.components.KvadrantProgressBar
import io.github.youndie.kvadrant.components.KvadrantProgressDots
import io.github.youndie.kvadrant.components.KvadrantRadioButton
import io.github.youndie.kvadrant.components.KvadrantSlider
import io.github.youndie.kvadrant.components.KvadrantTextBox
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantTileBadge
import io.github.youndie.kvadrant.components.KvadrantTileGrid
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.components.KvadrantTurnstile
import io.github.youndie.kvadrant.components.KvadrantTurnstileFeather
import io.github.youndie.kvadrant.components.TileSize
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantMetrics
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import io.github.youndie.kvadrant.theme.accessible
import io.github.youndie.kvadrant.theme.scaled
import io.github.youndie.kvadrant.theme.scaledToWidth

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

/**
 * The whole demo, in one composable, so that a desktop window and an Android activity show the same
 * thing rather than two things that drift.
 *
 * There is no per-platform scale to pass in: the metric set is derived from the width it is given,
 * the way Windows Phone scaled its 480-pixel canvas to WVGA, WXGA and 720p without reflowing
 * anything. A phone and a desktop window differ in how much canvas they have, not in what the
 * layout is. The slider then moves that derived scale, which is what it was always for.
 */
@Composable
public fun KvadrantSampleApp() {
    var dark by remember { mutableStateOf(true) }
    var accessible by remember { mutableStateOf(false) }
    var accent by remember { mutableStateOf(KvadrantAccents.Cyan) }
    var menuOpen by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf<Float?>(null) }
    // The demo's one piece of navigation, and it exists so that the turnstile has somewhere to
    // play: it is a *page* transition, not something a Pivot does between its own items — the phone
    // slid those sideways. Without a page to enter, `KvadrantTurnstile` was code nobody called.
    var openTile by remember { mutableStateOf<String?>(null) }

    val base = if (dark) KvadrantColors.dark(accent) else KvadrantColors.light(accent)
    val colors = if (accessible) base.accessible() else base

    val cyrillic = kvadrantCyrillic()

    // A Metro page always paints PhoneBackgroundBrush. Nothing below does it — a Pivot is a control,
    // not a page — so the root has to, or the window shows through white. Edge to edge, and the
    // content inset inside it: painting only the safe area leaves the status bar showing the
    // platform's own colour, and insetting nothing puts the Pivot header under the clock.
    // `safeDrawing` is zero on the desktop, so one expression serves both.
    Box(Modifier.fillMaxSize().background(colors.background)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // The scale is measured, not chosen: Metro's canvas is 480 px wide and the whole of it
            // is stretched to whatever the surface has, exactly as WVGA, WXGA and 720p ran the same
            // layout at three sizes. This is what stops a phone from showing a strip of unused
            // black beside a two-column tile grid. The slider below then moves this number, which
            // is the only thing it was ever for.
            val fitted = KvadrantMetrics().scaledToWidth(maxWidth)

            KvadrantTheme(
                colors = colors,
                typography = KvadrantTypography.default(kvadrantLatin()),
                metrics = scale?.let { KvadrantMetrics().scaled(it) } ?: fitted,
            ) {
                Column(Modifier.fillMaxSize()) {
                    KvadrantPivot(
                        titles = listOf("start", "почта", "settings"),
                        title = "KVADRANT UI",
                        cyrillic = cyrillic,
                        modifier = Modifier.weight(1f),
                    ) { page ->
                        when (page) {
                            0 -> {
                                StartPage(cyrillic, onOpen = { openTile = it })
                            }

                            1 -> {
                                MailPage(cyrillic)
                            }

                            else -> {
                                SettingsPage(
                                    dark = dark,
                                    onDark = { dark = it },
                                    accessible = accessible,
                                    onAccessible = { accessible = it },
                                    accent = accent,
                                    onAccent = { accent = it },
                                    density = scale ?: (fitted.margin / KvadrantMetrics().margin),
                                    onDensity = { scale = it },
                                    cyrillic = cyrillic,
                                )
                            }
                        }
                    }
                    KvadrantAppBar(
                        menuItems = listOf("Настройки", "О программе"),
                        menuExpanded = menuOpen,
                        onMenuToggle = { menuOpen = !menuOpen },
                        cyrillic = cyrillic,
                    ) {
                        listOf(KvadrantAccents.Cyan, KvadrantAccents.Emerald, KvadrantAccents.Amber)
                            .forEach { colour ->
                                KvadrantAppBarButton(onClick = {}) {
                                    // A stand-in until there is an icon set (B-18), and it has to be sized
                                    // to `KvadrantAppBarGlyphSize` — the ring is 36 dp and a Small tile is
                                    // 74.25, so a tile here draws a square straight through the circle the
                                    // button is made of.
                                    Box(
                                        Modifier
                                            .size(KvadrantAppBarGlyphSize)
                                            .clip(CircleShape)
                                            .background(colour),
                                    )
                                }
                            }
                    }

                    // The page, over everything, entering and leaving on the turnstile. Both are
                    // composed at once on purpose: an exit that unmounts the moment the flag flips
                    // never plays, and that is the ordinary way a leaving animation is lost.
                    KvadrantTurnstile(visible = openTile == null) {}
                    KvadrantTurnstile(
                        visible = openTile != null,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val name = openTile
                        if (name != null) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .background(KvadrantTheme.colors.background)
                                    .clickable { openTile = null }
                                    .padding(KvadrantTheme.metrics.margin),
                            ) {
                                KvadrantText(
                                    "KVADRANT UI",
                                    style = KvadrantTheme.typography.pageTitle,
                                )
                                KvadrantText(
                                    name,
                                    style = KvadrantTheme.typography.pivotHeader,
                                    cyrillic = cyrillic,
                                )
                                KvadrantText(
                                    "нажмите, чтобы вернуться",
                                    style =
                                        KvadrantTheme.typography.normal.copy(
                                            color = KvadrantTheme.colors.subtle,
                                        ),
                                    cyrillic = cyrillic,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartPage(
    cyrillic: androidx.compose.ui.text.font.FontFamily,
    onOpen: (String) -> Unit,
) {
    val labels = mapOf(0 to "почта", 3 to "календарь", 6 to "фото")
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
    KvadrantTileGrid(Layout) { index, size ->
        KvadrantTile(size, color = colours[index], onClick = { onOpen(labels[index] ?: "плитка") }) {
            if (index == 0) KvadrantTileBadge(7)
            labels[index]?.let {
                KvadrantText(
                    it,
                    Modifier.align(Alignment.BottomStart).padding(9.dp),
                    KvadrantTheme.typography.normal.copy(color = KvadrantTheme.colors.onAccent),
                    cyrillic,
                )
            }
        }
    }
}

@Composable
private fun MailPage(cyrillic: androidx.compose.ui.text.font.FontFamily) {
    var rowsShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { rowsShown = true }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "Анна Петрова" to "встреча в четверг",
            "build server" to "nightly is green",
            "Дмитрий" to "документы отправил",
        ).forEachIndexed { index, (who, what) ->
            // The feather, which is the turnstile for a list: one axis down the screen and the rows
            // 40 ms apart. It starts hidden and is shown from a LaunchedEffect because a component
            // whose `visible` is already true at first composition has nothing to animate — the
            // state and the target agree, and the entrance is skipped in exactly the way that makes
            // a transition look like it was never wired up.
            KvadrantTurnstileFeather(visible = rowsShown, index = index) {
                // The demo's choice, not the library's and not Microsoft's: the phone's Mail set its
                // sender line above the page default in its own data template, and no dictionary
                // records what it used. A demo imitating Mail has to make that call somewhere visible,
                // which is here rather than inside `KvadrantListItem`.
                KvadrantListItem(
                    who,
                    subtitle = what,
                    onClick = {},
                    cyrillic = cyrillic,
                    titleStyle = KvadrantTheme.typography.mediumLarge,
                )
            }
        }
        KvadrantProgressDots(Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SettingsPage(
    dark: Boolean,
    onDark: (Boolean) -> Unit,
    accessible: Boolean,
    onAccessible: (Boolean) -> Unit,
    accent: androidx.compose.ui.graphics.Color,
    onAccent: (androidx.compose.ui.graphics.Color) -> Unit,
    density: Float,
    onDensity: (Float) -> Unit,
    cyrillic: androidx.compose.ui.text.font.FontFamily,
) {
    var name by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(1) }
    var pickerOpen by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(0.65f) }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KvadrantToggleSwitch(dark, onDark)
            KvadrantText("тёмная тема", Modifier.padding(start = 12.dp), cyrillic = cyrillic)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            KvadrantToggleSwitch(accessible, onAccessible)
            KvadrantText("контрастная палитра", Modifier.padding(start = 12.dp), cyrillic = cyrillic)
        }

        KvadrantText("акцент", cyrillic = cyrillic)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                KvadrantAccents.Cyan,
                KvadrantAccents.Lime,
                KvadrantAccents.Magenta,
                KvadrantAccents.Amber,
                KvadrantAccents.Emerald,
            ).forEach { candidate ->
                KvadrantTile(TileSize.Small, color = candidate, onClick = { onAccent(candidate) })
            }
        }

        KvadrantCheckBox(preview, { preview = it }, label = "показывать превью", cyrillic = cyrillic)
        KvadrantCheckBox(sound, { sound = it }, label = "звук уведомлений", cyrillic = cyrillic)

        KvadrantRadioButton(frequency == 0, { frequency = 0 }, label = "вручную", cyrillic = cyrillic)
        KvadrantRadioButton(frequency == 1, { frequency = 1 }, label = "каждые 15 минут", cyrillic = cyrillic)

        KvadrantListPicker(
            items = listOf("никогда", "каждые 15 минут", "каждый час"),
            selectedIndex = frequency,
            onSelect = {
                frequency = it
                pickerOpen = false
            },
            expanded = pickerOpen,
            onExpandRequest = { pickerOpen = !pickerOpen },
            header = "загружать почту",
            cyrillic = cyrillic,
        )

        KvadrantText("имя", cyrillic = cyrillic)
        KvadrantTextBox(name, { name = it }, Modifier.fillMaxWidth(), "введите имя", cyrillic)

        KvadrantText("масштаб метрик", cyrillic = cyrillic)
        KvadrantSlider((density - 1f) / 1.5f, { onDensity(1f + it * 1.5f) })

        KvadrantText("яркость", cyrillic = cyrillic)
        KvadrantSlider(brightness, { brightness = it })
        KvadrantProgressBar(brightness, Modifier.padding(top = 6.dp))
    }
}
