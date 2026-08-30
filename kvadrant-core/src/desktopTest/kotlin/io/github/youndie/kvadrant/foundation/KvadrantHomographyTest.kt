package io.github.youndie.kvadrant.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The homography reproduces `graphicsLayer` when the eye is where `graphicsLayer` puts it — and that
 * is the control the whole of [KvadrantHomography] rests on.
 *
 * Three separate guesses go into that object: the order the two rotations are applied in, the shape
 * of the perspective divide, and the layout Compose's `Matrix` wants its projective terms written
 * in. Each of them can be wrong in a way that still produces a plausible trapezoid, and checking the
 * arithmetic against itself would find none of them.
 *
 * So it is checked against the renderer. At `eye = Offset.Zero` the maths here and
 * `Modifier.graphicsLayer` are describing the *same* camera, so the two must draw the same shape.
 * Once that holds, the off-centre case — which no `graphicsLayer` can produce, and which is the
 * whole point of B-26 — is the same code with one argument changed.
 */
@OptIn(ExperimentalTestApi::class)
class KvadrantHomographyTest {
    /**
     * The transform Compose's own `Matrix.map` reads back, which is what makes the layout in
     * [KvadrantHomography.homographyFromRect] a checked claim rather than a hopeful one.
     */
    @Test
    fun the_matrix_maps_the_rectangle_onto_the_quad_it_was_built_from() {
        val size = Size(200f, 120f)
        val quad =
            listOf(
                Offset(10f, 4f),
                Offset(190f, 20f),
                Offset(180f, 118f),
                Offset(22f, 100f),
            )
        val matrix = KvadrantHomography.homographyFromRect(size, quad)
        val corners =
            listOf(
                Offset(0f, 0f),
                Offset(size.width, 0f),
                Offset(size.width, size.height),
                Offset(0f, size.height),
            )

        corners.forEachIndexed { index, corner ->
            val mapped = matrix.map(corner)
            val wanted = quad[index]
            assertTrue(
                abs(mapped.x - wanted.x) < TOLERANCE && abs(mapped.y - wanted.y) < TOLERANCE,
                "corner $index of the rectangle mapped to $mapped, not to $wanted — the matrix is " +
                    "not the homography it was asked for, or its terms are in the wrong places",
            )
        }
    }

    /**
     * The control against the renderer.
     *
     * A square turned about its vertical axis, drawn twice: once by `graphicsLayer` with a camera
     * distance, once by concatenating the homography for the same rotation with the eye at the
     * element's own centre. Same camera, so the same trapezoid.
     */
    @Test
    fun at_the_elements_own_centre_it_draws_what_graphics_layer_draws() {
        val byLayer = profile { modifier -> modifier.layerTilted() }
        val byHomography = profile { modifier -> modifier.homographyTilted(eye = Offset.Zero) }

        assertTrue(byLayer.any { it > 0 }, "graphicsLayer drew nothing")
        assertTrue(byHomography.any { it > 0 }, "the homography drew nothing")
        // A ramp, or neither picture is of a projection and this compares two rectangles.
        val lit = byLayer.filter { it > 0 }
        assertTrue(
            lit.max() - lit.min() > 1,
            "graphicsLayer drew a flat ${lit.min()}..${lit.max()} shape, so there is no projection " +
                "in the reference and this test cannot tell the two apart",
        )

        val worst = byLayer.indices.maxOf { abs(byLayer[it] - byHomography[it]) }
        assertTrue(
            worst <= COLUMN_TOLERANCE,
            "the homography and graphicsLayer disagree by $worst px in a column: the rotation " +
                "order, the divide or the matrix layout in KvadrantHomography does not match what " +
                "the renderer does, and the off-centre case built on it cannot be trusted",
        )
    }

    /**
     * And with the eye moved, it draws something else — the case `graphicsLayer` has no way to ask
     * for. Without this the test above would be satisfied by a homography that ignored `eye`.
     */
    @Test
    fun moving_the_eye_changes_the_shape() {
        val centred = profile { it.homographyTilted(eye = Offset.Zero) }
        val offCentre = profile { it.homographyTilted(eye = Offset(EYE_OFFSET, 0f)) }

        val worst = centred.indices.maxOf { abs(centred[it] - offCentre[it]) }
        assertTrue(
            worst > COLUMN_TOLERANCE,
            "moving the eye $EYE_OFFSET px changed the drawn shape by only $worst px, so the eye " +
                "argument is not reaching the projection and every camera is still the element's own",
        )
    }

    private fun Modifier.layerTilted(): Modifier =
        graphicsLayer {
            cameraDistance = kvadrantCameraUnits()
            rotationY = ANGLE
        }

    private fun Modifier.homographyTilted(eye: Offset): Modifier =
        drawWithContent {
            val quad =
                KvadrantHomography.quadUnderCamera(
                    size = size,
                    rotationXDegrees = 0f,
                    rotationYDegrees = ANGLE,
                    cameraDistance = KvadrantCamera.Distance.toPx(),
                    eye = eye,
                )
            drawContext.canvas.save()
            drawContext.canvas.concat(KvadrantHomography.homographyFromRect(size, quad))
            drawContent()
            drawContext.canvas.restore()
        }

    /** The drawn height of each column, which is what a rotation about the vertical axis changes. */
    private fun profile(tilt: (Modifier) -> Modifier): IntArray {
        var profile = IntArray(0)
        runComposeUiTest {
            setContent {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color.Black).testTag(TAG)) {
                    Box(tilt(Modifier.size(SQUARE.dp)).background(Color.White))
                }
            }
            waitForIdle()
            val image = onNodeWithTag(TAG).captureToImage()
            val pixels = IntArray(image.width * image.height).also(image::readPixels)
            profile =
                IntArray(image.width) { x ->
                    (0 until image.height).count { y -> pixels[y * image.width + x] and 0xFFFFFF != 0 }
                }
        }
        return profile
    }

    private companion object {
        const val TAG = "homography"
        const val WIDTH = 400
        const val HEIGHT = 260
        const val SQUARE = 120
        const val ANGLE = 25f
        const val EYE_OFFSET = 200f

        const val TOLERANCE = 0.01f

        /** One column of antialiased edge either way. */
        const val COLUMN_TOLERANCE = 2
    }
}
