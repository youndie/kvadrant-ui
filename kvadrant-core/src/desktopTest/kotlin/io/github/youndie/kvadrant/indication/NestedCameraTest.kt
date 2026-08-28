package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.youndie.kvadrant.foundation.kvadrantCameraUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Can two nested `graphicsLayer`s express one camera over the whole screen? **No, and this measures
 * why**, because B-26 says they can and that sentence is the plan for the next attempt.
 *
 * The proposal is an inner layer turning the element about its own centre and an outer one placing
 * the result relative to the screen's axis, so that the projection centre and the rotation pivot can
 * finally be different points. The reason to doubt it is that a `graphicsLayer` renders its content
 * and applies a matrix to the *result*: whatever depth the inner rotation had is gone by the time
 * the outer layer sees it, and a perspective divide applied to a flat z = 0 quad is the identity
 * however far off-axis its origin sits.
 *
 * So: the same rotated square, drawn at two different places on the screen, wrapped in an outer
 * layer whose projection centre is the *screen's* centre in each case. If the outer layer does
 * anything, the two come out different shapes. If it does nothing, they are identical and the
 * approach is dead before it is built.
 *
 * This is deliberately not a test of `TiltIndication`. It is a test of the platform primitive the
 * plan rests on, and the reason to write it first is that B-26's previous attempt built the thing
 * and shipped a defect to a device.
 *
 * **Measured: identical.** A 120 dp square turned 30° about its own axis draws a trapezoid ramping
 * from 128 px to 114 px down its columns, and it draws *the same* trapezoid at the middle of the
 * screen and at 15 % across it. The outer layer contributes nothing, so a shared camera is not
 * reachable this way and the next attempt should not start here.
 *
 * The control took two corrections to get right, which is the argument for having one. Measured per
 * row first, and a `rotationY` leaves a middle row's width alone, so the profile was two constants;
 * then with `cameraDistance = 900f`, which is 900 × 72 px of depth and therefore orthographic. Both
 * times the assertion said the test could not tell the cases apart instead of reporting that they
 * matched.
 */
@OptIn(ExperimentalTestApi::class)
class NestedCameraTest {
    /**
     * The drawn **height of each column**, which is the shape a rotation about the vertical axis
     * changes: the edge that turns towards the eye grows and the far edge shrinks, so the square
     * becomes a trapezoid and the profile is a ramp.
     *
     * Measured per column rather than per row, which was the first attempt and was flat — a
     * `rotationY` leaves the width of a middle row alone, so a row profile compares two constants
     * and cannot tell any two cases apart. The control below is what caught it.
     */
    private fun profileAt(originX: Float): IntArray {
        var profile = IntArray(0)
        runComposeUiTest {
            setContent {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color.Black).testTag(TAG)) {
                    Box(
                        Modifier
                            .offset(x = (originX * WIDTH).dp - (SQUARE / 2).dp, y = 40.dp)
                            .graphicsLayer {
                                // The outer layer: the screen's centre expressed in this element's
                                // own coordinates, which is what "one camera over the display"
                                // would need.
                                transformOrigin =
                                    TransformOrigin(
                                        pivotFractionX = (WIDTH / 2f - originX * WIDTH) / SQUARE + 0.5f,
                                        pivotFractionY = 0.5f,
                                    )
                                cameraDistance = kvadrantCameraUnits()
                            }.graphicsLayer {
                                // The inner layer: the element turning about its own centre.
                                transformOrigin = TransformOrigin.Center
                                cameraDistance = kvadrantCameraUnits()
                                rotationY = ROTATION
                            }.size(SQUARE.dp)
                            .background(Color.White),
                    )
                }
            }
            val image = onNodeWithTag(TAG).captureToImage()
            val pixels = IntArray(image.width * image.height).also(image::readPixels)
            profile =
                IntArray(image.width) { x ->
                    (0 until image.height).count { y -> pixels[y * image.width + x] and 0xFFFFFF != 0 }
                }
        }
        return profile
    }

    @Test
    fun an_outer_layer_cannot_move_the_projection_centre() {
        val centred = profileAt(0.5f).filter { it > 0 }
        val offCentre = profileAt(0.15f).filter { it > 0 }

        assertTrue(centred.isNotEmpty(), "the square is not drawn at the centre")
        assertTrue(offCentre.isNotEmpty(), "the square is not drawn off-centre")
        // The control. A rotation about the vertical axis with a camera set turns the square into a
        // trapezoid, so the column heights ramp. A flat profile means no projection is reaching the
        // pixels and the comparison below would be two constants agreeing about nothing.
        assertTrue(
            centred.max() - centred.min() > 1,
            "the rotated square has a flat profile (${centred.min()}..${centred.max()}), so the " +
                "camera is not projecting and this test cannot tell the two cases apart",
        )

        assertEquals(
            centred,
            offCentre,
            "the same rotated square drawn at two places on the screen came out different shapes, " +
                "so an outer graphicsLayer *can* move the projection centre and B-26's plan is " +
                "viable — rewrite this test as the measurement of how much it changes",
        )
    }

    private companion object {
        const val TAG = "nested"
        const val WIDTH = 400
        const val HEIGHT = 260
        const val SQUARE = 120
        const val ROTATION = 30f
    }
}
