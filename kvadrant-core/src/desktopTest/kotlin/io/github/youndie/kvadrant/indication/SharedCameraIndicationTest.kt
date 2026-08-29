package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.theme.KvadrantAccents
import io.github.youndie.kvadrant.theme.KvadrantTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The spike's switch moves something, through the indication the library actually installs.
 *
 * [B-26](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-26-per-layer-camera-versus-a-global-one.md)
 * has a fixture that draws both cameras already — `OneSurfaceTwoPlacesScreenshots` — and it draws
 * them with `graphicsLayer` and `drawWithContent` written beside the tilt rather than through it. It
 * is the right thing for a still and the wrong thing for a switch: a demo wired to a copy of the
 * tilt would let the judgement be made about the copy, and the flag could be inert without anything
 * saying so.
 *
 * So this presses a real surface, held, under the real `TiltIndication`, in two places on one screen:
 *
 * - **per-layer** — every element carries its own camera at its own centre, so where it sits cannot
 *   matter and the two must come out identical, to the pixel;
 * - **shared** — one camera over the display, so they must not.
 *
 * The first assertion is the control. Without it a difference under the shared camera could as
 * easily be the harness placing two surfaces that were never the same to begin with.
 */
@OptIn(ExperimentalTestApi::class)
class SharedCameraIndicationTest {
    @Test
    fun the_per_layer_camera_draws_the_same_press_wherever_the_surface_is() {
        val moved = pressedInTwoPlaces(shared = false).moved()
        assertTrue(
            moved <= SAME_SHAPE,
            "$moved pixels of ${WINDOW * WINDOW} differ between the two positions under a camera " +
                "that cannot see where the surface is — measured at 4, which is the edge of the " +
                "quad landing on different subpixels, against 1 825 for the shared one",
        )
    }

    @Test
    fun the_shared_camera_draws_it_differently() {
        val moved = pressedInTwoPlaces(shared = true).moved()
        assertTrue(
            moved > MINIMUM_PIXELS_MOVED,
            "only $moved pixels of ${WINDOW * WINDOW} differ between the two positions — the shared " +
                "camera is not reaching the indication, and the demo's switch would be inert",
        )
    }

    private fun Pair<IntArray, IntArray>.moved(): Int = first.indices.count { first[it] != second[it] }

    /**
     * The same surface, pressed the same way, in two separate compositions.
     *
     * **One composition holding both was the first attempt and it compared two different things.**
     * A press on desktop takes focus, only one thing can hold it, and the focus ring the theme now
     * draws landed on whichever surface was pressed second — so the control failed on a dotted
     * border rather than on any camera. Two runs make the two frames identical in everything except
     * the one variable, which is what a control has to mean.
     */
    private fun pressedInTwoPlaces(shared: Boolean): Pair<IntArray, IntArray> =
        pressedAt(125, 40, shared) to pressedAt(10, 350, shared)

    private fun pressedAt(
        x: Int,
        y: Int,
        shared: Boolean,
    ): IntArray {
        lateinit var window: IntArray
        runComposeUiTest {
            setContent {
                KvadrantTheme {
                    CompositionLocalProvider(
                        LocalIndication provides
                            TiltIndication(
                                maxDepression = KvadrantTheme.metrics.tiltDepression,
                                sharedCamera = shared,
                            ),
                    ) {
                        Box(Modifier.testTag(FRAME).fillMaxSize().background(Color.Black)) {
                            Box(
                                Modifier
                                    .offset(x.dp, y.dp)
                                    .size(SURFACE.dp)
                                    .testTag(SURFACE_TAG)
                                    // `clickable` outside the fill. The indication transforms what
                                    // is below it in the chain, so a background applied first is
                                    // painted untilted and the tilt is applied to an empty box —
                                    // which draws a still square and looks exactly like a camera
                                    // that does nothing.
                                    .clickable {}
                                    .background(KvadrantAccents.Cobalt),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            // Held, not tapped: the lean unwinds on release and a capture after that is of nothing.
            onNodeWithTag(SURFACE_TAG).performTouchInput { down(0, PRESS) }
            waitForIdle()

            val frame = onNodeWithTag(FRAME).captureToImage()
            val pixels = IntArray(frame.width * frame.height).also(frame::readPixels)
            window =
                IntArray(WINDOW * WINDOW) { i ->
                    pixels[(y - MARGIN + i / WINDOW) * frame.width + (x - MARGIN + i % WINDOW)]
                }
        }
        return window
    }

    private companion object {
        const val FRAME = "frame"
        const val SURFACE_TAG = "surface"
        const val SURFACE = 150

        /** The surface's own upper-left quarter, so the lean has a direction. */
        val PRESS = Offset(30f, 30f)

        /** Room around the surface for the projection to spill into. Density is 1 here. */
        const val MARGIN = 10
        const val WINDOW = SURFACE + MARGIN * 2

        /**
         * Not zero, and the four pixels it is not zero by are the point of the number.
         *
         * The two surfaces sit at whole-dp offsets and are drawn by the same code, so "identical"
         * ought to mean identical — and the projected quad's edge still lands on different subpixels
         * in the two places, which four pixels of 28 900 report. Demanding equality would make this
         * control fail for a reason that has nothing to do with cameras; the bound is two orders of
         * magnitude below what the shared camera moves.
         */
        const val SAME_SHAPE = 50

        /**
         * 19.5 dp at the screen edge is the measured difference, so the two quads part company along
         * a whole edge. Measured at 1 825 pixels here.
         */
        const val MINIMUM_PIXELS_MOVED = 500
    }
}
