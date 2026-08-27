package io.github.youndie.kvadrant.behaviour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.components.KvadrantTurnstile
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The turnstile, guarded by time rather than by a picture.
 *
 * A screenshot cannot do this and it took recording one to see why: the component's two *resting*
 * states are "shown, square on, fully opaque" and "away, fully transparent". Both goldens for it
 * came back as a plain rectangle and a black square respectively, after guarding a hand-written copy
 * of the rotation for as long as they existed. The rotation only exists in between, so the clock has
 * to be stopped in between.
 *
 * What is asserted is the thing the component is named for: the axis is the **left edge**. Under a
 * rotation about the left edge the left edge stays put and the right edge swings away from the eye,
 * so mid-flight the drawn shape is narrower and it is narrower *on the right*.
 *
 * It is measured on the way **in**, and that is not arbitrary. The exit runs on exponential-in(6),
 * which is nearly flat at the start: 90 ms into a 250 ms exit the right edge had moved two pixels.
 * That is the asymmetry the component exists for — leaving feels quicker than arriving because
 * almost all of the leaving happens at the end — and it makes the exit a poor place to look for a
 * rotation, because by the time the angle is large the alpha has nearly finished fading.
 */
@OptIn(ExperimentalTestApi::class)
class TurnstileTest {
    private class Edges(
        val left: Int,
        val right: Int,
    )

    @Test
    fun mid_flight_the_content_turns_about_its_left_edge() =
        runComposeUiTest {
            mainClock.autoAdvance = false
            var visible by mutableStateOf(true)
            setContent {
                // The tag goes *before* the padding. Tagged after it, the capture begins exactly
                // at the content's left edge, so a left edge that moves outward is clipped away by
                // the frame rather than measured — both axes then report a left edge of 0 and the
                // second assertion below cannot tell them apart. It could not, for a while.
                Box(
                    Modifier
                        .size(400.dp)
                        .background(Color.Black)
                        .testTag("frame")
                        .padding(20.dp),
                ) {
                    KvadrantTurnstile(visible = visible) {
                        Box(Modifier.fillMaxWidth().height(80.dp).background(Color.White))
                    }
                }
            }
            mainClock.advanceTimeBy(1_000)
            val atRest = edges()

            // All the way out, then part of the way back in. The window where the content is both
            // visibly turned and visible at all is narrow, and finding it is the interesting part:
            // the angle runs on exponential-out(6) and is most of the way home early, while the
            // alpha runs on `tween`'s *default* easing — fast-out-slow-in, Compose's, not Metro's —
            // and is barely started. At 70 ms the brightest pixel on screen was 14 of 255. 105 ms
            // is where both are usable.
            visible = false
            mainClock.advanceTimeBy(1_000)
            visible = true
            mainClock.advanceTimeBy(105)
            val midFlight = edges()

            assertTrue(
                midFlight.right < atRest.right - 4,
                "mid-flight the right edge is at ${midFlight.right}, at rest ${atRest.right} — the " +
                    "content is not turning away at all",
            )
            assertTrue(
                kotlin.math.abs(midFlight.left - atRest.left) <= 2,
                "mid-flight the left edge moved from ${atRest.left} to ${midFlight.left} — the axis " +
                    "is not the left edge, which is the whole difference between a turnstile and a " +
                    "card flipping about its middle",
            )
        }

    private fun androidx.compose.ui.test.ComposeUiTest.edges(): Edges {
        val image = onNodeWithTag("frame").captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)

        // The widest lit row, not the middle one: the content is 80 dp tall at the top of a 400 dp
        // frame, and sampling the centre measured the black below it.
        // The threshold follows the frame rather than being written down: in flight the content is
        // a faint grey whose brightness is whatever the alpha curve says, and a fixed cut either
        // finds nothing or finds the whole background depending on the moment chosen.
        val brightest = pixels.maxOf { it shr 16 and 0xFF }
        val cut = maxOf(8, brightest / 2)

        fun litIn(y: Int) = (0 until image.width).filter { x -> (pixels[y * image.width + x] shr 16 and 0xFF) > cut }
        val lit = (0 until image.height).map(::litIn).maxByOrNull { it.size }.orEmpty()
        assertTrue(
            lit.isNotEmpty(),
            "nothing was drawn at all — brightest pixel is $brightest of 255",
        )
        return Edges(lit.first(), lit.last())
    }
}
