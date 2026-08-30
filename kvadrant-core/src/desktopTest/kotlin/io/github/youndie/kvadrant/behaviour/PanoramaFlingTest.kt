package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.v2.runComposeUiTest
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

/**
 * Which section a flick lands on, and — the part worth guarding — which one it *cannot* land on.
 *
 * [B-33](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-33-panorama-is-a-scroller-not-an-item-model.md)
 * asks for "a scenario naming which section is selected after a flick of a given size". The answer
 * is a decision rather than a measurement, because Microsoft published no fling model for the
 * panorama at all: a release settles on **one of the two stops the finger is between**, and the
 * decay only chooses which of the two.
 *
 * The rule it replaced took the nearest stop to the predicted end of the decay, and that skipped
 * sections — released between two narrow ones, a prediction landing slightly past the second is
 * nearer to the third, and a section goes by unseen. Nothing has held the replacement in place
 * since. A revert would look entirely reasonable in review and would show up only as a section
 * occasionally vanishing under a hard flick, which is the kind of thing that gets reported as
 * "sometimes it jumps".
 *
 * **The numbers below are measured, and the first set was worthless.** A guard for this has to be
 * checked by putting the old rule back, and the first version of this file passed with it restored —
 * it asserted a case where both rules happen to agree, which is most of them: the panorama *wraps*,
 * so a prediction that overshoots by a whole copy lands on some other copy of the same section and
 * the header at the margin comes out the same either way.
 *
 * So both rules were swept over drag and velocity, and these are cells where they differ:
 *
 * | drag | release | bracketed | nearest to the prediction |
 * |---|---|---|---|
 * | 250 px | 1500 px/s | `two` | `three` — a section skipped |
 * | 250 px | 3000 px/s | `two` | `one` — wrapped clean past |
 *
 * Those are the assertions. Anything else here would pass under the defect.
 */
@OptIn(ExperimentalTestApi::class)
class PanoramaFlingTest {
    private val headers = listOf("one", "two", "three")

    /** Tall, so the swipe at the node's centre lands on the scrolling row. `PanoramaSnapTest` too. */
    @Composable
    private fun Body(
        width: Dp,
        label: String,
    ) {
        Box(Modifier.width(width).height(400.dp)) { KvadrantText(label) }
    }

    /**
     * Drags the panorama a fixed distance and releases it at [endVelocity], then reports which
     * section header came to rest at the left margin.
     *
     * The distance is held constant across every case on purpose: this is a test about the release,
     * and a swipe that travelled further would move the finger into a different pair of stops and
     * change the answer for a reason that has nothing to do with velocity.
     */
    private fun landsOn(
        drag: Float,
        endVelocity: Float,
    ): String? {
        var landed: String? = null
        runComposeUiTest {
            setContent {
                KvadrantTheme(
                    colors = KvadrantColors.dark(),
                    typography = portableTypography(kvadrantLatin()),
                ) {
                    remember { ScrollState(0) }
                    Box(Modifier.size(400.dp, 600.dp).testTag("pano")) {
                        KvadrantPanorama(
                            title = "photos",
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

            onNodeWithTag("pano").performTouchInput {
                val y = centerY
                swipeWithVelocity(
                    start =
                        Offset(centerX + drag / 2f, y),
                    end =
                        Offset(centerX - drag / 2f, y),
                    endVelocity = endVelocity,
                    durationMillis = DURATION_MILLIS,
                )
            }
            waitForIdle()

            landed =
                headers.firstOrNull { header ->
                    onAllNodesWithText(header).fetchSemanticsNodes().indices.any { copy ->
                        abs(onAllNodesWithText(header)[copy].getUnclippedBoundsInRoot().left.value - MARGIN) < 2f
                    }
                }
        }
        return landed
    }

    /**
     * The naming half of the criterion: what a given gesture actually selects.
     *
     * It does **not** discriminate between the two fling rules — both pass it — and that is the
     * right division of labour rather than a weakness. This says what the control does, and
     * [no_flick_however_hard_skips_a_section] says what it must never do.
     */
    @Test
    fun a_gentle_release_falls_back_and_a_firm_one_carries_on() {
        assertEquals(
            headers[0],
            landsOn(SHORT_DRAG, GENTLE),
            "a gentle release did not fall back to where it started",
        )
        assertEquals(
            headers[1],
            landsOn(SHORT_DRAG, FIRM),
            "a firm release from the same drag did not carry on to the next section",
        )
    }

    /**
     * The bracketing guard, and the reason this file exists.
     *
     * A release cannot pass more than one stop beyond where the finger left the screen, however
     * hard it is thrown. Both cases land on `two`; under the rule this replaced the first lands on
     * `three` — the skipped section the item describes — and the second on `one`, having wrapped
     * past everything.
     */
    @Test
    fun no_flick_however_hard_skips_a_section() {
        assertEquals(
            headers[1],
            landsOn(LONG_DRAG, FIRM_THROW),
            "a firm throw travelled past the next section: a release is no longer bracketed by the " +
                "two stops the finger was between, and sections go by unseen",
        )
        assertEquals(
            headers[1],
            landsOn(LONG_DRAG, HARD_THROW),
            "a hard throw did not stop at the next section, so velocity is deciding how far the " +
                "panorama travels instead of only which of two stops it settles on",
        )
    }

    private companion object {
        const val DURATION_MILLIS = 100L
        const val MARGIN = 18f

        /** Short enough that a release without a throw still has the first stop as its bracket. */
        const val SHORT_DRAG = 100f

        /** Dragged and stopped, rather than thrown. */
        const val GENTLE = 0f
        const val FIRM = 500f

        /**
         * **Velocity cannot be raised without raising the distance**, and that is physics rather
         * than a limit of the harness: a release velocity is read from the last few events, so a
         * big one takes a big final movement. `swipeWithVelocity` says so by throwing, which is how
         * the first version of this file learned it.
         */
        const val LONG_DRAG = 250f
        const val FIRM_THROW = 1_500f
        const val HARD_THROW = 3_000f
    }
}
