package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantListPicker
import io.github.youndie.kvadrant.components.KvadrantLoopingSelector
import io.github.youndie.kvadrant.components.KvadrantPickerPage
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantTileBadge
import io.github.youndie.kvadrant.components.KvadrantToast
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

@Composable
private fun Themed(
    colors: KvadrantColors = KvadrantColors.dark(),
    content: @Composable () -> Unit,
) {
    KvadrantTheme(colors = colors, typography = portableTypography(kvadrantLatin())) {
        Box(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) { content() }
    }
}

/** A picker fills a page, because on the phone it *was* a page. */
@ViddikScreenshot(name = "date picker", group = "picker", width = 400, height = 500)
@Composable
internal fun DatePicker() {
    val cyrillic = kvadrantCyrillic()
    Themed {
        Column(Modifier.padding(9.dp)) {
            KvadrantText("выберите дату", style = KvadrantTheme.typography.large, cyrillic = cyrillic)
            KvadrantPickerPage(visible = true, modifier = Modifier.padding(top = 12.dp)) {
                KvadrantLoopingSelector(listOf("25", "26", "27"), 1, {}, label = "день", cyrillic = cyrillic)
                KvadrantLoopingSelector(listOf("июл", "авг", "сен"), 1, {}, label = "месяц", cyrillic = cyrillic)
            }
        }
    }
}

@ViddikScreenshot(name = "list picker", group = "picker", width = 400, height = 400)
@Composable
internal fun ListPicker() {
    val cyrillic = kvadrantCyrillic()
    Themed {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            KvadrantListPicker(
                items = listOf("никогда", "каждые 15 минут", "каждый час"),
                selectedIndex = 1,
                onSelect = {},
                expanded = true,
                header = "загружать почту",
                cyrillic = cyrillic,
            )
        }
    }
}

@ViddikScreenshot(name = "toast and badge", group = "picker", width = 400, height = 380)
@Composable
internal fun ToastAndBadge() {
    val cyrillic = kvadrantCyrillic()
    Themed {
        Column {
            // From the top, over the status bar — not from the bottom, where the app bar lives.
            KvadrantToast(
                visible = true,
                title = "Анна Петрова",
                message = "встреча в четверг в 14:00",
                cyrillic = cyrillic,
            )
            TileRow(Modifier.padding(9.dp)) {
                KvadrantTile(TileSize.Medium) {
                    KvadrantTileBadge(7)
                    KvadrantText(
                        "почта",
                        Modifier.align(Alignment.BottomStart).padding(9.dp),
                        KvadrantTheme.typography.normal.copy(color = KvadrantTheme.colors.onAccent),
                        cyrillic,
                    )
                }
                KvadrantTile(TileSize.Small, color = KvadrantAccents.Crimson) {
                    KvadrantTileBadge(214)
                }
            }
        }
    }
}
