package io.github.youndie.kvadrant.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantContextMenuHost
import io.github.youndie.kvadrant.components.KvadrantGroupHeader
import io.github.youndie.kvadrant.components.KvadrantJumpList
import io.github.youndie.kvadrant.components.KvadrantListItem
import io.github.youndie.kvadrant.components.KvadrantPageHeader
import io.github.youndie.kvadrant.components.KvadrantProgressBar
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantCyrillic
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import ru.workinprogress.viddik.annotations.ViddikScreenshot

private val Groups =
    listOf(
        "А" to listOf("Анна Петрова" to "встреча в четверг", "Артём" to "перезвонит"),
        "Д" to listOf("Дмитрий" to "документы отправил"),
        "B" to listOf("build server" to "nightly is green"),
    )

@Composable
private fun Contacts(
    colors: KvadrantColors,
    content: @Composable () -> Unit,
) {
    KvadrantTheme(colors = colors, typography = KvadrantTypography.default(kvadrantLatin())) {
        Column(Modifier.fillMaxSize().background(KvadrantTheme.colors.background)) { content() }
    }
}

@ViddikScreenshot(name = "contacts", group = "list", width = 400, height = 560)
@Composable
internal fun ContactsList() {
    val cyrillic = kvadrantCyrillic()
    Contacts(KvadrantColors.dark()) {
        KvadrantPageHeader("KVADRANT UI", "контакты", cyrillic = cyrillic)
        Column(Modifier.padding(horizontal = 9.dp)) {
            KvadrantProgressBar(0.4f, Modifier.padding(bottom = 12.dp))
            Groups.forEach { (letter, people) ->
                KvadrantGroupHeader(letter, cyrillic = cyrillic)
                people.forEach { (name, note) ->
                    KvadrantListItem(name, subtitle = note, onClick = {}, cyrillic = cyrillic)
                }
            }
        }
    }
}

/** The jump list a long list opens: live letters in the accent, dead ones kept in place. */
@ViddikScreenshot(name = "jump list", group = "list", width = 400, height = 460)
@Composable
internal fun JumpList() {
    Contacts(KvadrantColors.dark()) {
        KvadrantJumpList(
            letters = listOf("А", "Б", "В", "Г", "Д", "Е", "Ж", "З", "A", "B", "C", "D"),
            enabled = setOf("А", "Д", "B"),
            cyrillic = kvadrantCyrillic(),
        )
    }
}

/**
 * The context menu with the page pushed back to 0.94 behind it — sharp, not blurred, which is the
 * detail nearly every reimplementation gets wrong.
 */
@ViddikScreenshot(name = "context menu", group = "list", width = 400, height = 560)
@Composable
internal fun ContextMenu() {
    val cyrillic = kvadrantCyrillic()
    Contacts(KvadrantColors.dark()) {
        KvadrantContextMenuHost(
            expanded = true,
            items = listOf("закрепить на экране", "удалить", "изменить"),
            onDismiss = {},
            onItemClick = {},
            cyrillic = cyrillic,
        ) {
            Column {
                KvadrantPageHeader("KVADRANT UI", "контакты", cyrillic = cyrillic)
                Column(Modifier.padding(horizontal = 9.dp)) {
                    Groups.forEach { (letter, people) ->
                        KvadrantGroupHeader(letter, cyrillic = cyrillic)
                        people.forEach { (name, note) ->
                            KvadrantListItem(name, subtitle = note, cyrillic = cyrillic)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The feather's geometry, held still: every row on **one** axis off to the left, not each about its
 * own centre. Held at three angles because a still of a stagger is otherwise indistinguishable from
 * a still of nothing happening.
 */
@ViddikScreenshot(name = "turnstile feather axis", group = "list", width = 400, height = 400)
@Composable
internal fun FeatherAxis() {
    val cyrillic = kvadrantCyrillic()
    Contacts(KvadrantColors.dark()) {
        Column(
            Modifier.fillMaxSize().padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            listOf(0f, -25f, -50f, -80f).forEachIndexed { index, angle ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = angle
                            transformOrigin = TransformOrigin(-0.2f, 0.5f)
                            cameraDistance = 8f
                        }.background(KvadrantTheme.colors.chrome)
                        .padding(9.dp),
                ) {
                    KvadrantText(
                        "row $index · ${angle.toInt()}°",
                        style = KvadrantTheme.typography.normal,
                        cyrillic = cyrillic,
                    )
                }
            }
        }
    }
}
