package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantButton
import io.github.youndie.kvadrant.components.KvadrantCheckBox
import io.github.youndie.kvadrant.components.KvadrantTile
import io.github.youndie.kvadrant.components.KvadrantToggleSwitch
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tab reaches every control, in the order the controls are drawn, and space or enter presses one.
 *
 * [B-40](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-40-keyboard-and-focus-on-desktop-and-wasm.md).
 * The order is asserted against the **measured** positions rather than against the list this test
 * declares: the claim is "focus follows the screen", and comparing the walk with my own array would
 * only assert that I can copy a list twice. Reading the geometry means a layout that reverses while
 * the focus order does not — which is the failure the item names, and the one a refactor causes
 * silently — fails here.
 *
 * A [Row] inside the [Column] is deliberate. A single column cannot tell an order that follows the
 * screen from one that follows the composition, because in a column they are the same sequence.
 */
@OptIn(ExperimentalTestApi::class)
class KeyboardNavigationTest {
    @Test
    fun tab_walks_the_controls_in_the_order_they_are_drawn() =
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    Column {
                        KvadrantButton("go", {}, Modifier.testTag("one"))
                        Row {
                            KvadrantCheckBox(false, {}, Modifier.testTag("two"))
                            KvadrantCheckBox(true, {}, Modifier.testTag("three"))
                        }
                        KvadrantToggleSwitch(false, {}, Modifier.testTag("four"))
                        KvadrantTile(onClick = {}, modifier = Modifier.testTag("five").size(80.dp)) { }
                    }
                }
            }
            waitForIdle()

            val onScreen =
                TAGS
                    .map { tag -> tag to positionOf(tag) }
                    // Reading order: down the page, and left to right within a line. `y` is compared
                    // loosely because two controls in a row are not pixel-aligned — a check box and a
                    // switch have different heights, so their tops differ by a few pixels while they are
                    // plainly on the same line.
                    .sortedWith(compareBy({ (it.second.second / LINE_HEIGHT) }, { it.second.first }))
                    .map { it.first }

            val walked = mutableListOf<String>()
            repeat(TAGS.size) {
                onRoot().performKeyInput { pressKey(Key.Tab) }
                waitForIdle()
                walked += focusedTag() ?: "nothing"
            }
            assertEquals(onScreen, walked, "tab did not follow the controls down the screen")

            // And back. Shift+Tab is not a separate feature — it is the same order read backwards — but
            // it is the half that a hand-rolled `focusProperties` chain gets wrong.
            val back = mutableListOf<String>()
            repeat(TAGS.size - 1) {
                onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
                waitForIdle()
                back += focusedTag() ?: "nothing"
            }
            assertEquals(onScreen.dropLast(1).reversed(), back, "shift+tab did not walk back up")
        }

    @Test
    fun space_and_enter_press_the_tile() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                KvadrantTheme {
                    KvadrantTile(onClick = { clicks++ }, modifier = Modifier.testTag("tile").size(80.dp)) { }
                }
            }
            onRoot().performKeyInput { pressKey(Key.Tab) }
            waitForIdle()
            assertTrue(focusedTag(listOf("tile")) == "tile", "tab did not reach the tile")

            onRoot().performKeyInput { pressKey(Key.Spacebar) }
            waitForIdle()
            assertEquals(1, clicks, "space did not press the tile")

            onRoot().performKeyInput { pressKey(Key.Enter) }
            waitForIdle()
            assertEquals(2, clicks, "enter did not press the tile")
        }

    private fun SemanticsNodeInteractionsProvider.focusedTag(among: List<String> = TAGS): String? =
        among.firstOrNull { tag ->
            onAllNodes(hasTestTag(tag))
                .fetchSemanticsNodes()
                .any { it.config.getOrNull(SemanticsProperties.Focused) == true }
        }

    private fun SemanticsNodeInteractionsProvider.positionOf(tag: String): Pair<Float, Float> {
        val bounds = onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().first().boundsInRoot
        return bounds.left to bounds.top
    }

    private companion object {
        val TAGS = listOf("one", "two", "three", "four", "five")

        /** Coarse enough that two controls of different heights share a line, fine enough that
         * the rows do not merge. */
        const val LINE_HEIGHT = 40f
    }
}
