package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /**
     * It opens on a section even when the content is not measured on the first frame.
     *
     * **It does not reproduce what a phone showed, and that is worth writing down rather than
     * implying otherwise.** A device opened this panorama part-way through its second section; the
     * suspected mechanism is `scrollTo` clamping to a `maxValue` that had not arrived yet, with
     * nothing re-running the move because the fold watched only the position and the gesture. That
     * mechanism is closed — the limit is in the flow now, and the fold normalises by modulo instead
     * of one subtraction — but this test passes with the old code too, so it is evidence of an
     * invariant holding and not of that defect being the one.
     */
    @Test
    fun it_opens_on_a_section_even_when_the_width_arrives_late() {
        runComposeUiTest {
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    var ready by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { ready = true }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
                            sections =
                                headers.mapIndexed { index, header ->
                                    header to
                                        {
                                            // Nothing on the first frame, its real width on the
                                            // next, which is what a grid of tiles or an image does.
                                            if (ready) Body((240 + index * 60).dp, "body") else Unit
                                        }
                                },
                        )
                    }
                }
            }
            waitForIdle()

            val left = onAllNodesWithText(headers[0])[0].getUnclippedBoundsInRoot().left.value
            val opened =
                headers.firstOrNull { header ->
                    onAllNodesWithText(header).fetchSemanticsNodes().indices.any { copy ->
                        abs(onAllNodesWithText(header)[copy].getUnclippedBoundsInRoot().left.value - 18f) < 2f
                    }
                }
            assertEquals(headers[0], opened, "opened part-way through a section; first header at $left")
        }
    }

    /** Section after section, round and round, with no end to arrive at in either direction. */
    @Test
    fun the_sections_cycle_without_running_out() {
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    scroll = remember { ScrollState(0) }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
                            scroll = scroll,
                            sections =
                                listOf(
                                    headers[0] to { Body(300.dp, "alpha") },
                                    headers[1] to { Body(220.dp, "beta") },
                                    headers[2] to { Body(340.dp, "gamma") },
                                ),
                        )
                    }
                }
            }
            waitForIdle()

            fun lefts(header: String): List<Float> =
                onAllNodesWithText(header).fetchSemanticsNodes().indices.map { copy ->
                    onAllNodesWithText(header)[copy].getUnclippedBoundsInRoot().left.value
                }

            fun headerAtMargin(): String? = headers.firstOrNull { header -> lefts(header).any { abs(it - 18f) < 2f } }

            fun step(forward: Boolean): String? {
                onNodeWithTag("pano").performTouchInput {
                    if (forward) {
                        swipeLeft(startX = right, endX = right - 250f, durationMillis = 1000)
                    } else {
                        swipeRight(startX = left, endX = left + 250f, durationMillis = 1000)
                    }
                }
                waitForIdle()
                return headerAtMargin()
            }

            // Three sections, so four steps has to come back to where it began — and nine of them
            // exhausts any finite amount of laid-out content several times over.
            val forwards = List(9) { step(forward = true) }
            assertEquals(
                List(9) { headers[(it + 1) % 3] },
                forwards,
                "the panorama stopped cycling forwards",
            )
            val backwards = List(9) { step(forward = false) }
            assertEquals(
                List(9) { headers[(9 - it - 1) % 3] },
                backwards,
                "the panorama stopped cycling backwards",
            )
        }
    }

    /**
     * Panning backwards off the first section arrives at the last one.
     *
     * The wraparound used to be one-way: two copies with the scroll resting in the first, so there
     * was nothing to the left of the opening section and a backwards pan hit a wall. "There is
     * never an end to reach" was true only going forwards, which is not what a panorama does.
     */
    @Test
    fun panning_backwards_from_the_first_section_reaches_the_last() {
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    scroll = remember { ScrollState(0) }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
                            scroll = scroll,
                            sections =
                                listOf(
                                    headers[0] to { Body(300.dp, "alpha") },
                                    headers[1] to { Body(220.dp, "beta") },
                                    headers[2] to { Body(340.dp, "gamma") },
                                ),
                        )
                    }
                }
            }
            waitForIdle()

            fun lefts(header: String): List<Float> =
                onAllNodesWithText(header).fetchSemanticsNodes().indices.map { copy ->
                    onAllNodesWithText(header)[copy].getUnclippedBoundsInRoot().left.value
                }

            fun headerAtMargin(): String? = headers.firstOrNull { header -> lefts(header).any { abs(it - 18f) < 2f } }

            assertEquals(headers[0], headerAtMargin(), "the panorama does not open on its first section")

            // A short, slow pan. A full-width swipe drags further than a whole section on its own,
            // and then "one section back" is not a thing the gesture can be said to ask for — the
            // first version of this expected the last section and got the one before it, which was
            // the drag's own travel rather than a defect.
            onNodeWithTag("pano").performTouchInput {
                swipeRight(startX = left, endX = left + 250f, durationMillis = 1000)
            }
            waitForIdle()

            assertEquals(
                headers[2],
                headerAtMargin(),
                "backwards from the first section should arrive at the last: " +
                    headers.joinToString { "$it@${lefts(it)}" },
            )
        }
    }

    @Test
    fun a_section_wider_than_the_screen_can_be_looked_at() {
        runComposeUiTest {
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
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

            // Every section is laid out three times, so a header matches three nodes and the
            // question is always whether *any* of them is where it should be.
            fun lefts(header: String): List<Float> =
                onAllNodesWithText(header).fetchSemanticsNodes().indices.map { copy ->
                    onAllNodesWithText(header)[copy].getUnclippedBoundsInRoot().left.value
                }

            // A **pan**, which is the word the source uses, rather than a flick: slow enough that
            // the release carries almost no velocity. A hard throw travelling past a whole section
            // is correct and is what the other test does; this one is a person dragging the wide
            // section's content across to look at the rest of it.
            onNodeWithTag("pano").performTouchInput { swipeLeft(durationMillis = 1000) }
            waitForIdle()

            // No copy of the second header has reached the margin, so the release did not throw
            // the panorama past a section whose content is off the screen — which is what a single
            // left-edge stop per section does, and what this fixture exists to catch.
            assertTrue(
                lefts(headers[1]).none { abs(it - 18f) < 2f },
                "the next section is already at the margin: the wide one was skipped rather than " +
                    "panned — ${headers[1]}@${lefts(headers[1])}",
            )
            // And it settled on a stop rather than wherever momentum died: the wide section's right
            // edge against the right of the viewport, 900 plus its 27 of padding less the 400 shown.
            val rightEdge = 18f - (900f + 27f - 400f)
            assertTrue(
                lefts(headers[0]).any { abs(it - rightEdge) < 2f },
                "no copy of the wide section is showing its right edge: expected $rightEdge, got " +
                    "${lefts(headers[0])}",
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
                    typography = portableTypography(kvadrantLatin()),
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
            // **The next one, not any one.** A rule that takes the nearest stop to where the fling
            // was predicted to end skips a narrow section whenever the prediction overshoots it —
            // which it does — and "some header is at the margin" is satisfied by the section after
            // the one that was skipped. The gesture here is a hard swipe; it may not travel two.
            assertEquals(
                headers[1],
                headerAtMargin(),
                "settled at ${scroll.value}: " + headers.joinToString { "$it@${lefts(it)}" },
            )
        }
    }
}
