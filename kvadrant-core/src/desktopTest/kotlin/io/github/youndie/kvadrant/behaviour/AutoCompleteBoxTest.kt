package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantAutoCompleteBox
import io.github.youndie.kvadrant.components.KvadrantTextBox
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the autocomplete box offers, and when it offers nothing.
 *
 * [B-43](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-43-the-toolkit-was-never-inventoried.md).
 * Both of this control's defaults are transcribed rather than chosen — `FilterMode.StartsWith` and
 * `MinimumPrefixLength = 1`, each out of its property's own metadata in the Toolkit — so both are
 * asserted here. A default that came from a document and a default somebody liked look identical in
 * a signature.
 */
@OptIn(ExperimentalTestApi::class)
class AutoCompleteBoxTest {
    @Test
    fun nothing_is_offered_until_a_character_is_typed() {
        // `MinimumPrefixLength` is 1, so an empty field offers nothing at all — not "everything",
        // which is what a list with no filter would do and what a reader expects of a control that
        // has all the candidates in hand.
        assertEquals(emptyList(), offeredFor(""))
        assertEquals(listOf("Amsterdam"), offeredFor("a"))
    }

    @Test
    fun the_default_filter_matches_the_start_and_ignores_case() {
        assertEquals(listOf("Berlin"), offeredFor("BER"))
        // `StartsWith`, not `Contains`: "lin" is inside Berlin and Dublin and offers neither.
        assertEquals(emptyList(), offeredFor("lin"))
    }

    @Test
    fun choosing_a_suggestion_hands_back_the_whole_of_it() {
        var chosen: String? = null
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantAutoCompleteBox(
                        value = "co",
                        onValueChange = {},
                        suggestions = CITIES,
                        onSuggestionSelect = { chosen = it },
                    )
                }
            }
            onNodeWithText("Copenhagen").performClick()
        }
        assertEquals("Copenhagen", chosen, "the row handed back something other than its own text")
    }

    @Test
    fun the_text_box_action_icon_is_a_target_larger_than_the_glyph() {
        var taps = 0
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantTextBox(
                        value = "",
                        onValueChange = {},
                        actionIcon = { Box(Modifier.size(19.5.dp).testTag(GLYPH)) },
                        onActionIconClick = { taps++ },
                    )
                }
            }
            // Unmerged, because the field merges its subtree: the glyph is a Box with no semantics
            // of its own and the tag rides on it rather than on the field.
            val glyph = onNodeWithTag(GLYPH, useUnmergedTree = true).fetchSemanticsNode().size
            // 26 px against 84 × 72, at 0.75: what the eye sees is a third of what a thumb hits,
            // which is the relationship the app bar's rings have too.
            assertTrue(glyph.width <= 20, "the glyph is ${glyph.width} px wide, which is not 19.5 dp")

            onNodeWithTag(GLYPH, useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag(GLYPH, useUnmergedTree = true).performClick()
        }
        assertEquals(1, taps, "the action icon's target did not take a tap")
    }

    private fun offeredFor(typed: String): List<String> {
        val offered = mutableListOf<String>()
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantAutoCompleteBox(
                        value = typed,
                        onValueChange = {},
                        suggestions = CITIES,
                        onSuggestionSelect = {},
                        modifier = Modifier.testTag(TAG),
                    )
                }
            }
            offered +=
                CITIES.filter { city ->
                    onAllNodesWithText(city).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
                }
        }
        return offered
    }

    private companion object {
        const val TAG = "autocomplete"
        const val GLYPH = "glyph"
        val CITIES = listOf("Amsterdam", "Berlin", "Copenhagen", "Dublin")
    }
}
