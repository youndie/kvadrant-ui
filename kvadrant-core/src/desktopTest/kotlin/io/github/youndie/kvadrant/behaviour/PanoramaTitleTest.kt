package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantPanorama
import io.github.youndie.kvadrant.foundation.KvadrantText
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The title leaves the screen at a wrap and comes back from the other side, and does that at no
 * other time.
 *
 * [B-33](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-33-panorama-is-a-scroller-not-an-item-model.md)'s
 * last acceptance criterion. `PanningTitleLayer` "does not repeat itself when you pan past the edges
 * of the content. Instead ... it animates out of view in the direction it was previously moving and
 * animates back into the scene from the other side of the screen" (`ff941126`). Until this the title
 * was a cylinder like every other layer — a named deviation, correct while there was nothing to hang
 * a selection change on.
 *
 * **Absence is the assertion, because a repeating layer can never be absent.** The old title was
 * drawn three times and offset modulo its own width, so some copy of it was on screen at every
 * moment of every pan; a frame with an empty title band cannot be produced by that model at all.
 * That makes one measurement carry both halves of the claim — that it goes, and that it does not
 * repeat — without having to find a seam in a word.
 */
@OptIn(ExperimentalTestApi::class)
class PanoramaTitleTest {
    @Test
    fun the_title_leaves_and_returns_when_the_content_wraps() {
        val ink = inkPerFrame(wrap = true)

        assertTrue(ink.first() > 0, "the title is not on screen to begin with: ${ink.first()} px of ink")
        assertTrue(
            ink.min() == 0,
            "the title band never emptied across ${ink.size} frames (least ${ink.min()} px of ink), " +
                "so the title never left the screen — it is still a repeating layer",
        )
        assertTrue(
            ink.last() > 0,
            "the title went and did not come back: ${ink.last()} px of ink on the last frame",
        )
    }

    @Test
    fun it_does_not_do_that_when_the_panorama_merely_opens() {
        // From the first frame, and **that is the whole test.** Measuring after the panorama has
        // settled — which is what this did first — lets the opening animation finish before the
        // camera is switched on: removing the guard that suppresses it failed the *wrap* test, by
        // leaving a transition in flight, and left this one green. A guard that cannot fail for its
        // own defect is a guard for something else.
        val ink = inkPerFrame(wrap = false, openingFrames = 0)
        // Before the first layout there is nothing drawn at all, which is not the title leaving.
        val once = ink.dropWhile { it == 0 }

        assertTrue(once.size > FRAMES / 2, "the title never appeared: $ink")
        // **Not "never empty", and the difference was measured.** With the guard removed the
        // opening transition does play, and its extreme still leaves eighteen pixels of ink where a
        // real wrap leaves none — enough for an emptiness test to pass over it. What separates the
        // two cases is not zero, it is the two orders of magnitude between eighteen and the eight
        // thousand the title draws when it is where it belongs.
        assertTrue(
            once.min() > once.first() / 2,
            "the title lost more than half its ink while the panorama was only opening (least " +
                "${once.min()} of ${once.first()}) — the wrap transition is firing on the move into " +
                "the middle copy, so every panorama flies its title in from the side when it is " +
                "composed: $once",
        )
    }

    /**
     * How much of the title is on screen, frame by frame, with the clock stopped.
     *
     * The band is the top quarter of the panorama, which the title has to itself: the first section
     * header sits below it and no section's body reaches up. Stepping frames by hand is what makes
     * a transient visible at all — the whole thing is over in one settle, and `waitForIdle` lands
     * after it.
     */
    private fun inkPerFrame(
        wrap: Boolean,
        openingFrames: Int = OPENING_FRAMES,
    ): List<Int> {
        val ink = mutableListOf<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    scroll = remember { ScrollState(0) }
                    Box(Modifier.size(VIEWPORT.dp, HEIGHT.dp).testTag(TAG)) {
                        KvadrantPanorama(
                            title = "kvadrant",
                            scroll = scroll,
                            sections =
                                listOf(
                                    "one" to { KvadrantText("alpha", Modifier.width(500.dp)) },
                                    "two" to { KvadrantText("beta", Modifier.width(220.dp)) },
                                    "three" to { KvadrantText("gamma", Modifier.width(340.dp)) },
                                ),
                        )
                    }
                }
            }
            // Enough for the panorama to measure and settle into the middle copy, which is what
            // arms the transition for a real wrap.
            repeat(openingFrames) { mainClock.advanceTimeByFrame() }

            if (wrap) {
                // Past the far edge of the middle copy, which is what a person reaches by panning
                // off the last section. How it is reached does not matter to the title.
                runBlocking { scroll.scrollTo(scroll.maxValue) }
            }

            repeat(FRAMES) {
                mainClock.advanceTimeByFrame()
                ink += titleInk()
            }
        }
        return ink
    }

    private fun ComposeUiTest.titleInk(): Int {
        val image = onNodeWithTag(TAG).captureToImage()
        val pixels = IntArray(image.width * image.height).also(image::readPixels)
        val band = image.height / 4
        return (0 until band).sumOf { y ->
            (0 until image.width).count { x -> pixels[y * image.width + x] and 0xFFFFFF != 0 }
        }
    }

    private companion object {
        const val TAG = "pano"
        const val VIEWPORT = 400
        const val HEIGHT = 600
        const val OPENING_FRAMES = 8

        /** A settle and a half at sixty a frame: the whole transition and some of the rest. */
        const val FRAMES = 30
    }
}
