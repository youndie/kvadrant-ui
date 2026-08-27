package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantListItem
import io.github.youndie.kvadrant.components.KvadrantMessageBox
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.components.KvadrantProgressDots
import io.github.youndie.kvadrant.components.KvadrantTextBox
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
import io.github.youndie.kvadrant.theme.KvadrantTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

@Composable
private fun Themed(
    colors: KvadrantColors,
    content: @Composable () -> Unit,
) {
    KvadrantTheme(colors = colors, typography = KvadrantTypography.default(kvadrantLatin())) {
        Box(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) { content() }
    }
}

@Composable
private fun Gallery(colors: KvadrantColors) {
    val cyrillic = kvadrantCyrillic()
    Themed(colors) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            KvadrantText("controls", style = KvadrantTheme.typography.extraLarge, cyrillic = cyrillic)

            KvadrantTextBox("", {}, Modifier.fillMaxWidth(), "введите имя", cyrillic)
            KvadrantTextBox("готово", {}, Modifier.fillMaxWidth(), cyrillic = cyrillic)

            Row(
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KvadrantButton("сохранить", {}, cyrillic = cyrillic)
                KvadrantToggleSwitch(true, {})
                KvadrantToggleSwitch(false, {})
            }

            KvadrantProgressDots()

            Column {
                KvadrantListItem("Анна Петрова", subtitle = "встреча в четверг", onClick = {}, cyrillic = cyrillic)
                KvadrantListItem("build server", subtitle = "nightly is green", onClick = {}, cyrillic = cyrillic)
            }

            TileRow {
                KvadrantTile(TileSize.Small, color = KvadrantAccents.Emerald)
                KvadrantTile(TileSize.Small, color = KvadrantAccents.Amber)
                KvadrantTile(TileSize.Small, color = KvadrantAccents.Crimson)
            }
        }
    }
}

@ViddikScreenshot(name = "controls dark", group = "gallery", width = 400, height = 620)
@Composable
internal fun GalleryDark(): Unit = Gallery(KvadrantColors.dark())

@ViddikScreenshot(name = "controls light", group = "gallery", width = 400, height = 620)
@Composable
internal fun GalleryLight(): Unit = Gallery(KvadrantColors.light())

@ViddikScreenshot(name = "message box", group = "gallery", width = 400, height = 400)
@Composable
internal fun MessageBox() {
    val cyrillic = kvadrantCyrillic()
    Themed(KvadrantColors.dark()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            KvadrantMessageBox(
                // The fixture holds it open. `visible` also drives a swivel now, and a still frame is
                // taken once the transition has settled.
                visible = true,
                title = "удалить письмо?",
                message = "оно будет удалено с телефона и с сервера.",
                onConfirm = {},
                onCancel = {},
                cyrillic = cyrillic,
            )
        }
    }
}

// There is no turnstile fixture. It was pointed at the real component — the previous one guarded a
// hand-written copy of its `graphicsLayer` block — and then the recording showed why a picture
// cannot do this job: the component's two *resting* states are "square on and opaque" and "fully
// transparent", so the goldens came back as a plain rectangle and a black square. The rotation only
// exists between them. `TurnstileTest` stops the clock in between instead.

@ViddikScreenshot(name = "panorama", group = "gallery", width = 400, height = 500)
@Composable
internal fun Panorama() {
    val cyrillic = kvadrantCyrillic()
    Themed(KvadrantColors.dark()) {
        KvadrantPanorama(
            title = "почта",
            cyrillic = cyrillic,
            sections =
                listOf(
                    "входящие" to {
                        Column {
                            KvadrantListItem("Анна Петрова", subtitle = "встреча в четверг", cyrillic = cyrillic)
                            KvadrantListItem("Дмитрий", subtitle = "документы отправил", cyrillic = cyrillic)
                        }
                    },
                    "отправленные" to {
                        TileRow { KvadrantTile(TileSize.Small, color = KvadrantAccents.Steel) }
                    },
                ),
        )
    }
}
