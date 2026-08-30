package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.kvadrantLatin
import io.github.youndie.kvadrant.theme.KvadrantColors
import io.github.youndie.kvadrant.theme.KvadrantTheme
import io.github.youndie.kvadrant.type.portableTypography
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A press dragged to a corner leans like a press that started there.
 *
 * This is B-27's acceptance criterion, and the measurement that justified building anything: on a
 * 158 px surface the leading column of the drawn quad is 152 px for a centre press and 119 for a
 * corner one. A finger dragged across a tile used to leave that whole fifth of the effect unused,
 * because `TiltIndication` reads an `InteractionSource` and a source carries no motion.
 */
@OptIn(ExperimentalTestApi::class)
class TiltFollowsFingerTest {
    private fun frameAfter(gesture: androidx.compose.ui.test.TouchInjectionScope.() -> Unit): IntArray {
        var pixels = IntArray(0)
        runComposeUiTest {
            setContent {
                KvadrantTheme(KvadrantColors.dark(), portableTypography(kvadrantLatin())) {
                    Box(Modifier.size(300.dp).background(Color.Black).testTag("frame")) {
                        Box(
                            Modifier
                                .size(158.dp)
                                .kvadrantTilt {}
                                .background(Color.White),
                        )
                    }
                }
            }
            onNodeWithTag("frame").performTouchInput(gesture)
            waitForIdle()
            val image = onNodeWithTag("frame").captureToImage()
            pixels = IntArray(image.width * image.height).also(image::readPixels)
        }
        return pixels
    }

    private fun differing(
        a: IntArray,
        b: IntArray,
    ): Int = a.indices.count { a[it] != b[it] }

    @Test
    fun a_press_dragged_to_the_corner_leans_like_one_that_started_there() {
        val corner =
            androidx.compose.ui.geometry
                .Offset(4f, 4f)
        val centre =
            androidx.compose.ui.geometry
                .Offset(79f, 79f)

        // Held at the corner and never moved; held in the middle and never moved; and one that
        // starts in the middle and is dragged to the corner while still down.
        val started = frameAfter { down(corner) }
        val middle = frameAfter { down(centre) }
        val dragged =
            frameAfter {
                down(centre)
                moveTo(corner)
            }

        // The control. Comparing frames is only meaningful if two different presses produce
        // different frames — a surface that ignored the position entirely would satisfy everything
        // below perfectly.
        val apart = differing(started, middle)
        assertTrue(apart > 1000, "a corner press and a centre press render the same: $apart pixels differ")

        // **Relative, and it has to be.** The dragged frame does not match the corner press to the
        // pixel and cannot: by the time the second press is delivered the surface has already leaned
        // under the finger, so the local coordinate it reports is not quite the one a still finger
        // would have given. Measured, that residue is 425 pixels of 90 000 against 1 914 for the
        // centre press — the drag lands four and a half times nearer the corner than the middle,
        // which is the claim. An absolute threshold here would be a number chosen to fit today's
        // renderer.
        val toCorner = differing(started, dragged)
        val toCentre = differing(middle, dragged)
        assertTrue(
            toCorner * 3 < toCentre,
            "a press dragged to the corner sits $toCorner from one that started there and " +
                "$toCentre from one that stayed in the middle — the tilt is not following the finger",
        )
    }
}
