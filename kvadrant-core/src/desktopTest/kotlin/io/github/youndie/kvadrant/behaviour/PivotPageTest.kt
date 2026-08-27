package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Two things a pivot page owes its caller, both of which were quietly missing.
 *
 * They were fixed once, the edit did not land, and the item was marked done anyway. These exist to
 * make the claim checkable instead of asserted.
 */
@OptIn(ExperimentalTestApi::class)
class PivotPageTest {
    @Test
    fun the_caller_is_given_page_numbers_it_can_use() {
        val seen = mutableSetOf<Int>()
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantPivot(titles = listOf("a", "b", "c")) { page ->
                        seen += page
                        Box(Modifier.size(40.dp))
                    }
                }
            }
            waitForIdle()
        }
        // The pager counts from the middle of a virtual range; the caller must never see that, or
        // every `when (page)` falls through to its else branch.
        assertTrue(seen.isNotEmpty(), "no page was composed at all")
        assertTrue(seen.all { it in 0..2 }, "pages should arrive as 0..2, but the caller saw $seen")
    }

    @Test
    fun a_page_taller_than_the_screen_actually_moves_when_scrolled() =
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    KvadrantPivot(titles = listOf("a")) {
                        Column {
                            // Far taller than any test window, so the bottom is certainly off-screen
                            // and the assertion cannot pass because it happened to fit.
                            Box(Modifier.size(2000.dp))
                            Box(Modifier.size(40.dp).testTag("bottom"))
                        }
                    }
                }
            }
            waitForIdle()

            // `performScrollTo` alone proves nothing: it finds the pager as a scrollable ancestor and
            // quietly does nothing, so this passed with the vertical scroll removed. What has to be
            // asserted is that the content moved.
            val before = onNodeWithTag("bottom").fetchSemanticsNode().positionInRoot.y
            onNodeWithTag("bottom").performScrollTo()
            waitForIdle()
            val after = onNodeWithTag("bottom").fetchSemanticsNode().positionInRoot.y

            assertTrue(after < before - 1f, "scrolling should have brought the bottom up; it stayed at $before")
        }
}
