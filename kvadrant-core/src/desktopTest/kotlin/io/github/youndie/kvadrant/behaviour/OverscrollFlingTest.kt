package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A list **flung** into its end compresses, and harder for a harder throw.
 *
 * [B-45](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-45-overscroll-ignores-the-fling.md).
 * `OverscrollCompressionTest` next door drags a finger past the stop and holds it there, which is
 * the rarer of the two ways a list ends — and it was the only one covered, so the common one was
 * broken in a shipped release with a comment above it saying it was not. `applyToScroll` absorbs
 * leftover only for `NestedScrollSource.UserInput`; a fling's deltas are a `SideEffect`, and
 * `applyToFling` released a compression that nothing had ever applied.
 *
 * **The list starts at the top and is thrown, rather than starting at the stop.** Beginning at the
 * end would mean the drag itself compressed before the fling ever started, and the frame at the peak
 * could not say which of the two had done it. Here the drag is entirely within range and only the
 * fling reaches the wall, so what is measured is the fling's.
 *
 * The clock is stopped for the same reason the other test holds the finger down: the compression
 * exists between the fling ending and the spring finishing, and a frame taken after that is a
 * photograph of the return. Stepping frame by frame and keeping the largest displacement avoids
 * having to know which frame the peak lands on.
 */
@OptIn(ExperimentalTestApi::class)
class OverscrollFlingTest {
    @Test
    fun a_list_flung_into_its_end_compresses() {
        val series = flungAtTheStop(velocity = HARD)
        val smallest = series.minOrNull() ?: -1
        val settled = series.lastOrNull() ?: -1

        // The control. Once the spring has finished, the last band is its whole self again — so the
        // measurement means what it is claimed to mean, and a smaller number below is a squeeze
        // rather than the harness looking at the wrong rows.
        assertTrue(
            settled == BAND,
            "after the spring finished the last band is $settled px of $BAND — the measurement is " +
                "not of a settled list at its stop, and nothing below it means anything",
        )

        assertTrue(
            smallest < BAND,
            "the last band never went below its full $BAND px while the list sat at its stop, so a " +
                "list thrown into its end did not compress at all — the fling's leftover velocity " +
                "is going nowhere",
        )
    }

    @Test
    fun a_harder_throw_compresses_further() {
        val gentle = flungAtTheStop(velocity = GENTLE).min()
        val hard = flungAtTheStop(velocity = HARD).min()

        assertTrue(
            gentle < BAND && hard < BAND,
            "one of the two throws did not compress at all ($gentle and $hard of $BAND px), so this " +
                "is comparing a squeeze with nothing rather than two squeezes",
        )
        assertTrue(
            hard < gentle,
            "a throw at $HARD px/s squeezed the last band to $hard px and one at $GENTLE px/s to " +
                "$gentle — the depth does not follow the speed, so it is a constant wearing a " +
                "velocity's clothes and one test at one speed would have missed it",
        )
    }

    @Test
    fun the_compression_builds_up_rather_than_arriving_at_its_limit() {
        val series = flungAtTheStop(velocity = HARD)
        val floor = series.min()
        val approaching = series.take(series.indexOf(floor)).count { it in (floor + 1) until BAND }

        assertTrue(
            approaching >= MINIMUM_APPROACH_FRAMES,
            "the last band went from $BAND px to its smallest $floor px through $approaching " +
                "intermediate frames, so the compression arrives rather than building: $series",
        )
    }

