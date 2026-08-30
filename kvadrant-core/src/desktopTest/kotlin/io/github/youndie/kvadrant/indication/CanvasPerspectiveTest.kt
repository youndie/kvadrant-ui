package io.github.youndie.kvadrant.indication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What kind of transform `Canvas.concat` actually carries — measured, because the answer decides
 * whether
 * [B-26](https://github.com/youndie/kvadrant-ui/blob/main/docs/backlog/B-26-per-layer-camera-versus-a-global-one.md)
 * has a route at all.
 *
 * `NestedCameraTest` showed a screen-wide camera cannot come from nesting `graphicsLayer`s, and the
 * only remaining idea was to compute the projection here and hand it to the canvas. That is worth
 * nothing if the canvas flattens what it is given.
 *
 * **It carries a projective transform, and the perspective divide is driven by x and y — not by z.**
 * Sweeping every candidate slot of Compose's `Matrix` says so plainly: at `[0, 3]` a turned square
 * comes out a trapezoid ramping 136 px to 175 px down its columns, and at `[2, 3]`, `[3, 2]`,
 * `[3, 0]` and `[3, 1]` it stays a flat 120 px however the term is set. That is a **3 x 3
 * homography** wearing a 4 x 4's clothes, which also matches Compose's own `Matrix.map`, where
 * `w = m[0,3]·x + m[1,3]·y + m[3,3]` and z appears nowhere.
 *
 * **This is enough, and that is the useful half.** A flat surface rotated in space and projected
 * from any eye maps to a quadrilateral, and every plane-to-plane projective map *is* a homography —
 * so a camera anywhere, including one over the whole screen, can be expressed as one of these. What
 * an implementation has to do is compute where the element's four corners land under the shared
 * camera and solve for the homography that takes its rectangle there, rather than hand the canvas a
 * camera and expect it to divide.
 *
 * The first version of this file asserted the opposite and would have closed B-26 as impossible. It
 * set the term at `[3, 2]` — the slot a column-vector convention puts it in — measured a
 * parallelogram, and concluded the canvas dropped perspective. It drops *that* term. Sweeping the
 * others is what found the one it keeps.
 */
@OptIn(ExperimentalTestApi::class)
class CanvasPerspectiveTest {
    @Test
    fun the_canvas_carries_a_projective_transform() {
        val lit = profileOfRotatedSquare(row = 0, column = 3).filter { it > 0 }

        assertTrue(lit.isNotEmpty(), "nothing was drawn, so this measures nothing")
        assertTrue(
            lit.max() - lit.min() > 1,
            "the turned square came out ${lit.min()}..${lit.max()} px tall across its columns — a " +
                "parallelogram rather than a trapezoid. Canvas.concat has stopped carrying the " +
                "projective term, and B-26 has no route left on this renderer",
        )
    }

    /**
     * The other half, and the reason an implementation cannot simply set a camera distance.
     *
     * The slot a 3D convention puts the camera in does nothing here. Anything built on this has to
     * solve for the quadrilateral itself; handing the canvas a depth and expecting it to divide
     * produces a parallelogram, silently.
     */
    @Test
    fun the_divide_is_not_driven_by_depth() {
        val lit = profileOfRotatedSquare(row = 2, column = 3).filter { it > 0 }

        assertTrue(lit.isNotEmpty(), "nothing was drawn, so this measures nothing")
        assertEquals(
            lit.min(),
            lit.max(),
            "a term at [2, 3] now foreshortens the square, so the canvas has gained a depth-driven " +
                "divide and a shared camera could be expressed far more directly than the " +
                "homography B-26 is planned around",
        )
    }

    /** The height of each drawn column: a ramp under perspective, flat under an affine transform. */
    private fun profileOfRotatedSquare(
        row: Int,
        column: Int,
    ): IntArray {
        var profile = IntArray(0)
        runComposeUiTest {
            setContent {
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color.Black).testTag(TAG)) {
                    Box(
                        Modifier
                            .size(SQUARE.dp)
                            .drawWithContent {
                                val centre = size.width / 2f
                                // Built as an explicit product. Setting the perspective element
                                // and then calling `rotateY` does not survive: `rotateY`
                                // multiplies through the whole matrix and the term does not come
                                // out the other side, which is why the first attempt measured a
                                // parallelogram at both candidate slots.
                                val perspective =
                                    Matrix().apply {
                                        this[row, column] =
                                            -1f / (DEPTH * density)
                                    }
                                val matrix =
                                    Matrix().apply {
                                        translate(x = centre, y = size.height / 2f)
                                        timesAssign(perspective)
                                        timesAssign(Matrix().apply { rotateY(ANGLE) })
                                        timesAssign(Matrix().apply { translate(x = -centre, y = -size.height / 2f) })
                                    }
                                drawContext.canvas.save()
                                drawContext.canvas.concat(matrix)
                                drawContent()
                                drawContext.canvas.restore()
                            }.background(Color.White),
                    )
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
        const val TAG = "perspective"
        const val WIDTH = 400
        const val HEIGHT = 260
        const val SQUARE = 120
        const val ANGLE = 30f

        /** `KvadrantCamera.Distance` in dp, which is the depth the tilt actually uses. */
        const val DEPTH = 576f
    }
}
