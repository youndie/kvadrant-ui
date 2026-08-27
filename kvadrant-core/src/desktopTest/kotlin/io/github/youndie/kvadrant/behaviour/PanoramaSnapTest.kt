package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.theme.KvadrantTypography
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Letting go of a pan leaves a section's header at the left margin, not wherever momentum stopped.
 *
 * `ff941126`: a vertical `PanoramaItem` "will snap only to the left side of the screen during a
 * gesture movement". This asserts the thing a person sees rather than the scroll offset behind it —
 * an offset assertion would need the section boundaries, which are the very numbers under test.
 */
@OptIn(ExperimentalTestApi::class)
class PanoramaSnapTest {
    private val headers = listOf("one", "two", "three")

    /**
     * Tall on purpose.
     *
     * A swipe is dispatched at the node's centre, and with one-line bodies the scrolling row sits
     * in the top third of the box: the gesture landed below it, nothing moved, and that reads
     * exactly like a snap refusing to fire.
     */
    @Composable
    private fun Body(
        width: Dp,
        label: String,
    ) {
        Box(Modifier.width(width).height(400.dp)) { KvadrantText(label) }
    }

    @Test
    fun a_section_wider_than_the_screen_can_be_looked_at() {
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = KvadrantTypography.default(kvadrantLatin()),
                ) {
                    scroll = remember { ScrollState(0) }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
                            scroll = scroll,
                            sections =
                                listOf(
                                    // Two and a quarter screens of content in one section.
                                    headers[0] to { Body(900.dp, "alpha") },
                                    headers[1] to { Body(220.dp, "beta") },
                                    headers[2] to { Body(340.dp, "gamma") },
                                ),
                        )
                    }
                }
            }
            waitForIdle()

            val firstSectionEnds = onAllNodesWithText(headers[1])[0].getUnclippedBoundsInRoot().left.value
            // A **pan**, which is the word the source uses, rather than a flick: slow enough that
            // the release carries almost no velocity. A hard throw travelling past a whole section
            // is correct and is what the other test does; this one is a person dragging the wide
            // section's content across to look at the rest of it.
            onNodeWithTag("pano").performTouchInput { swipeLeft(durationMillis = 1000) }
            waitForIdle()

            // The second header has not reached the margin, so the release did not throw the
            // panorama past a section whose content is off the screen — which is what a single
            // left-edge stop per section does, and what this fixture exists to catch.
            val secondHeader = onAllNodesWithText(headers[1])[0].getUnclippedBoundsInRoot().left.value
            assertTrue(scroll.value > 0, "the swipe did not scroll")
            assertTrue(
                secondHeader > 18f,
                "settled at ${scroll.value} with the next section already at the margin: the wide " +
                    "one was skipped rather than panned (it started at $firstSectionEnds)",
            )
            // And it still settled on a stop rather than wherever momentum died: the wide section's
            // right edge against the right of the viewport.
            val alpha = onAllNodesWithText(headers[0])[0].getUnclippedBoundsInRoot().left.value
            assertTrue(
                abs(alpha - (18f - (900f + 27f - 400f))) < 2f,
                "settled at ${scroll.value}; the wide section's header is at $alpha, which is " +
                    "neither its left edge nor its right edge against the viewport",
            )
        }
    }

    @Test
    fun a_release_settles_with_a_header_at_the_margin() {
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = KvadrantTypography.default(kvadrantLatin()),
                ) {
                    scroll = remember { ScrollState(0) }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
                            scroll = scroll,
                            sections =
                                listOf(
                                    headers[0] to { Body(500.dp, "alpha") },
                                    headers[1] to { Body(220.dp, "beta") },
                                    headers[2] to { Body(340.dp, "gamma") },
                                ),
                        )
                    }
                }
            }
            waitForIdle()

            // Section padding 9 plus header padding 9, both from the template's own margins.
            val margin = 18.dp

            // Every section is laid out twice — one copy to look at, one to wrap into — so a
            // header matches two nodes, and either of them on the margin is a settled panorama.
            fun lefts(header: String): List<Float> =
                onAllNodesWithText(header).fetchSemanticsNodes().indices.map { copy ->
                    onAllNodesWithText(header)[copy].getUnclippedBoundsInRoot().left.value
                }

            fun headerAtMargin(): String? =
                headers.firstOrNull { header -> lefts(header).any { abs(it - margin.value) < 2f } }

            assertTrue(headerAtMargin() != null, "the panorama does not start on a section")

            val before = scroll.value
            onNodeWithTag("pano").performTouchInput { swipeLeft() }
            waitForIdle()

            // The positive control: a swipe that moved nothing would satisfy the assertion below,
            // because the panorama already starts on a section boundary.
            assertTrue(scroll.value != before, "the swipe did not scroll: still at $before")
            assertTrue(
                headerAtMargin() != null,
                "settled at ${scroll.value} with no header at the margin: " +
                    headers.joinToString { "$it@${lefts(it)}" },
            )
        }
    }
}