    /**
     * The visible height of the last band while the list sits at its stop: its smallest, and its
     * value once everything has settled.
     *
     * **Measured only at the stop, and that is the whole of why this is not the sibling test's
     * measurement.** A boundary's distance from the top moves for two reasons here — the list
     * scrolling under the fling, and the compression — and the first is far larger. Sampling only
     * the frames where `ScrollState` is at its maximum removes the scroll from the question
     * entirely; what is left can only be the effect.
     *
     * The last band is what is measured because it is against the pivot: compression at the bottom
     * edge keeps that edge still and pulls everything above it down, so the band's visible height
     * shrinks. It cannot shrink for any other reason once the list has stopped.
     */
    private fun flungAtTheStop(velocity: Float?): List<Int> {
        val series = mutableListOf<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            lateinit var scroll: ScrollState
            setContent {
                KvadrantTheme(colors = KvadrantColors.dark()) {
                    scroll = rememberScrollState()
                    Box(
                        Modifier
                            .size(WIDTH.dp, VIEWPORT.dp)
                            .background(Color.Black)
                            .testTag(TAG)
                            .verticalScroll(scroll),
                    ) {
                        // Bands, for `OverscrollCompressionTest`'s reason: a solid block taller than
                        // the viewport replaces whatever is squeezed away with more of itself, and
                        // reports no change from an effect that is working.
                        Column {
                            repeat(BANDS) { index ->
                                Box(
                                    Modifier
                                        .size(WIDTH.dp, BAND.dp)
                                        .background(if (index % 2 == 0) Color.White else Color.Blue),
                                )
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            if (velocity != null) {
                onNodeWithTag(TAG).performTouchInput {
                    // Upwards from the bottom of the viewport: the drag itself stays inside the
                    // content, so only what the fling carries past the last band reaches the stop
                    // and the compression measured is the fling's. The velocity is stated rather
                    // than inferred from a distance and a duration, so the two speeds the second
                    // test compares are numbers in the test instead of a consequence of how the
                    // injector spaces its samples.
                    swipeWithVelocity(
                        start = Offset(WIDTH / 2f, VIEWPORT - 1f),
                        end = Offset(WIDTH / 2f, VIEWPORT - 1f - THROW),
                        endVelocity = velocity,
                    )
                }
            }

            repeat(FRAMES) {
                mainClock.advanceTimeByFrame()
                if (scroll.value != scroll.maxValue) return@repeat
                series += lastBandHeight()
            }
        }
        return series
    }

    /** How many rows of the bottom band are visible: the run of blue upwards from the last row. */
    private fun ComposeUiTest.lastBandHeight(): Int {
        val image = onNodeWithTag(TAG).captureToImage()
        val pixels = IntArray(image.width * image.height).also(image::readPixels)

        fun blue(y: Int): Boolean {
            val pixel = pixels[y * image.width + image.width / 2]
            return (pixel and 0xFF) > 0x80 && (pixel shr 16 and 0xFF) < 0x40
        }
        var run = 0
        var y = image.height - 1
        while (y >= 0 && blue(y)) {
            run++
            y--
        }
        return run
    }

    private companion object {
        const val TAG = "list"
        const val WIDTH = 200
        const val VIEWPORT = 300
        const val BANDS = 6
        const val BAND = 100

        /**
         * Short of the 300 px the list can travel, so the drag never reaches the stop itself and
         * the fling has 50 px left to cover before it does.
         */
        const val THROW = 250f

        /**
         * Two speeds either side of the reference, in pixels per second.
         *
         * `DEFAULT_FLING_REFERENCE` is a viewport and a half per second, which is 450 px/s here, so
         * both of these are past it and the curve between them is still climbing. Two speeds chosen
         * further out would both saturate, and the assertion would hold for a constant.
         */
        const val GENTLE = 900f
        const val HARD = 4000f

        /**
         * How many frames the squeeze has to take on its way in.
         *
         * **Reported from a device and invisible to the two tests above**: a list flung into its end
         * appeared already fully compressed and then recovered, where a finger dragged into it
         * squeezes gradually. Both of those assert the *peak*, which a snap and a squeeze reach
         * alike — the shape over time is a different claim and needed its own. Three is comfortably
         * below the nine the fix produces here and unreachable by a step, which produces none.
         */
        const val MINIMUM_APPROACH_FRAMES = 3

        /** A second at sixty a frame: the fling, the peak and most of the 300 ms return. */
        const val FRAMES = 60
    }
}
