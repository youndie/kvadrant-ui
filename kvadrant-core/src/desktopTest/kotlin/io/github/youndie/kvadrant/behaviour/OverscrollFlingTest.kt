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
    fun an_ordinary_throw_does_not_spend_the_whole_spring() {
        // **The complaint this replaces a test for.** A previous version asserted that the squeeze
        // took several frames to arrive, on the reading that "it starts at maximum" was about
        // timing. It was not: the spring's whole range is a few per cent of a viewport and a throw
        // crosses that in a frame whatever model runs it, so the shape was never the thing to fix.
        // What was wrong is that every ordinary flick reached the limit, which is a depth that has
        // stopped answering how hard the list was thrown.
        val gentle = flungAtTheStop(velocity = GENTLE).min()
        val hard = flungAtTheStop(velocity = HARD).min()
        val limit = BAND - (BAND * MAX_COMPRESSION).toInt()

        assertTrue(
            gentle > limit,
            "a $GENTLE px/s throw squeezed the last band to $gentle px, which is the limit at " +
                "$limit — an ordinary flick is spending the whole spring and the depth no longer " +
                "says how hard anything was thrown",
        )
        assertTrue(
            hard > limit,
            "a $HARD px/s throw reached the limit at $limit px. The curve is chosen so that " +
                "nothing ever spends all of the spring; if this fails the reference has moved",
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
         * the fling has twenty pixels left to cover before it does.
         *
         * Twenty and not fifty: Compose's default decay is stiff, and the gentler of the two speeds
         * below — one and a half viewports a second, an ordinary flick — could not carry the list
         * fifty pixels. The list then never reached its stop, no frame was sampled, and both tests
         * failed on an empty series rather than on anything about compression.
         */
        const val THROW = 280f

        /**
         * Two speeds, and they are chosen **in viewports per second** rather than in pixels.
         *
         * That is the unit the effect works in, and getting it wrong is how the first version of
         * these numbers lied: 4 000 px/s is a hard but ordinary flick on a phone and **thirteen
         * viewports a second** in this fixture's 300 px frame, which is off the end of anything a
         * thumb produces. It saturated the spring, and a test at a speed nobody reaches cannot say
         * whether ordinary ones do.
         *
         * A thumb produces about 1.4 to 5.5 viewports per second (`DEFAULT_FLING_REFERENCE`), so
         * these are 1.5 and 5 of them — either side of the reference, both inside the range, and far
         * enough apart that the curve between them is plainly climbing.
         */
        const val GENTLE = 450f
        const val HARD = 1500f

        /** `KvadrantOverscroll.DEFAULT_MAX_COMPRESSION`, repeated so the limit can be computed. */
        const val MAX_COMPRESSION = 0.06f

        /** A second at sixty a frame: the fling, the peak and most of the 300 ms return. */
        const val FRAMES = 60
    }
}
