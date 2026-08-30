package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.components.KvadrantPivot
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The two layouts whose identity is horizontal movement travel the other way when the page does.
 *
 * [B-41](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-41-rtl-is-canon-and-untested.md)
 * asks for goldens of the three custom layouts under `LayoutDirection.Rtl`, and there are three —
 * `rtl/pivot`, `rtl/panorama`, `rtl/page header`. A still cannot answer the other half of the
 * criterion. "The header strip and the panorama travel the correct way" is a claim about a
 * direction, and a picture at rest shows a position.
 *
 * So this scrolls each of them and compares the **sign** of what moved, between one direction and
 * the other. It deliberately asserts nothing about how far: the distances are the same arithmetic
 * either way and are held by `PanoramaFoldTest` and `PivotPageTest`, while the only thing mirroring
 * can get wrong is which way.
 */
@OptIn(ExperimentalTestApi::class)
class MirroredTravelTest {
    @Test
    fun the_panorama_title_drifts_the_other_way_when_the_page_is_mirrored() {
        val leftToRight = titleTravel(LayoutDirection.Ltr)
        val rightToLeft = titleTravel(LayoutDirection.Rtl)

        // The control: it has to move at all, or two zeroes would agree about their sign.
        assertTrue(leftToRight != 0f, "the title did not drift in a left-to-right page")
        assertTrue(rightToLeft != 0f, "the title did not drift in a right-to-left page")

        assertTrue(
            leftToRight < 0f && rightToLeft > 0f,
            "panning forward moved the title by $leftToRight left-to-right and $rightToLeft " +
                "right-to-left. It should follow the content: leftwards on a page that reads that " +
                "way and rightwards on one that does not",
        )
    }

    @Test
    fun the_pivot_strip_travels_the_other_way_too() {
        val leftToRight = headerTravel(LayoutDirection.Ltr)
        val rightToLeft = headerTravel(LayoutDirection.Rtl)

        assertTrue(leftToRight != 0f, "the selected header did not move in a left-to-right page")
        assertTrue(rightToLeft != 0f, "the selected header did not move in a right-to-left page")

        // The strip is placed by hand, and it used `place` rather than `placeRelative` — so before
        // B-41 this number had the same sign in both directions, which is a strip running one way
        // inside a page running the other.
        assertTrue(
            leftToRight < 0f && rightToLeft > 0f,
            "the selected header moved by $leftToRight left-to-right and $rightToLeft " +
                "right-to-left, so the strip is not following the page it is on",
        )
    }

    /** How far the panorama's title moves when the page is panned forward, signed. */
    private fun titleTravel(direction: LayoutDirection): Float {
        var travel = 0f
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    KvadrantTheme {
                        scroll = remember { ScrollState(0) }
                        Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(TAG)) {
                            KvadrantPanorama(
                                title = TITLE,
                                scroll = scroll,
                                sections =
                                    listOf(
                                        "one" to { KvadrantText("alpha") },
                                        "two" to { KvadrantText("beta") },
                                    ),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            val before = onNodeWithText(TITLE).fetchSemanticsNode().positionInRoot.x
            runBlocking { scroll.scrollTo(scroll.value + PAN) }
            waitForIdle()
            travel = onNodeWithText(TITLE).fetchSemanticsNode().positionInRoot.x - before
        }
        return travel
    }

    /** How far the pivot's selected header moves when the pager turns a page, signed. */
    private fun headerTravel(direction: LayoutDirection): Float {
        var travel = 0f
        runComposeUiTest {
            lateinit var pager: androidx.compose.foundation.pager.PagerState
            setContent {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    KvadrantTheme {
                        pager =
                            io.github.youndie.kvadrant.components
                                .rememberKvadrantPivotState(TITLES.size)
                        Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(TAG)) {
                            KvadrantPivot(titles = TITLES, state = pager) { KvadrantText("page $it") }
                        }
                    }
                }
            }
            waitForIdle()
            val before = headerStripAt()
            // **A fraction of a page and not a whole one.** A full turn moves a header by its own
            // width, and the search below picks the copy nearest the centre — over a whole turn
            // that can become a *different* copy, which reads as a jump of most of the frame.
            runBlocking { pager.scrollToPage(pager.currentPage, PART_OF_A_PAGE) }
            waitForIdle()
            travel = headerStripAt() - before
        }
        return travel
    }

    /**
     * The average x of every copy of the first header.
     *
     * The strip lays its titles out **three times** — the spare copies either side are what let it
     * wrap — so a search by text finds three nodes and the plain accessor refuses. Picking one of
     * them by position was tried twice and is a trap: whichever rule chooses "the interesting copy"
     * can choose a *different* copy after the strip has moved, and the delta then reads as a jump
     * of most of the frame. An average needs no such rule, because every copy shifts by the same
     * amount as the strip.
     */
    private fun androidx.compose.ui.test.ComposeUiTest.headerStripAt(): Float =
        onAllNodesWithText(TITLES.first())
            .fetchSemanticsNodes()
            .map { it.positionInRoot.x }
            .average()
            .toFloat()

    private companion object {
        const val TAG = "frame"
        const val WIDTH = 400
        const val HEIGHT = 600
        const val TITLE = "panorama"
        const val PAN = 120

        /** Enough of a turn to move the strip visibly, too little to change which copy is nearest. */
        const val PART_OF_A_PAGE = 0.4f
        val TITLES = listOf("start", "mail", "settings")
    }
}
