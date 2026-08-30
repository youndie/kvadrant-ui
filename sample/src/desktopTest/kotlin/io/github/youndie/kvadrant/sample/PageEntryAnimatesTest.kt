package io.github.youndie.kvadrant.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opening a page turns it in rather than showing it finished.
 *
 * "It animates" is a claim about the frames in between, and both ends look identical whether or not
 * it does — which is how the entry was lost twice in one afternoon with every test still green. The
 * turnstile turns about the left edge, so mid-flight the content is narrower than it ends up: the
 * right edge of a label on the page is what moves.
 *
 * The way it is lost is always the same. A turnstile handed `visible = true` on the frame it is
 * first composed has state and target already in agreement, `updateTransition` has nothing to run,
 * and the page appears.
 *
 * Two things about the harness, both learned by hanging it for ten minutes: `performClick` and
 * `performScrollTo` wait for idle, and with the clock stopped idle never arrives — so the touch is
 * injected directly and the window is made tall enough that nothing needs scrolling to.
 */
@OptIn(ExperimentalTestApi::class)
class PageEntryAnimatesTest {
    @Test
    fun the_page_turns_in_rather_than_appearing() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            setContent {
                Box(Modifier.size(520.dp, 1400.dp)) { KvadrantSampleApp() }
            }
            mainClock.advanceTimeBy(1_000)

            onNodeWithText("календарь").performTouchInput { click() }

            // 350 ms is the whole entry; 60 is late enough to have started and early enough that a
            // turning page is still well short of its final width.
            mainClock.advanceTimeBy(60)
            val midFlight = onNodeWithText("месяц").getUnclippedBoundsInRoot().right.value

            mainClock.advanceTimeBy(2_000)
            val settled = onNodeWithText("месяц").getUnclippedBoundsInRoot().right.value

            assertTrue(
                abs(midFlight - settled) > 2f,
                "the label's right edge was at $midFlight 60 ms in and $settled when settled — the " +
                    "page did not turn in, it appeared",
            )
        }
}
