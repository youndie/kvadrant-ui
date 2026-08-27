package io.github.youndie.kvadrant.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * [initialScale] is the metric scale the demo opens at, and it is a parameter because the right
 * answer differs per platform rather than per taste: Metro's numbers were drawn for a 480 px phone,
 * so a phone wants 1.0 and a desktop window wants them scaled up. See `KvadrantMetrics.scaled`.
 */
@Composable
public fun KvadrantSampleApp(initialScale: Float) {
    var dark by remember { mutableStateOf(true) }
    var accessible by remember { mutableStateOf(false) }
    var accent by remember { mutableStateOf(KvadrantAccents.Cyan) }
    var menuOpen by remember { mutableStateOf(false) }
    var density by remember { mutableStateOf(initialScale) }

    val base = if (dark) KvadrantColors.dark(accent) else KvadrantColors.light(accent)
    val colors = if (accessible) base.accessible() else base

    val cyrillic = kvadrantCyrillic()

    // Metro's numbers were drawn for a 480 px phone. On a desktop window they read as cramped, so
    // the whole metric set is scaled by one factor rather than the margin being nudged alone.
    KvadrantTheme(
        colors = colors,
        typography = KvadrantTypography.default(kvadrantLatin()),
        metrics = KvadrantMetrics().scaled(density),
    ) {
        // A Metro page always paints PhoneBackgroundBrush. Nothing below does it — a Pivot is a
        // control, not a page — so the root has to, or the window shows through white.
        // The background is painted edge to edge and the content is inset, in that order. Painting
        // only the safe area leaves the status bar showing the platform's own colour, and insetting
        // nothing puts the Pivot header under the clock — which is what the first Android build did.
        // `safeDrawing` is zero on the desktop, so this is one expression for both.
        Column(
            Modifier
                .fillMaxSize()
                .background(KvadrantTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            KvadrantPivot(
                titles = listOf("start", "почта", "settings"),
                title = "KVADRANT UI",
                cyrillic = cyrillic,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> {
                        StartPage(cyrillic)
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
                            density = density,
                            onDensity = { density = it },
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
        }
    }
}

@Composable
private fun StartPage(cyrillic: androidx.compose.ui.text.font.FontFamily) {
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
        KvadrantTile(size, color = colours[index]) {
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "Анна Петрова" to "встреча в четверг",
            "build server" to "nightly is green",
            "Дмитрий" to "документы отправил",
        ).forEach { (who, what) ->
            KvadrantListItem(who, subtitle = what, onClick = {}, cyrillic = cyrillic)
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
