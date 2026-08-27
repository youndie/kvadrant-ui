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
import androidx.compose.ui.unit.Dp
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
internal fun ContextMenu(): Unit = ContextMenuAt(null)

/**
 * The same menu opened *on a row*, which is the branch that matters and the one that was wrong.
 *
 * `AdjustContextMenuPositionForPortraitMode` puts the menu directly below the pressed item when it
 * fits there, above it when it fits there instead, and at the bottom of the page when neither is
 * true — which is also where it goes with no anchor at all, the case the original reaches by opening
 * the menu from code. Both branches have a picture now; before, everything went to the top.
 */
@ViddikScreenshot(name = "context menu on a row", group = "list", width = 400, height = 560)
@Composable
internal fun ContextMenuOnRow(): Unit = ContextMenuAt(200.dp)

@Composable
private fun ContextMenuAt(anchorTop: Dp?) {
    val cyrillic = kvadrantCyrillic()
    Contacts(KvadrantColors.dark()) {
        KvadrantContextMenuHost(
            expanded = true,
            items = listOf("закрепить на экране", "удалить", "изменить"),
            onDismiss = {},
            onItemClick = {},
            cyrillic = cyrillic,
            anchorTop = anchorTop,
            anchorHeight = 42.dp,
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

// No feather fixture either, and for the same reason plus one of its own: at rest with `visible`
// false every row is fully transparent, and the stagger that makes it a feather — 40 ms in, 50 ms
// out — is in time rather than in angle. The fixture that used to stand here faked it by drawing
// four rows at four different angles, which is a thing the component never does at any instant.
