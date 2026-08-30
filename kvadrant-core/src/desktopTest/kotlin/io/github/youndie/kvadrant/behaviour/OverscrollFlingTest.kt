package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.v2.runComposeUiTest
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
    fun a_list_flung_into_its_end_pulls_past_it() {
        val series = flungAtTheStop(velocity = HARD)
        val furthest = series.min()
        val settled = series.last()

        // The control, and it does **not** assume which row a settled list puts the boundary on:
        // the first sample already carries the compression, because a throw's squeeze is instant.
        // What it asserts is that the series reaches a steady state at all — without that, "the
        // maximum is larger than the last value" is a statement about two arbitrary frames.
        assertTrue(
            series.takeLast(SETTLED_FRAMES).all { it == settled },
            "the content never came to rest: the last $SETTLED_FRAMES samples are " +
                "${series.takeLast(SETTLED_FRAMES)}, so nothing below this means anything",
        )

        assertTrue(
            furthest < settled,
            "the content's bottom never left $settled while the list sat at its stop, so a list " +
                "thrown into its end did not move at all — the fling's leftover velocity is going " +
                "nowhere",
        )
    }

    @Test
    fun a_harder_throw_pulls_further() {
        val gentle = flungAtTheStop(velocity = GENTLE).let { it.last() - it.min() }
        val hard = flungAtTheStop(velocity = HARD).let { it.last() - it.min() }

        assertTrue(
            gentle > 0 && hard > 0,
            "one of the two throws moved nothing at all ($gentle and $hard px), so this is " +
                "comparing a pull with nothing rather than two pulls",
        )
        assertTrue(
            hard > gentle,
            "a throw at $HARD px/s pulled $hard px and one at $GENTLE px/s $gentle — " +
                "the distance does not follow the speed, so it is a constant wearing a velocity's " +
                "clothes and one test at one speed would have missed it",
        )
    }

    @Test
    fun an_ordinary_throw_does_not_spend_the_whole_travel() {
        // **The complaint this replaces a test for.** A previous version asserted that the squeeze
        // took several frames to arrive, on the reading that "it starts at maximum" was about
        // timing. It was not: the spring's whole range is a few per cent of a viewport and a throw
        // crosses that in a frame whatever model runs it, so the shape was never the thing to fix.
        // What was wrong is that every ordinary flick reached the limit, which is a depth that has
        // stopped answering how hard the list was thrown.
        //
        // The limit is the effect's own clamp — `DEFAULT_MAX_OFFSET` of the viewport, eighteen
        // pixels here — rather than a number of mine. A separate saturating run was tried as the
        // control and measured zero about half the time: the whole gesture can finish inside the
        // injector's own clock advance, and a control that is itself flaky turns one claim into two.
        val gentle = flungAtTheStop(velocity = GENTLE).let { it.last() - it.min() }
        val hard = flungAtTheStop(velocity = HARD).let { it.last() - it.min() }

        assertTrue(gentle > 0, "the gentler throw moved nothing, so there is nothing to compare")
        assertTrue(
            hard < LIMIT_PX,
            "a $HARD px/s throw pulled the content $hard px of a possible $LIMIT_PX — an ordinary " +
                "flick is spending everything the effect has and the distance no longer says how " +
                "hard anything was thrown",
        )
        assertTrue(
            gentle < hard,
            "a $GENTLE px/s throw pulled as far as a $HARD px/s one ($gentle against $hard), " +
                "which is the same complaint one step lower",
        )
    }

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
                series += contentBottom()
            }
        }
        return series
    }

    @Test
    fun letting_go_past_the_end_does_not_pull_further() {
        // Drag past the stop, hold, and let go. The finger's displacement is already in the effect
        // and the finger's own velocity arrives as the fling's leftover, so **adding** them
        // deepened the squeeze at the instant of release — the moment a spring should be coming
        // back. Reported as a small throw jumping.
        val held = mutableListOf<Int>()
        val afterRelease = mutableListOf<Int>()
        runComposeUiTest {
            mainClock.autoAdvance = false
            list()
            repeat(SETTLED_FRAMES) { mainClock.advanceTimeByFrame() }
            onNodeWithTag(TAG).performTouchInput {
                down(Offset(WIDTH / 2f, VIEWPORT - 1f))
                moveBy(Offset(0f, -PAST_THE_END))
                moveBy(Offset(0f, -PAST_THE_END))
            }
            mainClock.advanceTimeByFrame()
            held += contentBottom()
            onNodeWithTag(TAG).performTouchInput { up() }
            repeat(RELEASE_FRAMES) {
                mainClock.advanceTimeByFrame()
                afterRelease += contentBottom()
            }
        }

        assertTrue(
            held.first() < VIEWPORT - 1,
            "the finger opened no gap at all: the content still reaches ${held.first()}",
        )
        assertTrue(
            afterRelease.min() >= held.first(),
            "the finger pulled the content's bottom to ${held.first()} and letting go took it to " +
                "${afterRelease.min()} — a release pulled further instead of starting the return: " +
                "$afterRelease",
        )
    }

    /** The list at its stop, held there by a drag rather than reached by a throw. */
    private fun ComposeUiTest.list() {
        setContent {
            KvadrantTheme(colors = KvadrantColors.dark()) {
                val scroll = rememberScrollState()
                LaunchedEffect(Unit) { scroll.scrollTo(scroll.maxValue) }
                Box(
                    Modifier
                        .size(WIDTH.dp, VIEWPORT.dp)
                        .background(Color.Black)
                        .testTag(TAG)
                        .verticalScroll(scroll),
                ) {
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
    }

    /**
     * The last row of the viewport that still has content on it.
     *
     * **The content slides away from the boundary and the page shows behind it**, so the thing that
     * moves is the bottom of the content: at rest it is the last row of the frame, and under a pull
     * it retreats by however far the finger has taken it.
     *
     * Two measurements were discarded on the way here and both are worth naming. The last band's
     * visible *height* has a lever arm of one band, so a one per cent effect moved it by a pixel and
     * two throws differing threefold rounded to the same row. The first boundary from the *top* has
     * the whole viewport as its arm and was right for a squeeze — but a translation at the end of a
     * list moves the content **up**, and the top of the frame is already covered, so it has nowhere
     * to go. What the effect does is open a gap, so the gap is what to measure.
     */
    private fun ComposeUiTest.contentBottom(): Int {
        val image = onNodeWithTag(TAG).captureToImage()
        val pixels = IntArray(image.width * image.height).also(image::readPixels)
        return (0 until image.height).lastOrNull { y ->
            pixels[y * image.width + image.width / 2] and 0xFFFFFF != 0
        } ?: -1
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
         * these are 2.5 and 5 of them — both inside the range and far enough apart that the curve
         * between them is plainly climbing.
         *
         * The gentler one was 1.5 until the curve gained its square, which is what suppressing the
         * low end means: at one and a half viewports a second the spring now gives a pixel and a
         * half in a 300 px frame, and a test cannot tell a pixel and a half from rounding. That is
         * the effect working, not a regression — but it moves what this fixture can measure, and a
         * speed too small to resolve would have made the comparison hold for a constant.
         */
        const val GENTLE = 750f
        const val HARD = 1500f

        /** `DEFAULT_MAX_OFFSET` of this viewport: the furthest the effect can pull, in pixels. */
        const val LIMIT_PX = (VIEWPORT * 0.06f).toInt()

        /** Long enough after the release to be sure it is over, at sixty a second. */
        const val SETTLED_FRAMES = 8

        /** Half the viewport of finger travel past the stop, which reaches the limit comfortably. */
        const val PAST_THE_END = 150f
        const val RELEASE_FRAMES = 10

        /** A second at sixty a frame: the fling, the peak and most of the 300 ms return. */
        const val FRAMES = 60
    }
}
